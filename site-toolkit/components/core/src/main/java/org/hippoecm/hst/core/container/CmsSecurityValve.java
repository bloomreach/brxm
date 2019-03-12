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

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.SessionSecurityDelegation;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.utilities.servlet.HttpSessionBoundJcrSessionHolder;

/**
 * <p>
 * CmsSecurityValve responsible for authenticating the user using CMS to render a preview of the website.
 * </p>
 * <p> This valve check if the CMS has provided encrypted credentials or not if and only if the
 * page request is done from the CMS context. This valve checks if the CMS has provided encrypted credentials or not.
 * If
 * the credentials are _not_ available
 * with the URL, this valve will redirect to the CMS auth URL with a secret. If the credentials are  available with the
 * URL, this valve will try to get the session for the credentials and continue. </p>
 *
 */
public class CmsSecurityValve extends AbstractBaseOrderableValve {

    public static final String HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CMS_PREVIEW_SESSION = CmsSecurityValve.class.getName() + ".CmsPreviewSession";

    private SessionSecurityDelegation sessionSecurityDelegation;

    public void setSessionSecurityDelegation(SessionSecurityDelegation sessionSecurityDelegation) {
        this.sessionSecurityDelegation = sessionSecurityDelegation;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        final HttpServletRequest servletRequest = context.getServletRequest();
        final HstRequestContext requestContext = context.getRequestContext();

        if (!requestContext.isCmsRequest()) {
            context.invokeNext();
            return;
        }

        log.debug("Request '{}' is invoked from CMS context. Check whether the SSO handshake is done.", servletRequest.getRequestURL());

        final HttpSession httpSession = servletRequest.getSession(false);
        final CmsSessionContext cmsSessionContext = httpSession != null ? CmsSessionContext.getContext(httpSession) : null;

        if (httpSession == null || cmsSessionContext == null) {
            throw new ContainerException("Request is a cms request but there has not been an SSO handshake.");
        }

        // We synchronize on http session to disallow concurrent requests for the Channel manager.
        synchronized (httpSession) {
            Session jcrSession = null;
            try {
                // request preview website, for example in channel manager. The request is not
                // a REST call
                if (sessionSecurityDelegation.sessionSecurityDelegationEnabled()) {
                    jcrSession = getOrCreateCmsPreviewSession(servletRequest, cmsSessionContext.getRepositoryCredentials());
                } else {
                    // do not yet create a session. just use the one that the HST container will create later
                }

                if (jcrSession != null) {
                    ((HstMutableRequestContext) requestContext).setSession(jcrSession);
                }
                context.invokeNext();

                if (jcrSession != null && jcrSession.hasPendingChanges()) {
                    log.warn("Request to {} triggered changes in JCR session that were not saved - they will be lost",
                            servletRequest.getPathInfo());
                }
            } catch (LoginException e) {
                // the credentials of the current CMS user have changed, so reset the current authentication
                log.info("Credentials of CMS user '{}' are no longer valid, resetting its HTTP session and starting the SSO handshake again.",
                        cmsSessionContext.getRepositoryCredentials().getUserID());
                httpSession.invalidate();
                throw new ContainerException("CMS user credentials have changed");
            } catch (RepositoryException e) {
                log.warn("RepositoryException : {}", e.toString());
                throw new ContainerException(e);
            } finally {
                if (jcrSession != null) {
                    try {
                        if (jcrSession.isLive() && jcrSession.hasPendingChanges()) {
                            log.warn("JcrSession '{}' had pending changes at the end of the request. This should never be " +
                                    "the case. Removing the changes now because the session will be reused.", jcrSession.getUserID());
                        }
                        if (jcrSession instanceof HippoSession) {
                            ((HippoSession)jcrSession).localRefresh();
                        } else {
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

    private Session getOrCreateCmsPreviewSession(final HttpServletRequest request, final SimpleCredentials cmsUserCred) throws LoginException, ContainerException {
        long start = System.currentTimeMillis();
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
