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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage and provide a user session, authenticated for the current HTTP session's hippo:username.
 * When this invoker is invoked while no user is logged in, a 403 forbidden error is returned.
 */
public class UserSessionProvider extends JAXRSInvoker {
    private static final Logger log = LoggerFactory.getLogger(UserSessionProvider.class);
    private static final String SESSION_ATTRIBUTE = UserSessionProvider.class.getName() + ".UserSession";
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
        Object result = null;

        try {
            if (StringUtils.isBlank(userId)) {
                throw new UserUnknownException();
            }
            final Session userSession = systemSession.impersonate(new SimpleCredentials(userId, new char[]{}));
            final HttpServletRequest servletRequest = getServletRequest(exchange);
            servletRequest.setAttribute(SESSION_ATTRIBUTE, userSession);
            try {
                result = invokeSuper(exchange, requestParams);
            } finally {
                userSession.logout();
            }
        } catch (UserUnknownException|RepositoryException e) {
            log.debug("Failed to create user session", e);
            result = new MessageContentsList(Response.status(Response.Status.FORBIDDEN).build());
        }

        return result;
    }

    // extracted call to super for better testability
    protected Object invokeSuper(Exchange exchange, Object requestParams) {
        return super.invoke(exchange, requestParams);
    }

    private HttpServletRequest getServletRequest(Exchange exchange) {
        return (HttpServletRequest)exchange.getInMessage().get("HTTP.REQUEST");
    }

    private static class UserUnknownException extends Exception { }
}
