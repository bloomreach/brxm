/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.cmsrest.container;

import java.io.IOException;
import java.security.SignatureException;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.sso.CredentialCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CmsRestSecurityValve responsible for authenticating CMS REST calls and sets the correct session on the request context
 */
public class CmsRestSecurityValve extends AbstractOrderableValve {

    private final static Logger log = LoggerFactory.getLogger(CmsRestSecurityValve.class);

    private static final String CREDENTIAL_CIPHER_KEY = "ENC_DEC_KEY";

    private static final String HEADER_CMS_REST_CREDENTIALS = "X-CMSREST-CREDENTIALS";

    private static final String ERROR_MESSAGE_NO_CMS_REST_CREDENTIALS_FOUND = "no CMS REST credentials found";

    private Repository repository;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();

        if(!requestContext.isCmsRequest()) {
            setResponseError(HttpServletResponse.SC_BAD_REQUEST, servletResponse, "Bad CMS REST call");
            return;
        }

        log.debug("Request '{}' is invoked from CMS context. Check for credentials and apply security rules or raise proper error!", servletRequest.getRequestURL());
        // Retrieve encrypted CMS REST username and password and use them as credentials to create a JCR session
        String cmsRestCredentials = servletRequest.getHeader(HEADER_CMS_REST_CREDENTIALS);

        if (StringUtils.isBlank(cmsRestCredentials)) {
            log.debug("No CMS REST credentials found");
            setResponseError(HttpServletResponse.SC_BAD_REQUEST, servletResponse, ERROR_MESSAGE_NO_CMS_REST_CREDENTIALS_FOUND);
            return;
        }

        Session cmsSession = null;
        try {
            CredentialCipher credentialCipher = CredentialCipher.getInstance();
            Credentials credentials = credentialCipher.decryptFromString(CREDENTIAL_CIPHER_KEY, cmsRestCredentials);
            cmsSession = repository.login(credentials);
            ((HstMutableRequestContext) requestContext).setSession(cmsSession);
            context.invokeNext();
        } catch (SignatureException se) {
            log.warn("SignatureException while processing CMS REST credentails : {}", se.toString());
            setResponseError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse);
            return;
        } catch (LoginException e) {
            log.warn("LoginException ", e.toString());
            setResponseError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse);
            return;
        }  catch (RepositoryException e) {
            log.warn("RepositoryException ", e.toString());
            setResponseError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse);
            return;
        } finally {
            if (cmsSession != null) {
                cmsSession.logout();
            }
        }
    }

    protected void setResponseError(int scError, HttpServletResponse response) {
        setResponseError(scError, response, null);
    }

    protected void setResponseError(int scError, HttpServletResponse response, String message) {
        try {
            if (StringUtils.isBlank(message)) {
                response.sendError(scError);
            } else {
                response.sendError(scError, message);
            }
        } catch (IOException ioe) {
            log.warn("Exception while sending HTTP error response", ioe);
        }
    }

}
