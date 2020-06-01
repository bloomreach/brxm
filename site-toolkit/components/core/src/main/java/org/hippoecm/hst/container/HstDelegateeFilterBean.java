/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.container;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.jcr.Repository;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ContainerNotFoundException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstRequestProcessor;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.core.jcr.pool.MultipleRepository;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.core.sitemapitemhandler.FilterChainAwareHstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.web.context.ServletContextAware;

import static java.lang.Boolean.TRUE;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hippoecm.hst.core.container.ContainerConstants.CMSSESSIONCONTEXT_BINDING_PATH;
import static org.hippoecm.hst.core.container.ContainerConstants.DEFAULT_SITE_PIPELINE_NAME;
import static org.hippoecm.hst.core.container.ContainerConstants.FORWARD_RECURSION_ERROR;
import static org.hippoecm.hst.core.container.ContainerConstants.HST_JAAS_LOGIN_ATTEMPT_RESOURCE_TOKEN;
import static org.hippoecm.hst.core.container.ContainerConstants.HST_JAAS_LOGIN_ATTEMPT_RESOURCE_URL_ATTR;
import static org.hippoecm.hst.util.HstRequestUtils.createURLWithExplicitSchemeForRequest;
import static org.hippoecm.hst.util.HstRequestUtils.getFarthestRemoteAddr;
import static org.hippoecm.hst.util.HstRequestUtils.getFarthestRequestHost;
import static org.hippoecm.hst.util.HstRequestUtils.getFarthestRequestScheme;
import static org.hippoecm.hst.util.HstRequestUtils.getRenderingHost;

