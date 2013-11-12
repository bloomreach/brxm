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

package org.hippoecm.hst.core.container;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for CMS REST {@link Valve}(s)
 */
public abstract class BaseCmsRestValve extends AbstractBaseOrderableValve {

    private final static Logger log = LoggerFactory.getLogger(BaseCmsRestValve.class);

    private static final String CREDENTIALS_ATTRIBUTE_NAME = BaseCmsRestValve.class.getName() + "_CMS_REST_CREDENTIALS";

    @Override
    public abstract void invoke(ValveContext context);

    protected void propagateCrendentials(HttpServletRequest request, Credentials credentials) {
        request.setAttribute(CREDENTIALS_ATTRIBUTE_NAME, credentials);
    }

    protected Credentials getPropagatedCredentials(HttpServletRequest request) {
        return (Credentials)request.getAttribute(CREDENTIALS_ATTRIBUTE_NAME);
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
