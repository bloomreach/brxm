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

import java.io.IOException;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for CMS REST {@link Valve}(s)
 */
public abstract class BaseCmsRestValve extends AbstractValve {

    private final static Logger log = LoggerFactory.getLogger(BaseCmsRestValve.class);

    protected static final String HEADER_CMS_REST_CREDENTIALS = "X-CMSREST-CREDENTIALS";

    protected static final String ERROR_MESSAGE_BAD_CMS_REST_CALL = "Bad CMS REST call!";
    protected static final String ERROR_MESSAGE_EXCEPTION_WHILE_SENDING_ERROR = "Exception while sending error response";
    protected static final String ERROR_MESSAGE_REQUEST_PROCESSING_CHAIN_ERROR = "Error while processing CMS REST call - %s : %s : %s";

    @Override
    public abstract void invoke(ValveContext context);

    protected void propagateCrendentials(HttpSession httpSession, Credentials credentials) {
        httpSession.setAttribute(CREDENTIALS_ATTRIBUTE_NAME, credentials);
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
            if (log.isErrorEnabled()) {
                log.error(ERROR_MESSAGE_EXCEPTION_WHILE_SENDING_ERROR, ioe);
            }
        }
    }

    protected void logError(Logger logger, String message) {
        if (logger.isErrorEnabled()) {
            logger.error(message);
        }
    }

    protected void logWarning(Logger logger, String message) {
        if (logger.isWarnEnabled()) {
            logger.warn(message);
        }
    }

    protected void logDebug(Logger logger, String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }

}
