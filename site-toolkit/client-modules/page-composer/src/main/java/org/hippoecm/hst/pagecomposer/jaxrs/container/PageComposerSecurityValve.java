/*
 *  Copyright 2018-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.container;

import java.io.IOException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.container.AbstractBaseOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.utilities.servlet.HttpSessionBoundJcrSessionHolder;

public class PageComposerSecurityValve extends AbstractBaseOrderableValve {

    public static final String HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CHANNEL_MNGR_SESSION = PageComposerSecurityValve.class.getName() + ".CmsChannelManagerRestSession";

    private Repository repository;

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HstRequestContext requestContext = context.getRequestContext();

        // mark the request to be a channel manager REST request such that code like HstSiteProvider implementations
        // can behave differently
        ((HstMutableRequestContext) requestContext).setChannelManagerRestRequest();

        final HttpSession cmsHttpSession = servletRequest.getSession(false);

        if (cmsHttpSession == null) {
            sendError(context.getServletResponse(), HttpServletResponse.SC_UNAUTHORIZED);
        }

        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(cmsHttpSession);

        if (cmsSessionContext == null) {
            sendError(context.getServletResponse(), HttpServletResponse.SC_UNAUTHORIZED);
        }

        // We synchronize on http session to disallow concurrent requests for the Channel manager.
        synchronized (cmsHttpSession) {
            Session jcrSession = null;
            try {

                jcrSession = getOrCreateCmsChannelManagerRestSession(cmsHttpSession, cmsSessionContext.getRepositoryCredentials());
                ((HstMutableRequestContext) requestContext).setSession(jcrSession);

                context.invokeNext();

            } catch (RepositoryException e) {
                log.warn("RepositoryException : {}", e.toString());
                throw new ContainerException(e);
            } finally {
                if (jcrSession != null) {
                    try {
                        if (jcrSession.isLive() && jcrSession.hasPendingChanges()) {
                            log.warn("JcrSession '{}' had pending changes at the end of the page composer request. This should never be " +
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


    private Session getOrCreateCmsChannelManagerRestSession(final HttpSession cmsHttpSession,
                                                            final SimpleCredentials credentials) throws RepositoryException, ContainerException {
        long start = System.currentTimeMillis();

        try {
            final Session session = HttpSessionBoundJcrSessionHolder.getOrCreateJcrSession(HTTP_SESSION_ATTRIBUTE_NAME_PREFIX_CHANNEL_MNGR_SESSION,
                 cmsHttpSession, credentials, repository::login);
            // This returns a plain session for credentials where access is not merged with for example preview user session
            log.debug("Acquiring cms rest session took '{}' ms.", (System.currentTimeMillis() - start));
            return session;
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerException("Failed to create session based on SSO.", e);
        }
    }

    private static void sendError(final HttpServletResponse servletResponse, final int errorCode) throws ContainerException {
        try {
            servletResponse.sendError(errorCode);
        } catch (IOException e) {
            throw new ContainerException(String.format("Unable to send unauthorized (%s) response to client", errorCode) , e);
        }
    }
}

