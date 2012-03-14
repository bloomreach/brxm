/*
 *  Copyright 2012 Hippo.
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

import static org.hippoecm.hst.core.container.CmsRestValvesConsts.CREDENTIALS_ATTRIBUTE_NAME;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Valve} class responsible for initiating a JCR Session based on {@link HttpSession} propagated {@link Credentials}
 */
public class CmsRestCredentialsToJcrSessionPropagatorValve extends BaseCmsRestValve {

    private static final Logger log = LoggerFactory.getLogger(CmsRestCredentialsToJcrSessionPropagatorValve.class);

    private static final String WARNING_MESSAGE_SECURITY_CONTEXT_NOT_FOUND = "Security context not found!";
    private static final String ERROR_MESSAGE_JCR_SESSION_PROPAGATION_ERROR = "Error while propagating JCR Session! - %s : %s : %s";

    private Repository repository;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void invoke(ValveContext context) {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();

        if(!requestContext.isCmsRequest()) {
            setResponseError(HttpServletResponse.SC_BAD_REQUEST, servletResponse, ERROR_MESSAGE_BAD_CMS_REST_CALL);
            return;
        }

        HttpSession session = servletRequest.getSession();
        Credentials credentials = (Credentials) session.getAttribute(CREDENTIALS_ATTRIBUTE_NAME);

        if (credentials == null) {
            logWarning(log, WARNING_MESSAGE_SECURITY_CONTEXT_NOT_FOUND);
             // setResponseError(HttpServletResponse.SC_UNAUTHORIZED, servletResponse);
             // return;
        }

        try {
            propagateJcrSession(credentials);
            context.invokeNext();
            dePropagateJcrSession();
        } catch (ContainerException ce) {
            logError(log, String.format(ERROR_MESSAGE_JCR_SESSION_PROPAGATION_ERROR, ce.getClass().getName(), ce.getMessage(), ce));
            setResponseError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse);
        }
    }

    protected void propagateJcrSession(Credentials credentials) throws ContainerException {
        try {
            // COMMENT - MNour: There should be a better way to make this more clean middleware oriented implementation
            if (credentials == null) {
                // Create a read only JCR session
                CmsJcrSessionThreadLocal.setJcrSession(repository.login());
            } else {
                // Create read/write JCR session
                CmsJcrSessionThreadLocal.setJcrSession(repository.login(credentials));
            }
        } catch (LoginException le) {
            throw new ContainerException(le);
        } catch (RepositoryException re) {
            throw new ContainerException(re);
        }
    }

    protected void dePropagateJcrSession() {
        // QUESTION: Should we make the valve responsible for login out from JCR session
        CmsJcrSessionThreadLocal.clearJcrSession();
    }

}
