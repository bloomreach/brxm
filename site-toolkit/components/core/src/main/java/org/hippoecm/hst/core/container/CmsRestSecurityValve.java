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

import java.security.SignatureException;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.sso.CredentialCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CmsRestSecurityValve responsible for authenticating CMS REST calls and then then propagates 
 * the relevant {@link Credentials}
 */
public class CmsRestSecurityValve extends BaseCmsRestValve {

    private final static Logger log = LoggerFactory.getLogger(CmsRestSecurityValve.class);

    private static final String WARNING_MESSAGE_NO_CMS_REST_CREDENTIALS_FOUND = "No CMS REST credentials found!";
    private static final String ERROR_MESSAGE_CREDENTIALS_PROCESSING_ERROR = "Error while processing CMS REST credentails - %s : %s : %s";
    // COMMENT - MNour: This is really bad workaround but it is used only for the time being
    private static final String CREDENTIAL_CIPHER_KEY = "ENC_DEC_KEY";

    @Override
    public void invoke(ValveContext context) {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();

        if(!doesCallComeFromCms(requestContext, servletRequest)) {
            setResponseError(HttpServletResponse.SC_BAD_REQUEST, servletResponse, ERROR_MESSAGE_BAD_CMS_REST_CALL);
            return;
        }

        logDebug(log, String.format("Request '%s' is invoked from CMS context. Check for credentials and apply security rules or raise proper error!", servletRequest.getRequestURL()));
        HttpSession session = servletRequest.getSession(true);
        // Retrieve encrypted CMS REST username and password and use them as credentials to create a JCR session
        String cmsRestCredentials = servletRequest.getHeader(HEADER_CMS_REST_CREDENTIALS);

        if (StringUtils.isBlank(cmsRestCredentials)) {
            logWarning(log, WARNING_MESSAGE_NO_CMS_REST_CREDENTIALS_FOUND);
            // COMMENT - MNour: That is wrong!
            // setResponseError(HttpServletResponse.SC_BAD_REQUEST, servletResponse, ERROR_MESSAGE_NO_CMS_REST_CREDENTIALS_FOUND);
            // return;
        }

        try {
            if (StringUtils.isNotBlank(cmsRestCredentials)) {
                CredentialCipher credentialCipher = CredentialCipher.getInstance();
                Credentials cred = credentialCipher.decryptFromString(CREDENTIAL_CIPHER_KEY, cmsRestCredentials);
                propagateCrendentials(session, cred);
            }
            context.invokeNext();
        } catch (SignatureException se) {
            ContainerException ce = new ContainerException(se);
            logError(log, String.format(ERROR_MESSAGE_CREDENTIALS_PROCESSING_ERROR, ce.getClass().getName(), ce.getMessage(), ce));
            setResponseError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse);
            return;
        } catch (ContainerException ce) {
            logError(log, String.format(ERROR_MESSAGE_REQUEST_PROCESSING_CHAIN_ERROR, ce.getClass().getName(), ce.getMessage(), ce));
            setResponseError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse);
            return;
        }
    }

}