public class HstDelegateeFilterBean extends AbstractFilterBean implements ServletContextAware, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(HstDelegateeFilterBean.class);

    private final static String FILTER_DONE_KEY = "filter.done_"+HstDelegateeFilterBean.class.getName();

    public static final String AUTORELOAD_PATHINFO = "/autoreload";

    private static final String HST_MODE_QUERYSTRING_ATTR = "HstMode";
    private static final String SKIP_HST_SESSION_ATTR = HstDelegateeFilterBean.class.getName() + ".SkipHstMode";

    private ServletContext servletContext;

    private HstContainerConfig requestContainerConfig;

    private HstManager hstManager;

    private HstRequestContextComponent requestContextComponent;

    private PreviewDecorator previewDecorator;

    private Repository repository;

    private HstRequestProcessor requestProcessor;

    private HstURLFactory urlFactory;

    /**
     * Note the '{}' below looks funny but very important to avoid IllegalAccessError because the RequestContextProvider
     * lives in the shared lib and from the RequestContextProvider we do not want to expose
     * RequestContextProvider#set(HstRequestContext) or RequestContextProvider#clear(), hence we need to extent the
     * protected ModifiableRequestContextProvider
     */
    private static final RequestContextProvider.ModifiableRequestContextProvider modifiableRequestContextProvider =
            new RequestContextProvider.ModifiableRequestContextProvider() {};

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void setHstManager(HstManager hstManager) {
        this.hstManager = hstManager;
    }

    public void setHstRequestContextComponent(HstRequestContextComponent requestContextComponent) {
        this.requestContextComponent = requestContextComponent;
    }

    public void setUrlFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
    }

    public void setPreviewDecorator(PreviewDecorator previewDecorator) {
        this.previewDecorator = previewDecorator;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setHstRequestProcessor(HstRequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        requestContainerConfig = new HstContainerConfigImpl(servletContext, Thread.currentThread().getContextClassLoader());
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest)request;

        final HttpServletResponse res = (HttpServletResponse)response;

        HttpSession session = req.getSession(false);

        final String contextPath = req.getContextPath();

        final HippoWebappContext hippoWebappContext = HippoWebappContextRegistry.get().getContext(contextPath);
        if (hippoWebappContext == null) {
            throw new IllegalStateException(String.format("No registered HST webapp for contextPath '%s'. Cannot handle " +
                    "request. ", contextPath));
        }

        if (hippoWebappContext.getType() == HippoWebappContext.Type.CMS) {
            if (skipHst(req, session)) {
                log.debug("Skipping HST Processing for CMS webapp");
                chain.doFilter(request, response);
                return;
            }
        }

        if (request.getAttribute(ContainerConstants.HST_RESET_FILTER) != null) {
            request.removeAttribute(FILTER_DONE_KEY);
            request.removeAttribute(ContainerConstants.HST_RESET_FILTER);
        }

        if (request.getAttribute(FILTER_DONE_KEY) != null) {
            chain.doFilter(request, response);
            return;
        }


        final boolean isJaasLoginAttempt = session != null
                && session.getAttribute(HST_JAAS_LOGIN_ATTEMPT_RESOURCE_TOKEN) != null
                && session.getAttribute(HST_JAAS_LOGIN_ATTEMPT_RESOURCE_TOKEN).equals(req.getParameter("token"));
        // make sure to not handle FORWARD requests for jaas authentication by the HST
        if (isJaasLoginAttempt && req.getAttribute(RequestDispatcher.FORWARD_SERVLET_PATH) != null) {
            chain.doFilter(req, res);
            return;
        }

        request.setAttribute(FILTER_DONE_KEY, TRUE);

        boolean requestContextSetToProvider = false;

        Task rootTask = null;

        // Sets up the container request wrapper
        HstContainerRequest containerRequest = new HstContainerRequestImpl(req, hstManager.getPathSuffixDelimiter());

        try {

            if (isAutoReloadEndpoint(containerRequest)) {
                log.info("Auto reload websocket endpoint request for {}, skip hst request processing", containerRequest);
                chain.doFilter(request, response);
                return;
            }

            VirtualHosts vHosts = hstManager.getVirtualHosts();

            final String renderingHost = getRenderingHost(containerRequest);

            if (isCmsSessionContextBindingRequest(req)) {
                if (CmsSSOAuthenticationHandler.isAuthenticated(containerRequest)) {
                    log.info("Already authenticated");
                    ((HttpServletResponse) response).setStatus(SC_NO_CONTENT);
                    return;
                }

                if (CmsSSOAuthenticationHandler.authenticate(containerRequest, res)) {
                    res.setStatus(HttpServletResponse.SC_OK);
                }
                return;

            } else if (requestComesFromCms(vHosts, renderingHost, req)
                    && !CmsSSOAuthenticationHandler.isAuthenticated(containerRequest)
                    && !CmsSSOAuthenticationHandler.authenticate(containerRequest, res)) {
                return;
            }

            // when getPathSuffix() is not null, we have a REST url and never skip hst request processing
            if ((containerRequest.getPathSuffix() == null && vHosts.isHstFilterExcludedPath(containerRequest.getPathInfo()))) {
                log.info("'{}' part of excluded paths for hst request matching.", containerRequest);
                chain.doFilter(request, response);
                return;
            }

            // we always want to have the virtualhost available, even when we do not have hst request processing:
            // We need to know whether to include the contextpath in URL's or not, even for jsp's that are not dispatched by the HST
            // This info is on the virtual host.
            String hostName = getFarthestRequestHost(containerRequest);


            String ip = getFarthestRemoteAddr(containerRequest);
            if (vHosts.isDiagnosticsEnabled(ip)) {
                rootTask = HDC.start(HstDelegateeFilterBean.class.getSimpleName());
                rootTask.setAttribute("request", containerRequest.toString());
            }

            ResolvedVirtualHost resolvedVirtualHost = vHosts.matchVirtualHost(hostName);

            // when resolvedVirtualHost = null, we cannot do anything else then fall through to the next filter
            if (resolvedVirtualHost == null) {
                if (isLocalhostIpPlatformRequest(containerRequest, hostName)) {
                    log.debug("'{}' can not be matched to a host. Skip HST Filter and request processing since most likely it " +
                            "is a hosting environment internal request, like a pinger. ", containerRequest);
                } else {
                    log.warn("'{}' can not be matched to a host. Skip HST Filter and request processing. ", containerRequest);
                }
                chain.doFilter(request, response);
                return;
            }

            log.debug("{} matched to host '{}'", containerRequest, resolvedVirtualHost.getVirtualHost());

            /*
             * HSTTWO-1519
             * Below is a workaround for JAAS authentication: The j_security_check URL is always handled by the
             * container, after which a REDIRECT takes place which always includes the contextpath. In case of a
             * proxy like httpd in front that again adds the contextpath, the URL ends up with twice the contextpath.
             * Another problem occurs when the login is over https: The container normally runs over http, and hence
             * always a http redirect is done by the container. Hence we check below whether there is an attribute
             * on the http session that is only present after a jaas login attempt. If present, we do another redirect.
             */
            if (isJaasLoginAttempt) {
                // we are dealing with client side redirect from the container after JAAS login. This redirect typically
                // fails in case of proxy taking care of the hippoWebappContext path in front of the application.
                // hence we need another redirect.
                String resourceURL = (String)session.getAttribute(HST_JAAS_LOGIN_ATTEMPT_RESOURCE_URL_ATTR);
                session.removeAttribute(HST_JAAS_LOGIN_ATTEMPT_RESOURCE_URL_ATTR);
                session.removeAttribute(HST_JAAS_LOGIN_ATTEMPT_RESOURCE_TOKEN);
                log.debug("Redirect {} to '{}'", containerRequest, resourceURL);
                res.sendRedirect(resourceURL);
                return;
            }

            request.setAttribute(ContainerConstants.VIRTUALHOSTS_REQUEST_ATTR, resolvedVirtualHost);

            HstMutableRequestContext requestContext = (HstMutableRequestContext) containerRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);

            if (requestContext == null) {
                requestContext = requestContextComponent.create();
                containerRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
            }
            requestContext.setServletContext(servletContext);
            requestContext.setServletRequest(containerRequest);
            requestContext.setServletResponse(res);
            requestContext.setPathSuffix(containerRequest.getPathSuffix());


            if (BooleanUtils.toBoolean(request.getParameter(ContainerConstants.HST_REQUEST_USE_FULLY_QUALIFIED_URLS))) {
                requestContext.setFullyQualifiedURLs(true);
            }

            // sets up the current thread's active request hippoWebappContext object.
            initializeResourceLifecycleManagements();
            modifiableRequestContextProvider.set(requestContext);

            requestContextSetToProvider = true;

            ResolvedMount resolvedMount = requestContext.getResolvedMount();

            if (resolvedMount == null) {
                resolvedMount = resolvedVirtualHost.matchMount(containerRequest.getPathInfo());
            }

            if (resolvedMount == null) {
                throw new MatchException(String.format("No matching Mount for '%s'", containerRequest));
            }

            final boolean authenticated = CmsSSOAuthenticationHandler.isAuthenticated(containerRequest);

            if (resolvedMount.getMatchingIgnoredPrefix() != null && !authenticated) {
                // eg _cmsinternal request but not authenticated, hence should return 404 (just page not found,
                // not even an unauthorized since for example http://www.example.org/_cmsinternal just doesn't exist
                // over the host of the site
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            request.setAttribute(ContainerConstants.RESOLVED_MOUNT_REQUEST_ATTR, resolvedMount);
            requestContext.setResolvedMount(resolvedMount);
            if (renderingHost != null) {
                requestContext.setRenderHost(renderingHost);
                if (requestComesFromCms(vHosts, renderingHost, req)) {
                    if (!authenticated) {
                        ((HttpServletResponse) response).sendError(SC_UNAUTHORIZED);
                        log.warn("Attempted Channel Manager preview request without being authenticated");
                        return;
                    }
                    requestContext.setChannelManagerPreviewRequest(true);
                    if (resolvedMount instanceof MutableResolvedMount) {
                        Mount undecoratedMount = resolvedMount.getMount();
                        if (!(undecoratedMount instanceof ContextualizableMount)) {
                            String msg = String.format("The matched mount for request '%s' is not an instanceof of a ContextualizableMount. " +
                                    "Cannot act as preview mount. Cannot proceed request for CMS SSO environment.", containerRequest);
                            throw new MatchException(msg);
                        }
                        Mount decoratedMount = previewDecorator.decorateMountAsPreview(undecoratedMount);
                        if (decoratedMount == undecoratedMount) {
                            log.debug("Matched mount pointing to site '{}' is already a preview so no need for CMS SSO hippoWebappContext to decorate the mount to a preview", undecoratedMount.getMountPoint());
                        } else {
                            log.debug("Matched mount pointing to site '{}' is because of CMS SSO hippoWebappContext replaced by preview decorated mount pointing to site '{}'", undecoratedMount.getMountPoint(), decoratedMount.getMountPoint());
                        }
                        ((MutableResolvedMount) resolvedMount).setMount(decoratedMount);
                    } else {
                        throw new MatchException("ResolvedMount must be an instance of MutableResolvedMount to be usable in CMS SSO environment. Cannot proceed request for " + hostName + " and " + containerRequest.getRequestURI());
                    }
                }
            }

            log.debug("{} matched to mount '{}'", containerRequest, resolvedMount.getMount());

            // sets filterChain for ValveContext to be able to retrieve...
            req.setAttribute(ContainerConstants.HST_FILTER_CHAIN, chain);

            setHstServletPath((GenericHttpServletRequestWrapper) containerRequest, resolvedMount);

            HstContainerURL hstContainerUrl = createOrGetContainerURL(containerRequest, hstManager, requestContext, resolvedMount, res);

            final String farthestRequestScheme = getFarthestRequestScheme(req);

            if (resolvedMount.getMount().isMapped()) {
                ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
                boolean processSiteMapItemHandlers = false;

                if (resolvedSiteMapItem == null) {
                    processSiteMapItemHandlers = true;
                    resolvedSiteMapItem = resolvedMount.matchSiteMapItem(hstContainerUrl.getPathInfo());
                    if(resolvedSiteMapItem == null) {
                        // should not be possible as when it would be null, an exception should have been thrown
                        log.warn("'{}' could not be processed by the HST: Error resolving request to sitemap item", containerRequest);
                        sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }

                    log.debug("{} matched to sitemapitem  '{}'", containerRequest, resolvedSiteMapItem.getHstSiteMapItem());
                    requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
                    finishMatchingPhase(requestContext);
                }

                if (!isSupportedScheme(requestContext, resolvedSiteMapItem, farthestRequestScheme)) {
                    final HstSiteMapItem hstSiteMapItem = resolvedSiteMapItem.getHstSiteMapItem();
                    final String urlWithExplicitSchemeForRequest;
                    switch (hstSiteMapItem.getSchemeNotMatchingResponseCode()) {
                        case HttpServletResponse.SC_OK:
                            // just continue;
                            break;
                        case HttpServletResponse.SC_MOVED_PERMANENTLY:
                            res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                            // create fully qualified redirect to scheme from sitemap item
                            urlWithExplicitSchemeForRequest = createURLWithExplicitSchemeForRequest(hstSiteMapItem.getScheme(), resolvedSiteMapItem.getResolvedMount().getMount(), req);
                            log.debug("Scheme not allowed: MOVED PERMANENTLY {} to {}", containerRequest, urlWithExplicitSchemeForRequest);
                            res.setHeader("Location", urlWithExplicitSchemeForRequest);
                            return;
                        case HttpServletResponse.SC_MOVED_TEMPORARILY:
                        case HttpServletResponse.SC_SEE_OTHER:
                        case HttpServletResponse.SC_TEMPORARY_REDIRECT:
                            // create fully qualified redirect to scheme from sitemap item
                            urlWithExplicitSchemeForRequest = createURLWithExplicitSchemeForRequest(hstSiteMapItem.getScheme(), resolvedSiteMapItem.getResolvedMount().getMount(), req);
                            log.debug("Scheme not allowed: MOVED TEMPORARILY {} to {}", containerRequest, urlWithExplicitSchemeForRequest);
                            res.sendRedirect(urlWithExplicitSchemeForRequest);
                            return;
                        case HttpServletResponse.SC_NOT_FOUND:
                            log.debug("Scheme not allowed for {} : SC_NOT_FOUND", containerRequest);
                            sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                            return;
                        case HttpServletResponse.SC_FORBIDDEN:
                            log.debug("Scheme not allowed for {} : SC_FORBIDDEN", containerRequest);
                            sendError(req, res, HttpServletResponse.SC_FORBIDDEN);
                            return;
                        default:
                            log.warn("Unsupported 'schemenotmatchingresponsecode' {} encountered. Continue rendering.", hstSiteMapItem.getSchemeNotMatchingResponseCode());
                    }
                }

                log.info("Start processing sitemap item '{}' for {}", resolvedSiteMapItem.getHstSiteMapItem(), containerRequest);
                processResolvedSiteMapItem(containerRequest, res, chain, requestContext, processSiteMapItemHandlers);

            } else {
                log.debug("{} matches mount {} that is not mapped by a sitemap.", containerRequest, resolvedMount.getMount());
                if(resolvedMount.getNamedPipeline() == null) {
                    log.warn("'{}' could not be processed by the HST: No hstSite and no custom namedPipeline for Mount", containerRequest);
                    sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                else {
                    finishMatchingPhase(requestContext);
                    if (!isSupportedScheme(requestContext, resolvedMount, farthestRequestScheme)) {
                        final Mount mount = resolvedMount.getMount();
                        final String urlWithExplicitSchemeForRequest;
                        switch (mount.getSchemeNotMatchingResponseCode()) {
                            case HttpServletResponse.SC_OK:
                                // just continue;
                                break;
                            case HttpServletResponse.SC_MOVED_PERMANENTLY :
                                res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                                // create fully qualified redirect to scheme from sitemap item
                                urlWithExplicitSchemeForRequest = createURLWithExplicitSchemeForRequest(mount.getScheme(), mount, req);
                                log.debug("MOVED PERMANENTLY {} to {}", containerRequest, urlWithExplicitSchemeForRequest);
                                res.setHeader("Location", urlWithExplicitSchemeForRequest);
                                return;
                            case HttpServletResponse.SC_MOVED_TEMPORARILY:
                            case HttpServletResponse.SC_SEE_OTHER:
                            case HttpServletResponse.SC_TEMPORARY_REDIRECT:
                                // create fully qualified redirect to scheme from sitemap item
                                urlWithExplicitSchemeForRequest = createURLWithExplicitSchemeForRequest(mount.getScheme(), mount, req);
                                log.debug("MOVED TEMPORARILY {} to {}", containerRequest, urlWithExplicitSchemeForRequest);
                                res.sendRedirect(urlWithExplicitSchemeForRequest);
                                return;
                            case HttpServletResponse.SC_NOT_FOUND:
                                log.debug("Scheme not allowed for {} : SC_NOT_FOUND", containerRequest);
                                sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                                return;
                            case HttpServletResponse.SC_FORBIDDEN:
                                log.debug("Scheme not allowed for {} : SC_FORBIDDEN", containerRequest);
                                sendError(req, res, HttpServletResponse.SC_FORBIDDEN);
                                return;
                            default :
                                log.warn("Unsupported 'schemenotmatchingresponsecode' {} encountered. Continue rendering.", mount.getSchemeNotMatchingResponseCode());
                        }
                    }
                    log.info("Start processing request for pipeline '{}' for {}", resolvedMount.getNamedPipeline(), containerRequest);
                    writeDefaultResponseHeaders(requestContext, res);
                    requestProcessor.processRequest(this.requestContainerConfig, requestContext, containerRequest, res, resolvedMount.getNamedPipeline());
                }
            }
        } catch (MatchException | ContainerNotFoundException e) {
            if(log.isDebugEnabled()) {
                log.info("{} for '{}':",e.getClass().getName(), containerRequest , e);
            } else {
                log.info("{} for '{}': '{}'" , e.getClass().getName(), containerRequest,  e.toString());
            }
            sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.warn("ContainerException for '{}':", containerRequest, e);
            } else {
                log.warn("ContainerException for '{}': {}",containerRequest,  e.toString());
            }
            sendError(req, res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        finally {
            request.removeAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
            // clears up the current thread's active request hippoWebappContext object.
            if (requestContextSetToProvider) {
                disposeHstRequestContext();
                modifiableRequestContextProvider.clear();
            }
            cleanupResourceLifecycleManagements();
            if (rootTask != null) {
                HDC.cleanUp();
            }
        }
    }

    private boolean isLocalhostIpPlatformRequest(final HstContainerRequest containerRequest, final String hostName) {
        if (hostName == null) {
            return false;
        }
        if (!hostName.startsWith("127.0.0.1")) {
            return false;
        }
        final HippoWebappContext context = HippoWebappContextRegistry.get().getContext(containerRequest.getContextPath());
        if (context == null) {
            return false;
        }
        return context.getType() == HippoWebappContext.Type.CMS || context.getType() == HippoWebappContext.Type.PLATFORM;
    }

    private boolean skipHst(final HttpServletRequest req, HttpSession session) {

        if (req.getParameter(HST_MODE_QUERYSTRING_ATTR) != null) {
            final Boolean hstMode = Boolean.valueOf(req.getParameter(HST_MODE_QUERYSTRING_ATTR));
            if (hstMode) {
                if (session != null && session.getAttribute(SKIP_HST_SESSION_ATTR) != null) {
                    session.removeAttribute(SKIP_HST_SESSION_ATTR);
                }
            } else {
                // hst should be skipped
                if (session == null) {
                    session = req.getSession(true);
                }
                session.setAttribute(SKIP_HST_SESSION_ATTR, TRUE);
            }
        }

        if (session != null && TRUE.equals(session.getAttribute(SKIP_HST_SESSION_ATTR))) {
            return true;
        }
        return false;
    }

    private void finishMatchingPhase(final HstMutableRequestContext requestContext) {
        requestContext.matchingFinished();
        if (!requestContext.isChannelManagerPreviewRequest()) {
            return;
        }
        final HttpSession session = requestContext.getServletRequest().getSession(false);
        if (session == null) {
            return;
        }
        final ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
        // we must not set the CMS_REQUEST_RENDERING_MOUNT_ID on the cmsSessionContext in case the resolved sitemap item
        // is a 'container resource' : A container resource always matches the root mount, and loading an image, css, js
        // etc file should not (re)set the CMS_REQUEST_RENDERING_MOUNT_ID as it will break in case of concurrent requests
        // for a submount if container resource requests are also involved
        // also, we should not set the CMS_REQUEST_RENDERING_MOUNT_ID in case the mount turns out to be an auto-appended
        // mount, like the pagemodelapi mount : Those mounts are typically used for some processing but not for setting
        // which mount (channel) is currently open in the channel mngr. And finally, we should not (re)set the
        // CMS_REQUEST_RENDERING_MOUNT_ID in case the resolved mount is for a mount that does not have a channel

        final Mount mount = requestContext.getResolvedMount().getMount();
        if (mount.isExplicit() && mount.getHstSite() != null && mount.getHstSite().getChannel() != null &&
                (resolvedSiteMapItem == null || !resolvedSiteMapItem.getHstSiteMapItem().isContainerResource())) {

            final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
            if (cmsSessionContext == null) {
                // no cms
                return;
            }
            final Map<String, Serializable> contextPayload = cmsSessionContext.getContextPayload();
            contextPayload.put(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID, requestContext.getResolvedMount().getMount().getIdentifier());
            contextPayload.put(ContainerConstants.RENDERING_HOST, requestContext.getRenderHost());
        }
    }

    private void disposeHstRequestContext() {
        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext != null) {
            requestContextComponent.release(requestContext);
        }
    }

    private boolean isAutoReloadEndpoint(final HstContainerRequest containerRequest) {
        if (AUTORELOAD_PATHINFO.equals(containerRequest.getPathInfo()) && containerRequest.getHeader("Sec-WebSocket-Key") != null) {
            return true;
        }
        return false;
    }

    private void setHstServletPath(final GenericHttpServletRequestWrapper request, final ResolvedMount resolvedMount) {
        if (resolvedMount.getMatchingIgnoredPrefix() != null) {
            request.setServletPath("/" + resolvedMount.getMatchingIgnoredPrefix() + resolvedMount.getResolvedMountPath());
        } else {
            request.setServletPath(resolvedMount.getResolvedMountPath());
        }
    }

    private boolean isSupportedScheme(final HstMutableRequestContext requestContext,
                                      final ResolvedSiteMapItem resolvedSiteMapItem,
                                      final String farthestRequestScheme) {
        if (requestContext.isChannelManagerPreviewRequest()) {
            // cms request always supported as piggybacking on cms host
            return true;
        }

        final HstSiteMapItem hstSiteMapItem = resolvedSiteMapItem.getHstSiteMapItem();
        if (hstSiteMapItem.isSchemeAgnostic()) {
            return true;
        }
        if (hstSiteMapItem.getScheme().equals(farthestRequestScheme)) {
            return true;
        }
        if ("https".equals(farthestRequestScheme) && resolvedSiteMapItem.getResolvedMount().getMount().getVirtualHost().isCustomHttpsSupported()) {
            // although sitemap item indicates http, https is approved by default to be rendered
            return true;
        }
        return false;
    }

    private boolean isSupportedScheme(final HstMutableRequestContext requestContext,
                                      final ResolvedMount resolvedMount,
                                      final String farthestRequestScheme) {
        if (requestContext.isChannelManagerPreviewRequest()) {
            // cms request always supported as piggybacking on cms host
            return true;
        }

        final Mount mount = resolvedMount.getMount();

        if (mount.isSchemeAgnostic()) {
            return true;
        }

        if (mount.getScheme().equals(farthestRequestScheme)) {
            return true;
        }
        if ("https".equals(farthestRequestScheme) && mount.getVirtualHost().isCustomHttpsSupported()) {
            // although mount indicates http, https is approved by default to be rendered
            return true;
        }
        return false;
    }


    private boolean isCmsSessionContextBindingRequest(final HttpServletRequest req) {
        return StringUtils.substring(req.getServletPath(), 1).equals(CMSSESSIONCONTEXT_BINDING_PATH);
    }

    // returns true if the request comes from cms
    private boolean requestComesFromCms(VirtualHosts vHosts, final String renderingHost, final HttpServletRequest req) {
        if (renderingHost == null) {
            return false;
        }
        if(vHosts.getCmsPreviewPrefix() == null || "".equals(vHosts.getCmsPreviewPrefix())) {
            return true;
        }
        if (req.getServletPath().isEmpty()) {
            return false;
        }
        if(req.getServletPath().substring(1).startsWith(vHosts.getCmsPreviewPrefix())) {
            return true;
        }
        return false;
    }

    /**
     * Sets the HST equivalent of the servletPath on the container request, namely the resolved path of the
     * resolved HST mount.
     *
     * @param containerRequest request to the set HST servlet path for
     * @param hstSitesManager
     * @param requestContext HST request context
     * @param mount the resolved HST mount
     * @param response servlet response for parsing the URL
     * @return the HST container URL for the resolved HST mount
     */
    private HstContainerURL createOrGetContainerURL(HstContainerRequest containerRequest, HstManager hstSitesManager, HstMutableRequestContext requestContext, ResolvedMount mount, HttpServletResponse response) {

        HstContainerURL hstContainerURL = requestContext.getBaseURL();

        if (hstContainerURL == null) {
            hstContainerURL = urlFactory.getContainerURLProvider().parseURL(containerRequest, response, mount);
            requestContext.setBaseURL(hstContainerURL);
        }

        return hstContainerURL;
    }

    /**
     * Removes the HST request context from the request and sends an error response.
     *
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     * @param errorCode the error code to send
     */
    private static void sendError(HttpServletRequest request, HttpServletResponse response, int errorCode) throws IOException {
        response.sendError(errorCode);
    }

    /**
     * Cleaning up resources when the entire hst request processing got skipped but there was already a jcr session taken
     * from the session pool. This currently can happen when some {@link HstSiteMapItemHandler} impl calls {@link HstRequestContext#getSession}
     * and the returns <code>null</code> from its  {@link HstSiteMapItemHandler#process(org.hippoecm.hst.core.request.ResolvedSiteMapItem, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * method, short circuiting the hst request handling (thus, during request matching, not even invoking a single {@link org.hippoecm.hst.core.container.Valve})
     */
    private void cleanupResourceLifecycleManagements() {
        if (repository instanceof MultipleRepository) {
            final ResourceLifecycleManagement[] resourceLifecycleManagements = ((MultipleRepository) repository).getResourceLifecycleManagements();
            for (ResourceLifecycleManagement resourceLifecycleManagement : resourceLifecycleManagements) {
                resourceLifecycleManagement.disposeResourcesAndReset();
            }
        }
    }

    private void initializeResourceLifecycleManagements() {
        if (repository instanceof MultipleRepository) {
            final ResourceLifecycleManagement[] resourceLifecycleManagements = ((MultipleRepository) repository).getResourceLifecycleManagements();
            for (ResourceLifecycleManagement resourceLifecycleManagement : resourceLifecycleManagements) {
                resourceLifecycleManagement.setActive(true);
            }
        }
    }

    protected void processResolvedSiteMapItem(HttpServletRequest containerRequest, HttpServletResponse res, FilterChain filterChain,
                                              HstMutableRequestContext requestContext, boolean processHandlers) throws ContainerException {
        ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();

        if (processHandlers) {
            // run the sitemap handlers if present: the returned resolvedSiteMapItem can be a different one then the one that is put in
            try {
                resolvedSiteMapItem = processHandlers(resolvedSiteMapItem , containerRequest, res, filterChain);
                if(resolvedSiteMapItem == null) {
                    return;
                }
            } catch (HstSiteMapItemHandlerException e) {
                throw e;
            }
            // sync possibly changed ResolvedSiteMapItem
            if (requestContext.getResolvedSiteMapItem() != resolvedSiteMapItem) {
                log.debug("Resetting currently request context sitemap item '{}' to '{}'",
                        requestContext.getResolvedSiteMapItem().getHstSiteMapItem(), resolvedSiteMapItem.getHstSiteMapItem());
                requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
            }
        }

        if (resolvedSiteMapItem.getErrorCode() > 0) {
            try {
                log.info("The resolved sitemap item for {} has error status: {}", containerRequest , Integer.valueOf(resolvedSiteMapItem.getErrorCode()));
                res.sendError(resolvedSiteMapItem.getErrorCode());
                
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception invocation on sendError().", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Exception invocation on sendError() : {}", e.toString());
                }
            }
            // we're done:
            return;
        }

        if (resolvedSiteMapItem.getStatusCode() > 0) {
            log.debug("Setting the status code to '{}' for '{}' because the matched sitemap item has specified the status code"
                    , String.valueOf(resolvedSiteMapItem.getStatusCode()), containerRequest.getRequestURL().toString());
            res.setStatus(resolvedSiteMapItem.getStatusCode());
        }

        log.info("Start processing request for pipeline '{}' for {}", resolvedSiteMapItem.getNamedPipeline(), containerRequest);
        writeDefaultResponseHeaders(requestContext, res);


        final String namedPipeline = resolvedSiteMapItem.getNamedPipeline();

        if ((namedPipeline == null || DEFAULT_SITE_PIPELINE_NAME.equals(namedPipeline))
                && HttpMethod.OPTIONS.matches(containerRequest.getMethod())) {
            try {
                res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method OPTIONS has not " +
                        "been implemented for the default site pipeline");
                return;
            } catch (IOException e) {
                throw new ContainerException(e);
            }
        }

        requestProcessor.processRequest(this.requestContainerConfig, requestContext, containerRequest, res, namedPipeline);

        // now, as long as there is a forward, we keep invoking processResolvedSiteMapItem:
        if (containerRequest.getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO) != null) {
            if (Boolean.TRUE.equals(requestContext.getAttribute(FORWARD_RECURSION_ERROR))) {
                throw new ContainerException("Forwarding recursion exception. Short-circuit");
            }

            String forwardPathInfo = (String) containerRequest.getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO);
            containerRequest.removeAttribute(ContainerConstants.HST_FORWARD_PATH_INFO);

            final ResolvedSiteMapItem forwardedResolvedSiteMapItem = resolvedSiteMapItem.getResolvedMount().matchSiteMapItem(forwardPathInfo);

            if (forwardedResolvedSiteMapItem.getHstSiteMapItem() == resolvedSiteMapItem.getHstSiteMapItem()) {
                log.warn("Forwarding recursion. Process forward last time");
                requestContext.setAttribute(FORWARD_RECURSION_ERROR, Boolean.TRUE);
            }

            if(forwardedResolvedSiteMapItem == null) {
                // should not be possible as when it would be null, an exception should have been thrown
                String msg = String.format("Could not match request '%s' to a sitemap item for forwardPathInfo '%s'.",
                        containerRequest, forwardPathInfo);
                throw new MatchException(msg);
            }
            requestContext.clearObjectAndQueryManagers();
            requestContext.setResolvedSiteMapItem(forwardedResolvedSiteMapItem);
            requestContext.setBaseURL(urlFactory.getContainerURLProvider().createURL(requestContext.getBaseURL(), forwardPathInfo));

            processResolvedSiteMapItem(containerRequest, res, filterChain, requestContext, true);
        }

        return;
    }

    /**
     * This method is invoked for every {@link HstSiteMapItemHandler} from the resolvedSiteMapItem that was matched from {@link ResolvedMount#matchSiteMapItem(String)}.
     * If in the for loop the <code>orginalResolvedSiteMapItem</code> switches to a different newResolvedSiteMapItem, then still
     * the handlers for  <code>orginalResolvedSiteMapItem</code> are processed and not the one from <code>newResolvedSiteMapItem</code>. If some intermediate
     * {@link HstSiteMapItemHandler#process(ResolvedSiteMapItem, HttpServletRequest, HttpServletResponse)} returns <code>null</code>, the loop and processing is stooped,
     * and <code>null</code> is returned. Entire request processing at that point is assumed to be completed already by one of the {@link HstSiteMapItemHandler}s (for
     * example if one of the handlers is a caching handler). When <code>null</code> is returned, request processing is stopped.
     * @param orginalResolvedSiteMapItem
     * @param req
     * @param res
     * @return a new or original {@link ResolvedSiteMapItem}, or <code>null</code> when request processing can be stopped
     */
    protected ResolvedSiteMapItem processHandlers(final ResolvedSiteMapItem orginalResolvedSiteMapItem,
                                                  final HttpServletRequest req,
                                                  final HttpServletResponse res,
                                                  final FilterChain filterChain) {

        ResolvedSiteMapItem newResolvedSiteMapItem = orginalResolvedSiteMapItem;

        for (HstSiteMapItemHandler handler : orginalResolvedSiteMapItem.getHstSiteMapItem().getHstSiteMapItemHandlers()) {
            log.debug("Processing siteMapItemHandler for configuration handler '{}'", handler.getClass().getName());
            try {
                if (handler instanceof FilterChainAwareHstSiteMapItemHandler) {
                    newResolvedSiteMapItem = ((FilterChainAwareHstSiteMapItemHandler) handler).process(newResolvedSiteMapItem, req, res, filterChain);
                } else {
                    newResolvedSiteMapItem = handler.process(newResolvedSiteMapItem, req, res);
                }
                if(newResolvedSiteMapItem == null) {
                    log.debug("handler for '{}' return null. Request processing done. Return null", handler.getClass().getName());
                    return null;
                }
            } catch (HstSiteMapItemHandlerException e){
                log.error(String.format("Exception during executing siteMapItemHandler '%s'", handler.getClass().getName()));
                throw e;
            }
        }

        return newResolvedSiteMapItem;
    }

    /**
     * Write default response headers which are set in sitemap item, mount or virtual host configuration.
     * @param requestContext request context
     * @param response http servlet response
     */
    private void writeDefaultResponseHeaders(final HstRequestContext requestContext, final HttpServletResponse response) {
        final Map<String, String> headerMap = getDefaultResponseHeaders(requestContext);

        if (headerMap != null) {
            headerMap.forEach((name, value) -> {
                response.setHeader(name, value);
            });
        }
    }

    /**
     * Read HTTP Response headers configuration from sitemapitem or mount and return those as a name-value paired map.
     * @param requestContext request context
     * @return a name-value paired map from HTTP Response headers configuration from sitemapitem or mount.
     */
    private Map<String, String> getDefaultResponseHeaders(final HstRequestContext requestContext) {
        ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();

        if (resolvedSiteMapItem != null) {
            return resolvedSiteMapItem.getHstSiteMapItem().getResponseHeaders();
        } else {
            ResolvedMount resolvedMount = requestContext.getResolvedMount();

            if (resolvedMount != null) {
                return resolvedMount.getMount().getResponseHeaders();
            }
        }

        return null;
    }
}
