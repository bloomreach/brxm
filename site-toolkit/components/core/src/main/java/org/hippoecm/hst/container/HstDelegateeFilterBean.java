/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

import javax.jcr.Repository;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.BooleanUtils;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
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
import org.hippoecm.hst.core.internal.MountDecorator;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.jcr.pool.MultipleRepository;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.core.sitemapitemhandler.FilterChainAwareHstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

public class HstDelegateeFilterBean extends AbstractFilterBean implements ServletContextAware, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(HstDelegateeFilterBean.class);

    private final static String FILTER_DONE_KEY = "filter.done_"+HstDelegateeFilterBean.class.getName();

    public static final String AUTORELOAD_PATHINFO = "/autoreload";

    private ServletContext servletContext;

    private HstContainerConfig requestContainerConfig;

    private HstManager hstManager;

    private HstRequestContextComponent requestContextComponent;

    private MountDecorator mountDecorator;

    private Repository repository;

    private HstRequestProcessor requestProcessor;

    private HstSiteMapItemHandlerFactory siteMapItemHandlerFactory;

    private HstURLFactory urlFactory;

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

    public void setSiteMapItemHandlerFactory(HstSiteMapItemHandlerFactory siteMapItemHandlerFactory) {
        this.siteMapItemHandlerFactory = siteMapItemHandlerFactory;
    }

    public void setMountDecorator(MountDecorator mountDecorator) {
        this.mountDecorator = mountDecorator;
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

        if (request.getAttribute(ContainerConstants.HST_RESET_FILTER) != null) {
            request.removeAttribute(FILTER_DONE_KEY);
            request.removeAttribute(ContainerConstants.HST_RESET_FILTER);
        }

        if (request.getAttribute(FILTER_DONE_KEY) != null) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;

        request.setAttribute(FILTER_DONE_KEY, Boolean.TRUE);

        boolean requestContextSetToProvider = false;

        Task rootTask = null;

        try {
            // Sets up the container request wrapper
            HstContainerRequest containerRequest = new HstContainerRequestImpl(req, hstManager.getPathSuffixDelimiter());

            if (isAutoReloadEndpoint(containerRequest)) {
                log.info("Auto reload websocket endpoint request, skip hst request processing");
                chain.doFilter(request, response);
                return;
            }

            // when getPathSuffix() is not null, we have a REST url and never skip hst request processing
            if ((containerRequest.getPathSuffix() == null && hstManager.isHstFilterExcludedPath(containerRequest.getPathInfo()))) {
                chain.doFilter(request, response);
                return;
            }

            // we always want to have the virtualhost available, even when we do not have hst request processing:
            // We need to know whether to include the contextpath in URL's or not, even for jsp's that are not dispatched by the HST
            // This info is on the virtual host.
            String hostName = HstRequestUtils.getFarthestRequestHost(containerRequest);

            VirtualHosts vHosts = hstManager.getVirtualHosts(isStaleConfigurationAllowedForRequest(containerRequest, hostName));

            String ip = HstRequestUtils.getFarthestRemoteAddr(containerRequest);
            if (vHosts.isDiagnosticsEnabled(ip)) {
                rootTask = HDC.start(HstDelegateeFilterBean.class.getSimpleName());
                rootTask.setAttribute("hostName", hostName);
                rootTask.setAttribute("uri", req.getRequestURI());
                rootTask.setAttribute("query", req.getQueryString());
            }

            ResolvedVirtualHost resolvedVirtualHost = vHosts.matchVirtualHost(hostName);

            // when resolvedVirtualHost = null, we cannot do anything else then fall through to the next filter
            if (resolvedVirtualHost == null) {
                log.warn("hostName '{}' can not be matched. Skip HST Filter and request processing. ", hostName);
                chain.doFilter(request, response);
                return;
            }

            /*
             * HSTTWO-1519
             * Below is a workaround for JAAS authentication: The j_security_check URL is always handled by the
             * container, after which a REDIRECT takes place which always includes the contextpath. In case of a
             * proxy like httpd in front that again adds the contextpath, the URL ends up with twice the contextpath.
             * Another problem occurs when the login is over https: The container normally runs over http, and hence
             * always a http redirect is done by the container. Hence we check below whether there is an attribute
             * on the http session that is only present after a jaas login attempt. If present, we do another redirect.
             */
            HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute(ContainerConstants.HST_JAAS_LOGIN_ATTEMPT_RESOURCE_TOKEN) != null ) {
                if (session.getAttribute(ContainerConstants.HST_JAAS_LOGIN_ATTEMPT_RESOURCE_TOKEN).equals(req.getParameter("token"))) {
                    // we are dealing with client side redirect from the container after JAAS login. This redirect typically
                    // fails in case of proxy taking care of the context path in front of the application.
                    // hence we need another redirect.
                    String resourceURL = (String)session.getAttribute(ContainerConstants.HST_JAAS_LOGIN_ATTEMPT_RESOURCE_URL_ATTR);
                    session.removeAttribute(ContainerConstants.HST_JAAS_LOGIN_ATTEMPT_RESOURCE_URL_ATTR);
                    session.removeAttribute(ContainerConstants.HST_JAAS_LOGIN_ATTEMPT_RESOURCE_TOKEN);
                    res.sendRedirect(resourceURL);
                    return;
                }
            }

            request.setAttribute(ContainerConstants.VIRTUALHOSTS_REQUEST_ATTR, resolvedVirtualHost);

            HstMutableRequestContext requestContext = (HstMutableRequestContext) containerRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);

            if (requestContext == null) {
                requestContext = requestContextComponent.create();
                containerRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
            }
            requestContext.setServletContext(servletContext);
            requestContext.setPathSuffix(containerRequest.getPathSuffix());

            if (BooleanUtils.toBoolean(request.getParameter(ContainerConstants.HST_REQUEST_USE_FULLY_QUALIFIED_URLS))) {
                requestContext.setFullyQualifiedURLs(true);
            }

            // sets up the current thread's active request context object.
            RequestContextProvider.set(requestContext);
            requestContextSetToProvider = true;

            ResolvedMount resolvedMount = requestContext.getResolvedMount();

            if (resolvedMount == null) {

                resolvedMount = vHosts.matchMount(hostName, containerRequest.getContextPath(), containerRequest.getPathInfo());
                if (resolvedMount != null) {
                    requestContext.setResolvedMount(resolvedMount);
                    // if we are in RENDERING_HOST mode, we always need to include the contextPath, even if showcontextpath = false.
                    String renderingHost = HstRequestUtils.getRenderingHost(containerRequest);
                    if (renderingHost != null) {
                        requestContext.setRenderHost(renderingHost);
                        if (requestComesFromCms(vHosts, resolvedMount) && session != null && Boolean.TRUE.equals(session.getAttribute(ContainerConstants.CMS_SSO_AUTHENTICATED))) {
                            requestContext.setCmsRequest(true);
                            session.setAttribute(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID, resolvedMount.getMount().getIdentifier());
                            session.setAttribute(ContainerConstants.RENDERING_HOST, renderingHost);
                            if (resolvedMount instanceof MutableResolvedMount) {
                                Mount undecoratedMount = resolvedMount.getMount();
                                if (!(undecoratedMount instanceof ContextualizableMount)) {
                                    throw new MatchException("The matched mount for request '" + hostName + " and " + containerRequest.getRequestURI() + "' is not an instanceof of a ContextualizableMount. Cannot act as preview mount. Cannot proceed request for CMS SSO environment.");
                                }
                                Mount decoratedMount = mountDecorator.decorateMountAsPreview(undecoratedMount);
                                if (decoratedMount == undecoratedMount) {
                                    log.debug("Matched mount pointing to site '{}' is already a preview so no need for CMS SSO context to decorate the mount to a preview", undecoratedMount.getMountPoint());
                                } else {
                                    log.debug("Matched mount pointing to site '{}' is because of CMS SSO context replaced by preview decorated mount pointing to site '{}'", undecoratedMount.getMountPoint(), decoratedMount.getMountPoint());
                                }
                                ((MutableResolvedMount) resolvedMount).setMount(decoratedMount);
                            } else {
                                throw new MatchException("ResolvedMount must be an instance of MutableResolvedMount to be usable in CMS SSO environment. Cannot proceed request for " + hostName + " and " + containerRequest.getRequestURI());
                            }
                        }
                    }
                } else {
                    throw new MatchException("No matching Mount for '" + hostName + "' and '" + containerRequest.getRequestURI() + "'");
                }
            }

            // sets filterChain for ValveContext to be able to retrieve...
            req.setAttribute(ContainerConstants.HST_FILTER_CHAIN, chain);

            setHstServletPath((GenericHttpServletRequestWrapper) containerRequest, resolvedMount);

            HstContainerURL hstContainerUrl = createOrGetContainerURL(containerRequest, hstManager, requestContext, resolvedMount, res);

            final String farthestRequestScheme = HstRequestUtils.getFarthestRequestScheme(req);
            if (resolvedMount.getMount().isMapped()) {
                ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
                boolean processSiteMapItemHandlers = false;

                if (resolvedSiteMapItem == null) {
                    processSiteMapItemHandlers = true;
                    resolvedSiteMapItem = resolvedMount.matchSiteMapItem(hstContainerUrl.getPathInfo());
                    if(resolvedSiteMapItem == null) {
                        // should not be possible as when it would be null, an exception should have been thrown
                        log.warn(hostName+"' and '"+containerRequest.getRequestURI()+"' could not be processed by the HST: Error resolving request to sitemap item");
                        sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
                }

                if (!isSupportedScheme(requestContext, resolvedSiteMapItem, farthestRequestScheme)) {
                   final HstSiteMapItem hstSiteMapItem = resolvedSiteMapItem.getHstSiteMapItem();
                   switch (hstSiteMapItem.getSchemeNotMatchingResponseCode()) {
                       case HttpServletResponse.SC_OK:
                            // just continue;
                            break;
                       case HttpServletResponse.SC_MOVED_PERMANENTLY :
                           res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                           // create fully qualified redirect to scheme from sitemap item
                           res.setHeader("Location", HstRequestUtils.createURLWithExplicitSchemeForRequest(hstSiteMapItem.getScheme(), resolvedSiteMapItem.getResolvedMount().getMount(), req));
                           return;
                       case HttpServletResponse.SC_MOVED_TEMPORARILY:
                       case HttpServletResponse.SC_SEE_OTHER:
                       case HttpServletResponse.SC_TEMPORARY_REDIRECT:
                           // create fully qualified redirect to scheme from sitemap item
                           res.sendRedirect(HstRequestUtils.createURLWithExplicitSchemeForRequest(hstSiteMapItem.getScheme(), resolvedSiteMapItem.getResolvedMount().getMount(), req));
                           return;
                       case HttpServletResponse.SC_NOT_FOUND:
                           sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                           return;
                       case HttpServletResponse.SC_FORBIDDEN:
                           sendError(req, res, HttpServletResponse.SC_FORBIDDEN);
                           return;
                       default :
                           log.warn("Unsupported 'schemenotmatchingresponsecode' {} encountered. Continue rendering.", hstSiteMapItem.getSchemeNotMatchingResponseCode());
                   }
                }

                processResolvedSiteMapItem(containerRequest, res, chain, hstManager, siteMapItemHandlerFactory, requestContext, processSiteMapItemHandlers);

            } else {
                if(resolvedMount.getNamedPipeline() == null) {
                    log.warn(hostName + "' and '" + containerRequest.getRequestURI() + "' could not be processed by the HST: No hstSite and no custom namedPipeline for Mount");
                    sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                }
                else {
                    if (!isSupportedScheme(requestContext, resolvedMount, farthestRequestScheme)) {
                        final Mount mount = resolvedMount.getMount();
                        switch (mount.getSchemeNotMatchingResponseCode()) {
                            case HttpServletResponse.SC_OK:
                                // just continue;
                                break;
                            case HttpServletResponse.SC_MOVED_PERMANENTLY :
                                res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                                // create fully qualified redirect to scheme from sitemap item
                                res.setHeader("Location", HstRequestUtils.createURLWithExplicitSchemeForRequest(mount.getScheme(), mount, req));
                                return;
                            case HttpServletResponse.SC_MOVED_TEMPORARILY:
                            case HttpServletResponse.SC_SEE_OTHER:
                            case HttpServletResponse.SC_TEMPORARY_REDIRECT:
                                // create fully qualified redirect to scheme from sitemap item
                                res.sendRedirect(HstRequestUtils.createURLWithExplicitSchemeForRequest(mount.getScheme(), mount, req));
                                return;
                            case HttpServletResponse.SC_NOT_FOUND:
                                sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                                return;
                            case HttpServletResponse.SC_FORBIDDEN:
                                sendError(req, res, HttpServletResponse.SC_FORBIDDEN);
                                return;
                            default :
                                log.warn("Unsupported 'schemenotmatchingresponsecode' {} encountered. Continue rendering.", mount.getSchemeNotMatchingResponseCode());
                        }
                    }
                    log.info("Processing request for pipeline '{}'", resolvedMount.getNamedPipeline());
                    requestProcessor.processRequest(this.requestContainerConfig, requestContext, containerRequest, res, resolvedMount.getNamedPipeline());
                }
            }
        }
        catch (MatchException e) {
            if(log.isDebugEnabled()) {
                log.info(e.getClass().getName() + " for '{}':", req.getRequestURI() , e);
            } else {
                log.info(e.getClass().getName() + " for '{}': '{}'" , req.getRequestURI(),  e.toString());
            }
            sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
        } catch (ContainerNotFoundException e) {
           if(log.isDebugEnabled()) {
               log.info(e.getClass().getName() + " for '{}':", req.getRequestURI() , e);
            } else {
               log.info(e.getClass().getName() + " for '{}': '{}'" , req.getRequestURI(),  e.toString());
            }
           sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
        } catch (ContainerException e) {
            if(log.isDebugEnabled()) {
                log.warn("ContainerException for '{}':",req.getRequestURI(), e);
            } else {
                log.warn("ContainerException for '{}': {}",req.getRequestURI(),  e.toString());
            }
            sendError(req, res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        finally {
            // clears up the current thread's active request context object.
            if (requestContextSetToProvider) {
                RequestContextProvider.clear();
            }

            if (rootTask != null) {
                HDC.cleanUp();
            }
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
        if (requestContext.isCmsRequest()) {
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
        if (requestContext.isCmsRequest()) {
            // cms request always supported as piggybacking on cms host
            return true;
        }

        final Mount mount = resolvedMount.getMount();

        if (mount.getScheme().equals(farthestRequestScheme)) {
            return true;
        }
        if ("https".equals(farthestRequestScheme) && mount.getVirtualHost().isCustomHttpsSupported()) {
            // although mount indicates http, https is approved by default to be rendered
            return true;
        }
        return false;
    }

    /*
     * we do never allow a stale model for cms sso logged in users as they need to see changes directly in the
     * channel manager
     */
    private boolean isStaleConfigurationAllowedForRequest(final HttpServletRequest request, String hostName) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            int index = hostName.indexOf(":");
            if (index > -1) {
                hostName = hostName.substring(0, index);
            }
            if ("127.0.0.1".equals(hostName)) {
                // internal cms rest proxy call, see SpringComponentManager-cmsrest.xml
                return false;
            }
            return true;
        }
        if (Boolean.TRUE.equals(session.getAttribute(ContainerConstants.CMS_SSO_AUTHENTICATED))) {
            return false;
        }
        return true;
    }

    // returns true if the request comes from cms
    private boolean requestComesFromCms(VirtualHosts vHosts, ResolvedMount resolvedMount) {
        if(vHosts.getCmsPreviewPrefix() == null || "".equals(vHosts.getCmsPreviewPrefix())) {
            return true;
        }
        if(vHosts.getCmsPreviewPrefix().equals(resolvedMount.getMatchingIgnoredPrefix())) {
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
        request.removeAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
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
                resourceLifecycleManagement.disposeAllResources();
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

    protected void processResolvedSiteMapItem(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain, HstManager hstSitesManager,
            HstSiteMapItemHandlerFactory siteMapItemHandlerFactory, HstMutableRequestContext requestContext, boolean processHandlers) throws ContainerException {
        ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();

        if (processHandlers) {
            initializeResourceLifecycleManagements();
            // run the sitemap handlers if present: the returned resolvedSiteMapItem can be a different one then the one that is put in
            try {
                resolvedSiteMapItem = processHandlers(resolvedSiteMapItem, siteMapItemHandlerFactory , req, res, filterChain);
                if(resolvedSiteMapItem == null) {
                    // one of the handlers has finished the request/response already
                    // call clean up of the resourceLifecycleManagements as there might have been taken a jcr session from some session
                    // pool already
                    cleanupResourceLifecycleManagements();
                    return;
                }
            } catch (HstSiteMapItemHandlerException e) {
                cleanupResourceLifecycleManagements();
                throw e;
            }
            // sync possibly changed ResolvedSiteMapItem
            requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        }

        if (resolvedSiteMapItem.getErrorCode() > 0) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("The resolved sitemap item for {} has error status: {}", requestContext.getBaseURL().getRequestPath(), Integer.valueOf(resolvedSiteMapItem.getErrorCode()));
                }
                res.sendError(resolvedSiteMapItem.getErrorCode());
                
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception invocation on sendError().", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Exception invocation on sendError().");
                }
            }
            // we're done:
            return;
        }

        if (resolvedSiteMapItem.getStatusCode() > 0) {
            log.debug("Setting the status code to '{}' for '{}' because the matched sitemap item has specified the status code"
                    ,String.valueOf(resolvedSiteMapItem.getStatusCode()), req.getRequestURL().toString() );
            res.setStatus(resolvedSiteMapItem.getStatusCode());
        }

        requestProcessor.processRequest(this.requestContainerConfig, requestContext, req, res, resolvedSiteMapItem.getNamedPipeline());

        // now, as long as there is a forward, we keep invoking processResolvedSiteMapItem:
        if(req.getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO) != null) {
            String forwardPathInfo = (String) req.getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO);
            req.removeAttribute(ContainerConstants.HST_FORWARD_PATH_INFO);

            resolvedSiteMapItem = resolvedSiteMapItem.getResolvedMount().matchSiteMapItem(forwardPathInfo);
            if(resolvedSiteMapItem == null) {
                // should not be possible as when it would be null, an exception should have been thrown
                throw new MatchException("Error resolving request to sitemap item: '"+HstRequestUtils.getFarthestRequestHost(req)+"' and '"+req.getRequestURI()+"'");
            }
            requestContext.clearObjectAndQueryManagers();
            requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
            requestContext.setBaseURL(urlFactory.getContainerURLProvider().createURL(requestContext.getBaseURL(), forwardPathInfo));

            processResolvedSiteMapItem(req, res, filterChain, hstSitesManager, siteMapItemHandlerFactory, requestContext, true);
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
     * @param siteMapItemHandlerFactory
     * @param req
     * @param res
     * @return a new or original {@link ResolvedSiteMapItem}, or <code>null</code> when request processing can be stopped
     */
    protected ResolvedSiteMapItem processHandlers(ResolvedSiteMapItem orginalResolvedSiteMapItem,
                                                  HstSiteMapItemHandlerFactory siteMapItemHandlerFactory,
                                                  HttpServletRequest req,
                                                  HttpServletResponse res,
                                                  FilterChain filterChain) {

        ResolvedSiteMapItem newResolvedSiteMapItem = orginalResolvedSiteMapItem;
        List<HstSiteMapItemHandlerConfiguration> handlerConfigsFromMatchedSiteMapItem = orginalResolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerConfigurations();
        for(HstSiteMapItemHandlerConfiguration handlerConfig : handlerConfigsFromMatchedSiteMapItem) {
           HstSiteMapItemHandler handler = siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfig);
            log.debug("Processing siteMapItemHandler for configuration handler '{}'", handlerConfig.getName() );
           try {
               if (handler instanceof FilterChainAwareHstSiteMapItemHandler) {
                   newResolvedSiteMapItem = ((FilterChainAwareHstSiteMapItemHandler) handler).process(newResolvedSiteMapItem, req, res, filterChain);
               } else {
                   newResolvedSiteMapItem = handler.process(newResolvedSiteMapItem, req, res);
               }
               if(newResolvedSiteMapItem == null) {
                   log.debug("handler for '{}' return null. Request processing done. Return null", handlerConfig.getName());
                   return null;
               }
           } catch (HstSiteMapItemHandlerException e){
               log.error("Exception during executing siteMapItemHandler '"+handlerConfig.getName()+"'");
               throw e;
           }
        }
        return newResolvedSiteMapItem;
    }

}
