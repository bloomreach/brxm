/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides and manages a JCR session authenticated for the current logged in user.
 * The name of the logged is user is read from the HTTP session attribute 'hippo:username'.
 * Returns a 403 forbidden error when invoked while no user is logged in.
 */
public class UserSessionProvider extends JAXRSInvoker {

    private static final Logger log = LoggerFactory.getLogger(UserSessionProvider.class);

    private static final char[] EMPTY_PASSWORD = new char[]{};
    private static final String SESSION_ATTRIBUTE = UserSessionProvider.class.getName() + ".UserSession";
    private static final MessageContentsList FORBIDDEN = new MessageContentsList(Response.status(Response.Status.FORBIDDEN).build());

    private final Session systemSession;

    public UserSessionProvider(final Session systemSession) {
        this.systemSession = systemSession;
    }

    public Session get(final HttpServletRequest servletRequest) {
        return (Session)servletRequest.getAttribute(SESSION_ATTRIBUTE);
    }

    @Override
    public Object invoke(Exchange exchange, Object requestParams) {
        final String userId = (String)exchange.getSession().get("hippo:username");

        if (StringUtils.isBlank(userId)) {
            return FORBIDDEN;
        }

        try {
            final Session userSession = systemSession.impersonate(new SimpleCredentials(userId, EMPTY_PASSWORD));
            try {
                final HttpServletRequest servletRequest
                        = (HttpServletRequest) exchange.getInMessage().get(AbstractHTTPDestination.HTTP_REQUEST);
                servletRequest.setAttribute(SESSION_ATTRIBUTE, userSession);
                return invokeSuper(exchange, requestParams);
            } finally {
                userSession.logout();
            }
        } catch (RepositoryException e) {
            log.debug("Failed to create user session for '{}'", userId, e);
            return FORBIDDEN;
        }
    }

    // extracted call to super for better testability
    protected Object invokeSuper(Exchange exchange, Object requestParams) {
        return super.invoke(exchange, requestParams);
    }
}
