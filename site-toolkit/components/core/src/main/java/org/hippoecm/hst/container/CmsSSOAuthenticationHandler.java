/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsContextService;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.utilities.servlet.HttpSessionBoundJcrSessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.CmsSecurityValve.HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CMS_PREVIEW_SESSION;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_REPO_CREDS_ATTR;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_USER_ID_ATTR;
import static org.hippoecm.hst.util.HstRequestUtils.getCmsBaseURL;

public class CmsSSOAuthenticationHandler {

    private final static Logger log = LoggerFactory.getLogger(CmsSSOAuthenticationHandler.class);

    static boolean isAuthenticated(final HstContainerRequest containerRequest, final HttpServletResponse servletResponse) {
        log.debug("Request '{}' is invoked from CMS context. Check whether the SSO handshake is done.", containerRequest.getRequestURL());

        HttpSession httpSession = containerRequest.getSession(false);
        CmsSessionContext cmsSessionContext = httpSession != null ? CmsSessionContext.getContext(httpSession) : null;
        if (httpSession == null || cmsSessionContext == null) {
            return false;
        }

        setRequestAttributes(containerRequest, cmsSessionContext);
        return true;
    }


    /**
     * @return {@code true} if the {@code containerRequest} contains the information to create an SSO handshake with the
     * CMS and it returns {@code false} if the {@code containerRequest} does not contain the required info: When {@core
     * false} is returned, the {@code servletResponse} gets committed already with a redirect or an error
     */
    static boolean authenticate(final HstContainerRequest containerRequest,
                              final HttpServletResponse servletResponse) throws ContainerException {

        log.debug("Request '{}' is invoked from CMS context. Check whether the SSO handshake is done.", containerRequest.getRequestURL());


        CmsContextService cmsContextService = HippoServiceRegistry.getService(CmsContextService.class);
        if (cmsContextService == null) {
            log.debug("No CmsContextService available");
            sendError(servletResponse, HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        final String cmsContextServiceId = containerRequest.getParameter("cmsCSID");
        final String cmsSessionContextId = containerRequest.getParameter("cmsSCID");
        if (cmsContextServiceId == null || cmsSessionContextId == null) {
            // no CmsSessionContext and/or CmsContextService IDs provided:  if possible, request these by redirecting back to CMS
            final String method = containerRequest.getMethod();
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                log.warn("Invalid request to redirect for authentication because request method is '{}' and only" +
                        " 'GET' or 'HEAD' are allowed", method);
                sendError(servletResponse, HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            log.debug("No CmsSessionContext and/or CmsContextService IDs found. Redirect to the CMS");
            redirectToCms(containerRequest, servletResponse, cmsContextService.getId());
            return false;
        }

        if (!cmsContextServiceId.equals(cmsContextService.getId())) {
            log.warn("Cannot authorize request: not coming from this CMS HOST. Redirecting to cms authentication URL to retry.");
            redirectToCms(containerRequest, servletResponse, cmsContextService.getId());
            return false;
        }


        HttpSession httpSession = containerRequest.getSession(false);

        // if the (HST) http session already exists, it might be because the user logged out from cms and logged in with
        // different user or logged in with same user again but the authorization rules for example changed (that is
        // why the user for example logged out and in). However, now it can happen that we have stale jcr sessions
        // still on the HttpSessionBoundJcrSessionHolder attribute of the HST http session. We need to clear these
        // now actively
        if (httpSession != null) {
            HttpSessionBoundJcrSessionHolder.clearAllBoundJcrSessions(HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CMS_PREVIEW_SESSION, httpSession);
        } else {
            httpSession = containerRequest.getSession(true);
        }

        final CmsSessionContext cmsSessionContext = cmsContextService.attachSessionContext(cmsSessionContextId, httpSession);
        if (cmsSessionContext == null) {
            httpSession.invalidate();
            log.warn("Cannot authorize request: CmsSessionContext not found");
            sendError(servletResponse, HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        log.debug("Authenticated '{}' successfully", cmsSessionContext.getRepositoryCredentials().getUserID());

        setRequestAttributes(containerRequest, cmsSessionContext);

        return true;
    }

    private static void setRequestAttributes(final HstContainerRequest containerRequest, final CmsSessionContext cmsSessionContext) {
        containerRequest.setAttribute(CMS_REQUEST_USER_ID_ATTR, cmsSessionContext.getRepositoryCredentials().getUserID());
        // TODO HSTTWO-4375  remove this attribute once we have addressed HSTTWO-4375
        containerRequest.setAttribute(CMS_REQUEST_REPO_CREDS_ATTR, cmsSessionContext.getRepositoryCredentials());
    }

    private static void sendError(final HttpServletResponse servletResponse, final int errorCode) throws ContainerException {
        try {
            servletResponse.sendError(errorCode);
        } catch (IOException e) {
            throw new ContainerException(String.format("Unable to send unauthorized (%s) response to client", errorCode) , e);
        }
    }

    private static void redirectToCms(final HstContainerRequest containerRequest, final HttpServletResponse servletResponse,
                                      final String cmsContextServiceId) throws ContainerException {

        if (containerRequest.getParameterMap().containsKey("retry")) {
            // endless redirect loop protection
            // in case the loadbalancer keeps skewing the CMS and HST application from different container instances
            sendError(servletResponse, HttpServletResponse.SC_CONFLICT);
            return;
        }

        try {
            final String cmsAuthUrl = createCmsAuthenticationUrl(containerRequest, cmsContextServiceId);
            servletResponse.sendRedirect(cmsAuthUrl);
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to encode the destination url with UTF8 encoding " + e.getMessage(), e);
            sendError(servletResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            log.error("Something gone wrong so stopping valve invocation fall through: " + e.getMessage(), e);
            sendError(servletResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static String createCmsAuthenticationUrl(final HstContainerRequest containerRequest, final String cmsContextServiceId) throws ContainerException {
        // we need to find out whether the cms URL looks like http(s)://host/cms or http(s)://host (without context path)
        // we know that the current request is over the current cms host. We need to match the current host to the
        // platform hst model to find out whether to include the platform context path or not

        final String cmsLocation = getCmsBaseURL(containerRequest);
        final String destinationPath = createDestinationPath(containerRequest);

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

    private static String createDestinationPath(final HstContainerRequest containerRequest) {
        final StringBuilder destinationPath = new StringBuilder();

        // we start with the request uri including the context path (normally this is /site/...)
        destinationPath.append(containerRequest.getRequestURI());

        if (containerRequest.getPathSuffix() != null) {
            final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
            final String subPathDelimiter = hstManager.getPathSuffixDelimiter();
            destinationPath.append(subPathDelimiter).append(containerRequest.getPathSuffix());
        }

        final String queryString = containerRequest.getQueryString();
        if (queryString != null) {
            destinationPath.append("?").append(queryString);
        }
        return destinationPath.toString();
    }

}
