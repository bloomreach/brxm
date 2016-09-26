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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class UserSessionProviderTest {
    private Session systemSession = createMock(Session.class);
    private UserSessionProvider provider = new UserSessionProvider(systemSession) {
        @Override
        protected Object invokeSuper(Exchange exchange, Object requestParams) {
            final Map<String, Object> map = new HashMap<>();
            map.put("exchange", exchange);
            map.put("requestParams", requestParams);
            return map;
        }
    };
    private HttpServletRequest servletRequest = createMock(HttpServletRequest.class);

    @Test
    public void readTheUserSessionFromTheHttpServletRequest() {
        final Session userSession = createMock(Session.class);
        final String attributeName = UserSessionProvider.class.getName() + ".UserSession";
        expect(servletRequest.getAttribute(attributeName)).andReturn(userSession);
        replay(servletRequest);

        final Session retrievedSession = provider.get(servletRequest);

        verify(servletRequest);
        assertThat(retrievedSession, equalTo(userSession));
    }

    @Test
    public void returnNullIfTheresNoUserSession() {
        final String attributeName = UserSessionProvider.class.getName() + ".UserSession";
        expect(servletRequest.getAttribute(attributeName)).andReturn(null);
        replay(servletRequest);

        final Session retrievedSession = provider.get(servletRequest);

        verify(servletRequest);
        assertThat(retrievedSession, equalTo(null));
    }

    @Test
    public void returnForbiddenIfUsernameMissing() {
        final Exchange exchange = prepareHippoUsername(null);
        replay(exchange);

        final Object result = provider.invoke(exchange, "test");

        verify(exchange);
        assertForbidden(result);
    }

    @Test
    public void returnForbiddenIfUsernameEmpty() {
        final Exchange exchange = prepareHippoUsername("");
        replay(exchange);

        final Object result = provider.invoke(exchange, "test");

        verify(exchange);
        assertForbidden(result);
    }

    @Test
    public void invokeJaxrs() throws Exception {
        final Session userSession = createMock(Session.class);
        userSession.logout();
        expectLastCall();
        expect(systemSession.impersonate(anyObject())).andReturn(userSession);
        final Exchange exchange = prepareHippoUsername("tester");
        prepareServletRequest(exchange);
        servletRequest.setAttribute(UserSessionProvider.class.getName() + ".UserSession", userSession);
        expectLastCall();
        replay(systemSession, userSession, servletRequest, exchange);

        final Object result = provider.invoke(exchange, "test");

        verify(systemSession, userSession, servletRequest, exchange);
        assertThat("a map is returned", result instanceof Map);
        final Map<String, Object> map = (Map<String, Object>)result;
        assertThat(map.get("exchange"), equalTo(exchange));
        assertThat(map.get("requestParams"), equalTo("test"));
    }

    @Test
    public void returnForbiddenIfImpersonationFails() throws Exception {
        expect(systemSession.impersonate(anyObject())).andThrow(new RepositoryException());
        final Exchange exchange = prepareHippoUsername("tester");
        replay(systemSession, exchange);

        final Object result = provider.invoke(exchange, "test");

        verify(systemSession, exchange);
        assertForbidden(result);
    }

    @Test
    public void logoutInSpiteOfUnexpectedException() throws Exception {
        final RuntimeException testException = new RuntimeException();
        final UserSessionProvider exceptionThrowingProvider = new UserSessionProvider(systemSession) {
            @Override
            protected Object invokeSuper(Exchange exchange, Object requestParams) {
                throw testException;
            }
        };

        final Session userSession = createMock(Session.class);
        userSession.logout();
        expectLastCall();
        expect(systemSession.impersonate(anyObject())).andReturn(userSession);
        final Exchange exchange = prepareHippoUsername("tester");
        prepareServletRequest(exchange);
        servletRequest.setAttribute(UserSessionProvider.class.getName() + ".UserSession", userSession);
        expectLastCall();
        replay(systemSession, userSession, servletRequest, exchange);

        try {
            exceptionThrowingProvider.invoke(exchange, "test");
            assertThat("we don't get here", false);
        } catch (Exception e) {
            verify(systemSession, userSession, servletRequest, exchange);
            assertThat(e, equalTo(testException));
        }
    }

    private Exchange prepareHippoUsername(String userName) {
        final org.apache.cxf.transport.Session cxfSession = createMock(org.apache.cxf.transport.Session.class);
        final Exchange exchange = createMock(Exchange.class);

        expect(exchange.getSession()).andReturn(cxfSession);
        expect(cxfSession.get("hippo:username")).andReturn(userName);
        replay(cxfSession);

        return exchange;
    }

    private void prepareServletRequest(final Exchange exchange) {
        final Message message = createMock(Message.class);
        expect(message.get(AbstractHTTPDestination.HTTP_REQUEST)).andReturn(servletRequest);
        expect(exchange.getInMessage()).andReturn(message);
        replay(message);
    }

    private void assertForbidden(final Object result) {
        assertThat(result instanceof MessageContentsList, equalTo(true));
        final MessageContentsList mcl = (MessageContentsList) result;
        assertThat(mcl.get(0) instanceof Response, equalTo(true));
        final Response response = (Response) mcl.get(0);
        assertThat(response.getStatus(), equalTo(403));
    }
}
