/*
 *  Copyright 2011-2020 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.tuple.Pair;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.SessionSecurityDelegationImpl;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.container.security.AccessToken;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.utilities.servlet.HttpSessionBoundJcrSessionHolder;
import org.onehippo.cms7.utilities.servlet.HttpSessionBoundJcrSessionHolder.JcrSessionCreator;
import org.onehippo.repository.security.domain.DomainRuleExtension;

import static org.hippoecm.hst.core.container.ContainerConstants.CMS_USER_SESSION_ATTR_NAME;
import static org.hippoecm.hst.core.container.ContainerConstants.PREVIEW_ACCESS_TOKEN_REQUEST_ATTRIBUTE;

/**
 * <p> This valve check if the reqquest is a channel manager preview request, and if it is so, provides a correct
 * JCR Session which is a combination of the access of the current logged in CMS User constrained by only access
 * on the preview documents + the access of the HST preview user. This is done by making use of
 * {@link HippoSession#createSecurityDelegate(Session, DomainRuleExtension...)}.
 * </p>
 * <p>
 * If the credentials or token is _not_ available, this valve throw an exception in case the request is for a
 * channel manager preview request
 * </p>
 */
public class CmsSecurityValve extends AbstractBaseOrderableValve {

    public static final String HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CMS_PREVIEW_SESSION = CmsSecurityValve.class.getName() + ".CmsPreviewSession";
    public static final String HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CMS_SESSION = CmsSecurityValve.class.getName() + ".CmsSession";

    private SessionSecurityDelegationImpl sessionSecurityDelegation;

    // non-pooled repository
    private Repository delegatingRepository;

    public void setSessionSecurityDelegation(SessionSecurityDelegationImpl sessionSecurityDelegation) {
        this.sessionSecurityDelegation = sessionSecurityDelegation;
    }

    public void setDelegatingRepository(final Repository delegatingRepository) {
        this.delegatingRepository = delegatingRepository;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        final HttpServletRequest servletRequest = context.getServletRequest();
        final HstRequestContext requestContext = context.getRequestContext();

        if (!requestContext.isChannelManagerPreviewRequest()) {
            context.invokeNext();
            return;
        }

        final SimpleCredentials cmsUserCredentials;

        final AccessToken accessToken = (AccessToken) servletRequest.getAttribute(PREVIEW_ACCESS_TOKEN_REQUEST_ATTRIBUTE);
        if (accessToken != null) {
            // render on behalf of authorized user
            log.debug("Request '{}' is invoked with a valid token", accessToken);
            cmsUserCredentials = accessToken.getCmsSessionContext().getRepositoryCredentials();
            Session previewCmsUserSession = null;
            Session cmsUserSession = null;
            try {
                Pair<Session, Session> pair = sessionSecurityDelegation.createCmsUserAndChannelMgrPreviewUser(cmsUserCredentials);

                previewCmsUserSession = pair.getLeft();
                cmsUserSession = pair.getRight();

                ((HstMutableRequestContext) requestContext).setSession(previewCmsUserSession);
                requestContext.setAttribute(CMS_USER_SESSION_ATTR_NAME, cmsUserSession);

                context.invokeNext();
            } catch (RepositoryException e) {
                log.warn("RepositoryException : {}", e.toString());
                throw new ContainerException(e);
            } finally {
                logoutSession(previewCmsUserSession);
                logoutSession(cmsUserSession);
            }

        } else {

            log.debug("Request '{}' is invoked from CMS context. Check whether the SSO handshake is done.", servletRequest.getRequestURL());


            final HttpSession httpSession = servletRequest.getSession(false);
            final CmsSessionContext cmsSessionContext = httpSession != null ? CmsSessionContext.getContext(httpSession) : null;

            if (httpSession == null || cmsSessionContext == null) {
                throw new ContainerException("Request is a channel manager request but there has not been an SSO handshake.");
            }
            cmsUserCredentials = cmsSessionContext.getRepositoryCredentials();
            // We synchronize on http session to disallow concurrent requests for the Channel manager.
            synchronized (httpSession) {
                Session previewCmsUserSession = null;
                Session cmsUserSession = null;
                try {
                    // request preview website, for example in channel manager. The request is not
                    // a REST call
                    if (sessionSecurityDelegation.sessionSecurityDelegationEnabled()) {
                        previewCmsUserSession = getOrCreateHttpSessionBoundJcrSession(httpSession, cmsUserCredentials,
                                HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CMS_PREVIEW_SESSION,
                                credentials -> sessionSecurityDelegation.createPreviewSecurityDelegate(credentials, false));
                    } else {
                        // do not yet create a session. just use the one that the HST container will create later
                    }

                    cmsUserSession = getOrCreateHttpSessionBoundJcrSession(httpSession, cmsUserCredentials,
                            HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CMS_SESSION, credentials -> delegatingRepository.login(credentials));
                    requestContext.setAttribute(CMS_USER_SESSION_ATTR_NAME, cmsUserSession);

                    if (previewCmsUserSession != null) {
                        ((HstMutableRequestContext) requestContext).setSession(previewCmsUserSession);
                    }
                    context.invokeNext();

                    if (previewCmsUserSession != null && previewCmsUserSession.hasPendingChanges()) {
                        log.warn("Request to {} triggered changes in JCR session that were not saved - they will be lost",
                                servletRequest.getPathInfo());
                    }
                } catch (LoginException e) {
                    // the credentials of the current CMS user have changed, so reset the current authentication
                    log.info("CMS user '{}' is not longer a valid user, resetting HTTP session and starting the SSO handshake again.",
                            cmsUserCredentials.getUserID());
                    httpSession.invalidate();
                    throw new ContainerException("CMS user credentials have changed");
                } catch (RepositoryException e) {
                    log.warn("RepositoryException : {}", e.toString());
                    throw new ContainerException(e);
                } finally {
                    validatePristine(previewCmsUserSession);
                    validatePristine(cmsUserSession);
                }
            }
        }
    }

