/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.SessionSecurityDelegation;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsContextService;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.utilities.servlet.HttpSessionBoundJcrSessionHolder;

import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_REPO_CREDS_ATTR;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_USER_ID_ATTR;

/**
 * <p>
 * CmsSecurityValve responsible for authenticating the user using CMS.
 * </p>
 * <p> This valve check if the CMS has provided encrypted credentials or not if and only if the
 * page request is done from the CMS context. This valve checks if the CMS has provided encrypted credentials or not.
 * If
 * the credentials are _not_ available
 * with the URL, this valve will redirect to the CMS auth URL with a secret. If the credentials are  available with the
 * URL, this valve will try to get the session for the credentials and continue. </p>
 *
 * <p>
 * The check whether the page request originates from a CMS context is done by checking whether the {@link
 * HstRequestContext#getRenderHost()}
 * is not <code>null</code> : A non-null render host implies that the CMS requested the page.
 * </p>
 */
public class CmsSecurityValve extends AbstractBaseOrderableValve {

    public static final String HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CHANNEL_MNGR_SESSION = CmsSecurityValve.class.getName() + ".CmsChannelManagerRestSession";
    public static final String HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CMS_PREVIEW_SESSION = CmsSecurityValve.class.getName() + ".CmsPreviewSession";

    private SessionSecurityDelegation sessionSecurityDelegation;

    public void setSessionSecurityDelegation(SessionSecurityDelegation sessionSecurityDelegation) {
        this.sessionSecurityDelegation = sessionSecurityDelegation;
    }

