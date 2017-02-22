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
import java.util.Locale;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.junit.Test;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ManagedUserSessionInvokerTest {
    private static final String ATTRIBUTE_SESSION = ManagedUserSessionInvoker.class.getName() + ".UserSession";
    private static final String ATTRIBUTE_LOCALE = ManagedUserSessionInvoker.class.getName() + ".Locale";

    private Session systemSession = createMock(Session.class);
    private ManagedUserSessionInvoker invoker = new ManagedUserSessionInvoker(systemSession) {
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
        expect(servletRequest.getAttribute(ATTRIBUTE_SESSION)).andReturn(userSession);
        replay(servletRequest);

        final Session retrievedSession = invoker.getJcrSession(servletRequest);

        verify(servletRequest);
        assertThat(retrievedSession, equalTo(userSession));
    }

    @Test
    public void returnNullIfTheresNoUserSession() {
        expect(servletRequest.getAttribute(ATTRIBUTE_SESSION)).andReturn(null);
        replay(servletRequest);

        final Session retrievedSession = invoker.getJcrSession(servletRequest);

        verify(servletRequest);
        assertNull(retrievedSession);
    }

    @Test
    public void returnForbiddenIfHttpSessionMissing() {
        final Exchange exchange = prepareExchange();

        expect(servletRequest.getSession(false)).andReturn(null);
        replay(servletRequest);

        final Object result = invoker.invoke(exchange, "test");

        verify(servletRequest);
        assertForbidden(result);
    }

    @Test
    public void returnForbiddenIfNoCmsSessionContext() {
        final Exchange exchange = prepareExchange();
        final HttpSession httpSession = createMock(HttpSession.class);
        final CmsSessionContext context = createMock(CmsSessionContext.class);

        expect(servletRequest.getSession(false)).andReturn(httpSession);
        expect(httpSession.getAttribute(CmsSessionContext.SESSION_KEY)).andReturn(context);

        replay(servletRequest);

        final Object result = invoker.invoke(exchange, "test");

        verify(servletRequest);
        assertForbidden(result);
    }

    @Test
    public void invokeJaxrs() throws Exception {
        final SimpleCredentials credentials = new SimpleCredentials("tester", new char[]{});
        final Locale locale = new Locale("en");
        final Exchange exchange = prepareExchange();
        final HttpSession httpSession = createMock(HttpSession.class);
        final CmsSessionContext context = createMock(CmsSessionContext.class);
        final Repository repository = createMock(Repository.class);
        final Session userSession = createMock(Session.class);

        expect(servletRequest.getSession(false)).andReturn(httpSession);
        expect(httpSession.getAttribute(CmsSessionContext.SESSION_KEY)).andReturn(context);
        expect(context.getRepositoryCredentials()).andReturn(credentials);
        expect(context.getLocale()).andReturn(locale);
        expect(systemSession.getRepository()).andReturn(repository);
        expect(repository.login(credentials)).andReturn(userSession);

        servletRequest.setAttribute(ATTRIBUTE_SESSION, userSession);
        expectLastCall();
        servletRequest.setAttribute(ATTRIBUTE_LOCALE, locale);
        expectLastCall();
        userSession.logout();
        expectLastCall();

        replay(servletRequest, httpSession, context, systemSession, repository, userSession);

        final Object result = invoker.invoke(exchange, "test");

        verify(servletRequest, httpSession, context, systemSession, repository, userSession);

        assertThat("a map is returned", result instanceof Map);
        final Map<String, Object> map = (Map<String, Object>)result;
        assertThat(map.get("exchange"), equalTo(exchange));
        assertThat(map.get("requestParams"), equalTo("test"));
    }

    @Test
    public void returnForbiddenIfLoginFails() throws Exception {
        final SimpleCredentials credentials = new SimpleCredentials("tester", new char[]{});
        final Exchange exchange = prepareExchange();
        final HttpSession httpSession = createMock(HttpSession.class);
        final CmsSessionContext context = createMock(CmsSessionContext.class);
        final Repository repository = createMock(Repository.class);

        expect(servletRequest.getSession(false)).andReturn(httpSession);
        expect(httpSession.getAttribute(CmsSessionContext.SESSION_KEY)).andReturn(context);
        expect(context.getRepositoryCredentials()).andReturn(credentials);
        expect(systemSession.getRepository()).andReturn(repository);
        expect(repository.login(credentials)).andThrow(new RepositoryException());

        replay(servletRequest, httpSession, context, systemSession, repository);

        final Object result = invoker.invoke(exchange, "test");

        verify(servletRequest, httpSession, context, systemSession, repository);
        assertForbidden(result);
    }

    @Test
    public void logoutInSpiteOfUnexpectedException() throws Exception {
        final RuntimeException testException = new RuntimeException();
        final ManagedUserSessionInvoker exceptionThrowingInvoker = new ManagedUserSessionInvoker(systemSession) {
            @Override
            protected Object invokeSuper(Exchange exchange, Object requestParams) {
                throw testException;
            }
        };

        final SimpleCredentials credentials = new SimpleCredentials("tester", new char[]{});
        final Locale locale = new Locale("en");
        final Exchange exchange = prepareExchange();
        final HttpSession httpSession = createMock(HttpSession.class);
        final CmsSessionContext context = createMock(CmsSessionContext.class);
        final Repository repository = createMock(Repository.class);
        final Session userSession = createMock(Session.class);

        expect(servletRequest.getSession(false)).andReturn(httpSession);
        expect(httpSession.getAttribute(CmsSessionContext.SESSION_KEY)).andReturn(context);
        expect(context.getRepositoryCredentials()).andReturn(credentials);
        expect(context.getLocale()).andReturn(locale);
        expect(systemSession.getRepository()).andReturn(repository);
        expect(repository.login(credentials)).andReturn(userSession);

        servletRequest.setAttribute(ATTRIBUTE_SESSION, userSession);
        expectLastCall();
        servletRequest.setAttribute(ATTRIBUTE_LOCALE, locale);
        expectLastCall();
        userSession.logout();
        expectLastCall();

        replay(servletRequest, httpSession, context, systemSession, repository, userSession);

        try {
            exceptionThrowingInvoker.invoke(exchange, "test");
            fail("We mustn't get here");
        } catch (Exception e) {
            verify(servletRequest, httpSession, context, systemSession, repository, userSession);
            assertThat(e, equalTo(testException));
        }
    }

    @Test
    public void fallbackToEnglishLocale() {
        final CmsSessionContext context = createMock(CmsSessionContext.class);

        expect(context.getLocale()).andReturn(null);
        replay(context);

        final Locale locale = invoker.getLocale(context);

        assertThat("locale is not null", locale != null);
        assertThat("default locale is English", locale.getLanguage().equals("en"));
    }

    private Exchange prepareExchange() {
        final Message message = createMock(Message.class);
        final Exchange exchange = createMock(Exchange.class);

        expect(exchange.getInMessage()).andReturn(message);
        expect(message.get(AbstractHTTPDestination.HTTP_REQUEST)).andReturn(servletRequest);
        replay(message, exchange);

        return exchange;
    }

    private void assertForbidden(final Object result) {
        assertTrue(result instanceof MessageContentsList);
        final MessageContentsList mcl = (MessageContentsList) result;
        assertTrue(mcl.get(0) instanceof Response);
        final Response response = (Response) mcl.get(0);
        assertThat(response.getStatus(), equalTo(403));
    }
}