    private void logoutSession(final Session session) throws ContainerException {
        if (session != null) {
            try {
                if (session.isLive() && session.hasPendingChanges()) {
                    log.error("JcrSession '{}' had pending changes at the end of the request. This should never be " +
                            "the case. Removing the changes now because the session will be reused.", session.getUserID());
                }
            } catch (RepositoryException e) {
                log.error("RepositoryException while checking jcr session.", e);
                throw new ContainerException(e);
            }
            session.logout();
        }
    }

    private void validatePristine(final Session previewCmsUserSession) throws ContainerException {
        if (previewCmsUserSession != null) {
            try {
                if (previewCmsUserSession.isLive() && previewCmsUserSession.hasPendingChanges()) {
                    log.error("Session '{}' had pending changes at the end of the request. This should never be " +
                            "the case. Removing the changes now because the session will be reused.", previewCmsUserSession.getUserID());
                }
                if (previewCmsUserSession instanceof HippoSession) {
                    ((HippoSession) previewCmsUserSession).localRefresh();
                } else {
                    previewCmsUserSession.refresh(false);
                }
            } catch (RepositoryException e) {
                log.error("RepositoryException while checking / clearing jcr session.", e);
                throw new ContainerException(e);
            }
        }
    }

    /**
     * <p>
     *     Returns a JCR Session that gets automatically logged out when the http session gets invalidated
     * </p>
     */
    private Session getOrCreateHttpSessionBoundJcrSession(final HttpSession httpSession,
                                                          final SimpleCredentials cmsUserCred,
                                                          final String httpSessionAttrName,
                                                          final JcrSessionCreator jcrSessionCreator) throws LoginException, ContainerException {
        long start = System.currentTimeMillis();
        try {

            final Session session = HttpSessionBoundJcrSessionHolder.getOrCreateJcrSession(httpSessionAttrName,
                    httpSession, cmsUserCred, credentials -> jcrSessionCreator.login(credentials));
            log.debug("Acquiring session took '{}' ms.", (System.currentTimeMillis() - start));
            return session;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerException("Failed to create Session based on SSO.", e);
        }
    }

}