    public boolean isCmsSecuredRequest(final ValveContext context) {
        final HstRequestContext requestContext = context.getRequestContext();
        if (requestContext.isCmsRequest()) {
            return true;
        }
        final ResolvedMount resolvedMount = requestContext.getResolvedMount();
        String ignoredPrefix = resolvedMount.getMatchingIgnoredPrefix();

        if (StringUtils.isEmpty(ignoredPrefix)) {
            // TODO HSTTWO-4374can we still allow ignoredPrefix to be empty on production?
            return false;
        }

        // TODO HSTTWO-4374 is #getCmsPreviewPrefix allowed to be empty still? should be possible if the matched mount is a cms
        // TODO HSTTWO-4374 host but how will this work again?
        if (ignoredPrefix.equals(resolvedMount
                .getMount().getVirtualHost().getVirtualHosts().getCmsPreviewPrefix())) {
            return true;
        }

        return false;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();

        if (!isCmsSecuredRequest(context)) {
            context.invokeNext();
            return;
        }

        log.debug("Request '{}' is invoked from CMS context. Check whether the SSO handshake is done.", servletRequest.getRequestURL());

        HttpSession httpSession = servletRequest.getSession(false);
        CmsSessionContext cmsSessionContext = httpSession != null ? CmsSessionContext.getContext(httpSession) : null;

        if (httpSession == null || cmsSessionContext == null) {
            CmsContextService cmsContextService = HippoServiceRegistry.getService(CmsContextService.class);
            if (cmsContextService == null) {
                log.debug("No CmsContextService available");
                sendError(servletResponse, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            final String cmsContextServiceId = servletRequest.getParameter("cmsCSID");
            final String cmsSessionContextId = servletRequest.getParameter("cmsSCID");
            if (cmsContextServiceId == null || cmsSessionContextId == null) {
                // no CmsSessionContext and/or CmsContextService IDs provided:  if possible, request these by redirecting back to CMS
                final String method = servletRequest.getMethod();
                if (!"GET".equals(method) && !"HEAD".equals(method)) {
                    log.warn("Invalid request to redirect for authentication because request method is '{}' and only" +
                            " 'GET' or 'HEAD' are allowed", method);
                    sendError(servletResponse, HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                log.debug("No CmsSessionContext and/or CmsContextService IDs found. Redirect to the CMS");
                redirectToCms(servletRequest, servletResponse, requestContext, cmsContextService.getId());
                return;
            }

            if (!cmsContextServiceId.equals(cmsContextService.getId())) {
                log.warn("Cannot authorize request: not coming from this CMS HOST. Redirecting to cms authentication URL to retry.");
                redirectToCms(servletRequest, servletResponse, requestContext, cmsContextService.getId());
                return;
            }


            // if the (HST) http session already exists, it might be because the user logged out from cms and logged in with
            // different user or logged in with same user again but the authorization rules for example changed (that is
            // why the user for example logged out and in). However, now it can happen that we have stale jcr sessions
            // still on the HttpSessionBoundJcrSessionHolder attribute of the HST http session. We need to clear these
            // now actively
            if (httpSession != null) {
                HttpSessionBoundJcrSessionHolder.clearAllBoundJcrSessions(HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CHANNEL_MNGR_SESSION, httpSession);
                HttpSessionBoundJcrSessionHolder.clearAllBoundJcrSessions(HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CMS_PREVIEW_SESSION, httpSession);
            } else {
                httpSession = servletRequest.getSession(true);
            }

            cmsSessionContext = cmsContextService.attachSessionContext(cmsSessionContextId, httpSession);
            if (cmsSessionContext == null) {
                httpSession.invalidate();
                log.warn("Cannot authorize request: CmsSessionContext not found");
                sendError(servletResponse, HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        servletRequest.setAttribute(CMS_REQUEST_USER_ID_ATTR, cmsSessionContext.getRepositoryCredentials().getUserID());
        // TODO HSTTWO-4375  remove this attribute once we have addressed HSTTWO-4375
        servletRequest.setAttribute(CMS_REQUEST_REPO_CREDS_ATTR, cmsSessionContext.getRepositoryCredentials());

        // We synchronize on http session to disallow concurrent requests for the Channel manager.
        synchronized (httpSession) {
            Session jcrSession = null;
            try {
                if (isPageComposerRequest(servletRequest)) {
                    jcrSession = getOrCreateCmsChannelManagerRestSession(servletRequest);
                } else {
                    // request preview website, for example in channel manager. The request is not
                    // a REST call
                    if (sessionSecurityDelegation.sessionSecurityDelegationEnabled()) {
                        jcrSession = getOrCreateCmsPreviewSession(servletRequest);
                    } else {
                        // do not yet create a session. just use the one that the HST container will create later
                    }
                }

                if (jcrSession != null) {
                    ((HstMutableRequestContext) requestContext).setSession(jcrSession);
                }
                context.invokeNext();

                if (jcrSession != null && jcrSession.hasPendingChanges()) {
                    log.warn("Request to {} triggered changes in JCR session that were not saved - they will be lost", servletRequest.getPathInfo());
                }
            } catch (LoginException e) {
                // the credentials of the current CMS user have changed, so reset the current authentication
                log.info("Credentials of CMS user '{}' are no longer valid, resetting its HTTP session and starting the SSO handshake again.",
                        cmsSessionContext.getRepositoryCredentials().getUserID());
                httpSession.invalidate();
                redirectToCms(servletRequest, servletResponse, requestContext, null);
                return;
            } catch (RepositoryException e) {
                log.warn("RepositoryException : {}", e.toString());
                throw new ContainerException(e);
            } finally {
                if (jcrSession != null) {
                    try {
                        if (jcrSession.isLive() && jcrSession.hasPendingChanges()) {
                            log.warn("JcrSession '{}' had pending changes at the end of the request. This should never be " +
                                    "the case. Removing the changes now because the session will be reused.", jcrSession.getUserID());
                            jcrSession.refresh(false);
                        }
                    } catch (RepositoryException e) {
                        log.error("RepositoryException while checking / clearing jcr session.", e);
                        throw new ContainerException(e);
                    }
                }
            }
        }
    }

    private static void sendError(final HttpServletResponse servletResponse, final int errorCode) throws ContainerException {
        try {
            servletResponse.sendError(errorCode);
        } catch (IOException e) {
            throw new ContainerException(String.format("Unable to send unauthorized (%s) response to client", errorCode) , e);
        }
    }

    private static void redirectToCms(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse,
                                      final HstRequestContext requestContext, final String cmsContextServiceId) throws ContainerException {

        if (servletRequest.getParameterMap().containsKey("retry")) {
            // endless redirect loop protection
            // in case the loadbalancer keeps skewing the CMS and HST application from different container instances
            sendError(servletResponse, HttpServletResponse.SC_CONFLICT);
            return;
        }

        try {
            final String cmsAuthUrl = createCmsAuthenticationUrl(servletRequest, requestContext, cmsContextServiceId);
            servletResponse.sendRedirect(cmsAuthUrl);
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to encode the destination url with UTF8 encoding " + e.getMessage(), e);
            sendError(servletResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            log.error("Something gone wrong so stopping valve invocation fall through: " + e.getMessage(), e);
            sendError(servletResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static String createCmsAuthenticationUrl(final HttpServletRequest servletRequest, final HstRequestContext requestContext, final String cmsContextServiceId) throws ContainerException {
        final String farthestRequestUrlPrefix = getFarthestUrlPrefix(servletRequest);
        final String cmsLocation = getCmsLocationByPrefix(requestContext, farthestRequestUrlPrefix);
        final String destinationPath = createDestinationPath(servletRequest, requestContext);

        final StringBuilder authUrl = new StringBuilder(cmsLocation);
        if (!cmsLocation.endsWith("/")) {
            authUrl.append("/");
        }
        authUrl.append("auth?destinationPath=").append(destinationPath);
        if (cmsContextServiceId != null) {
            authUrl.append("&cmsCSID=").append(cmsContextServiceId);
        }
        return authUrl.toString();
    }

    private static String getFarthestUrlPrefix(final HttpServletRequest servletRequest) {
        final String farthestRequestScheme = HstRequestUtils.getFarthestRequestScheme(servletRequest);
        final String farthestRequestHost = HstRequestUtils.getFarthestRequestHost(servletRequest, false);
        return farthestRequestScheme + "://" + farthestRequestHost;
    }

    private static String getCmsLocationByPrefix(final HstRequestContext requestContext, final String prefix) throws ContainerException {
        final Mount mount = requestContext.getResolvedMount().getMount();
        final List<String> cmsLocations = mount.getCmsLocations();
        for (String cmsLocation : cmsLocations) {
            if (cmsLocation.startsWith(prefix)) {
                return cmsLocation;
            }
        }
        throw new ContainerException("Could not establish a SSO between CMS & site application because no CMS location could be found that starts with '" + prefix + "'");
    }

    private static String createDestinationPath(final HttpServletRequest servletRequest, final HstRequestContext requestContext) {
        final StringBuilder destinationPath = new StringBuilder();

        // we start with the request uri including the context path (normally this is /site/...)
        destinationPath.append(servletRequest.getRequestURI());

        if (requestContext.getPathSuffix() != null) {
            final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
            final String subPathDelimiter = hstManager.getPathSuffixDelimiter();
            destinationPath.append(subPathDelimiter).append(requestContext.getPathSuffix());
        }

        final String queryString = servletRequest.getQueryString();
        if (queryString != null) {
            destinationPath.append("?").append(queryString);
        }
        return destinationPath.toString();
    }

    private static boolean isPageComposerRequest(final HttpServletRequest servletRequest) {
        return Boolean.TRUE.equals(servletRequest.getAttribute(ContainerConstants.CHANNEL_MGR_PAGE_COMPOSER_REQUEST_CONTEXT));
    }

    private Session getOrCreateCmsChannelManagerRestSession(final HttpServletRequest request) throws LoginException, ContainerException {
        long start = System.currentTimeMillis();

        try {
            final SimpleCredentials credentials = (SimpleCredentials)request.getAttribute(ContainerConstants.CMS_REQUEST_REPO_CREDS_ATTR);
            final Session session = HttpSessionBoundJcrSessionHolder.getOrCreateJcrSession(HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CHANNEL_MNGR_SESSION,
                    request.getSession(), credentials, sessionSecurityDelegation::getDelegatedSession);
            // This returns a plain session for credentials where access is not merged with for example preview user session
            log.debug("Acquiring cms rest session took '{}' ms.", (System.currentTimeMillis() - start));
            return session;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerException("Failed to create session based on SSO.", e);
        }
    }

    private Session getOrCreateCmsPreviewSession(final HttpServletRequest request) throws LoginException, ContainerException {
        long start = System.currentTimeMillis();
        SimpleCredentials cmsUserCred = (SimpleCredentials)request.getAttribute(ContainerConstants.CMS_REQUEST_REPO_CREDS_ATTR);
        try {

            final Session session = HttpSessionBoundJcrSessionHolder.getOrCreateJcrSession(HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CMS_PREVIEW_SESSION,
                    request.getSession(), cmsUserCred, credentials -> sessionSecurityDelegation.createPreviewSecurityDelegate(credentials, false));
            log.debug("Acquiring security delegate session took '{}' ms.", (System.currentTimeMillis() - start));
            return session;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerException("Failed to create Session based on SSO.", e);
        }
    }
}
