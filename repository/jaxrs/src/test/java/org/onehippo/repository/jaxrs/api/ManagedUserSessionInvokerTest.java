/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.jaxrs.api;

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
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.utilities.servlet.HttpSessionBoundJcrSessionHolder;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.jaxrs.api.ManagedUserSessionInvoker.JCR_SESSION_HOLDER_ATTR_NAME;

public class ManagedUserSessionInvokerTest {
    private static final String ATTRIBUTE_SESSION = ManagedUserSessionInvoker.class.getName() + ".UserSession";
    private static final String ATTRIBUTE_LOCALE = ManagedUserSessionInvoker.class.getName() + ".Locale";

    private Session systemSession = EasyMock.createMock(Session.class);
    private ManagedUserSessionInvoker invoker = new ManagedUserSessionInvoker(systemSession) {
        @Override
        protected Object invokeSuper(Exchange exchange, Object requestParams) {
            final Map<String, Object> map = new HashMap<>();
            map.put("exchange", exchange);
            map.put("requestParams", requestParams);
            return map;
        }
    };
    private HttpServletRequest servletRequest = EasyMock.createMock(HttpServletRequest.class);

    @Test
    public void readTheUserSessionFromTheHttpServletRequest() {
        final Session userSession = EasyMock.createMock(Session.class);
        EasyMock.expect(servletRequest.getAttribute(ATTRIBUTE_SESSION)).andReturn(userSession);
        EasyMock.replay(servletRequest);

        final Session retrievedSession = invoker.getJcrSession(servletRequest);

        EasyMock.verify(servletRequest);
        assertEquals(retrievedSession, userSession);
    }

    @Test
    public void returnNullIfTheresNoUserSession() {
        EasyMock.expect(servletRequest.getAttribute(ATTRIBUTE_SESSION)).andReturn(null);
        EasyMock.replay(servletRequest);

        final Session retrievedSession = invoker.getJcrSession(servletRequest);

        EasyMock.verify(servletRequest);
        assertNull(retrievedSession);
    }

    @Test
    public void returnForbiddenIfHttpSessionMissing() {
        final Exchange exchange = prepareExchange();

        EasyMock.expect(servletRequest.getSession(false)).andReturn(null);
        EasyMock.replay(servletRequest);

        final Object result = invoker.invoke(exchange, "test");

        EasyMock.verify(servletRequest);
        assertForbidden(result);
    }

    @Test
    public void returnForbiddenIfNoCmsSessionContext() {
        final Exchange exchange = prepareExchange();
        final HttpSession httpSession = EasyMock.createMock(HttpSession.class);
        final CmsSessionContext context = EasyMock.createMock(CmsSessionContext.class);

        EasyMock.expect(servletRequest.getSession(false)).andReturn(httpSession);
        EasyMock.expect(httpSession.getAttribute(CmsSessionContext.SESSION_KEY)).andReturn(context);

        EasyMock.replay(servletRequest);

        final Object result = invoker.invoke(exchange, "test");

        EasyMock.verify(servletRequest);
        assertForbidden(result);
    }

    @Test
    public void invokeJaxrs() throws Exception {
        final SimpleCredentials credentials = new SimpleCredentials("tester", new char[]{});
        final Locale locale = new Locale("en");
        final Exchange exchange = prepareExchange();
        final HttpSession httpSession = EasyMock.createMock(HttpSession.class);
        final CmsSessionContext context = EasyMock.createMock(CmsSessionContext.class);
        final Repository repository = EasyMock.createMock(Repository.class);
        final Session userSession = EasyMock.createMock(Session.class);

        // only first time we expect null to be returned
        EasyMock.expect(httpSession.getAttribute(EasyMock.eq(JCR_SESSION_HOLDER_ATTR_NAME + '\uFFFF' + "tester"))).andReturn(null);
        final Capture<HttpSessionBoundJcrSessionHolder> holder = Capture.newInstance();
        httpSession.setAttribute(EasyMock.eq(JCR_SESSION_HOLDER_ATTR_NAME + '\uFFFF' + "tester"), EasyMock.capture(holder));
        EasyMock.expectLastCall();

        EasyMock.expect(servletRequest.getSession(false)).andReturn(httpSession);
        EasyMock.expect(httpSession.getAttribute(CmsSessionContext.SESSION_KEY)).andReturn(context);
        EasyMock.expect(context.getRepositoryCredentials()).andReturn(credentials);
        EasyMock.expect(context.getLocale()).andReturn(locale);
        EasyMock.expect(systemSession.getRepository()).andReturn(repository);
        EasyMock.expect(repository.login(credentials)).andReturn(userSession);

        servletRequest.setAttribute(ATTRIBUTE_SESSION, userSession);
        EasyMock.expectLastCall();
        servletRequest.setAttribute(ATTRIBUTE_LOCALE, locale);
        EasyMock.expectLastCall();

        EasyMock.expect(userSession.hasPendingChanges()).andReturn(false);
        EasyMock.expect(userSession.isLive()).andReturn(true);

        EasyMock.expect(servletRequest.getHeader("X-Forwarded-Host")).andReturn("locahost");
        servletRequest.setAttribute(ManagedUserSessionInvoker.ATTRIBUTE_FARTHEST_REQUEST_HOST, "locahost");
        EasyMock.expectLastCall();

        EasyMock.expect(servletRequest.getAttribute(EasyMock.eq(ManagedUserSessionInvoker.class.getName() + ".SystemSession"))).andReturn(null).anyTimes();

        EasyMock.replay(servletRequest, httpSession, context, systemSession, repository, userSession);

        final Object result = invoker.invoke(exchange, "test");

        EasyMock.verify(servletRequest, httpSession, context, systemSession, repository, userSession);

        // after first invoke. the jcr session is expected to be stored in the http session attribute
        final HttpSessionBoundJcrSessionHolder value = holder.getValue();
        assertTrue("jcr session is expected to be stored in http session attr", userSession == value.getSession());

        assertThat("a map is returned", result instanceof Map);
        final Map<String, Object> map = (Map<String, Object>)result;
        assertEquals(map.get("exchange"), exchange);
        assertEquals(map.get("requestParams"), "test");

    }

    @Test
    public void returnForbiddenIfLoginFails() throws Exception {
        final SimpleCredentials credentials = new SimpleCredentials("tester", new char[]{});
        final Exchange exchange = prepareExchange();
        final HttpSession httpSession = EasyMock.createMock(HttpSession.class);
        final CmsSessionContext context = EasyMock.createMock(CmsSessionContext.class);
        final Repository repository = EasyMock.createMock(Repository.class);

        EasyMock.expect(httpSession.getAttribute(EasyMock.eq(JCR_SESSION_HOLDER_ATTR_NAME + '\uFFFF' + "tester"))).andReturn(null);

        EasyMock.expect(servletRequest.getSession(false)).andReturn(httpSession);
        EasyMock.expect(httpSession.getAttribute(CmsSessionContext.SESSION_KEY)).andReturn(context);
        EasyMock.expect(context.getRepositoryCredentials()).andReturn(credentials);
        EasyMock.expect(systemSession.getRepository()).andReturn(repository);
        EasyMock.expect(repository.login(credentials)).andThrow(new RepositoryException());

        EasyMock.replay(servletRequest, httpSession, context, systemSession, repository);

        Log4jInterceptor.onWarn().deny(ManagedUserSessionInvoker.class).run( () -> {
            final Object result = invoker.invoke(exchange, "test");
            EasyMock.verify(servletRequest, httpSession, context, systemSession, repository);
            assertForbidden(result);
        });
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
        final HttpSession httpSession = EasyMock.createMock(HttpSession.class);
        final CmsSessionContext context = EasyMock.createMock(CmsSessionContext.class);
        final Repository repository = EasyMock.createMock(Repository.class);
        final Session userSession = EasyMock.createMock(Session.class);

        // only first time we expect null to be returned
        EasyMock.expect(httpSession.getAttribute(EasyMock.eq(JCR_SESSION_HOLDER_ATTR_NAME + '\uFFFF' + "tester"))).andReturn(null);

        final Capture<HttpSessionBoundJcrSessionHolder> holder = Capture.newInstance();
        httpSession.setAttribute(EasyMock.eq(JCR_SESSION_HOLDER_ATTR_NAME + '\uFFFF' + "tester"), EasyMock.capture(holder));
        EasyMock.expectLastCall();

        EasyMock.expect(servletRequest.getSession(false)).andReturn(httpSession).anyTimes();
        EasyMock.expect(httpSession.getAttribute(CmsSessionContext.SESSION_KEY)).andReturn(context);
        EasyMock.expect(context.getRepositoryCredentials()).andReturn(credentials);
        EasyMock.expect(context.getLocale()).andReturn(locale);
        EasyMock.expect(systemSession.getRepository()).andReturn(repository);
        EasyMock.expect(repository.login(credentials)).andReturn(userSession);

        servletRequest.setAttribute(ATTRIBUTE_SESSION, userSession);
        EasyMock.expectLastCall();
        servletRequest.setAttribute(ATTRIBUTE_LOCALE, locale);
        EasyMock.expectLastCall();
        EasyMock.expect(userSession.hasPendingChanges()).andReturn(false);
        EasyMock.expect(userSession.isLive()).andReturn(true);
        EasyMock.expect(servletRequest.getHeader("X-Forwarded-Host")).andReturn("locahost");
        servletRequest.setAttribute(ManagedUserSessionInvoker.ATTRIBUTE_FARTHEST_REQUEST_HOST, "locahost");
        EasyMock.expectLastCall();

        EasyMock.expect(servletRequest.getAttribute(EasyMock.eq(ManagedUserSessionInvoker.class.getName() + ".SystemSession"))).andReturn(null).anyTimes();

        EasyMock.replay(servletRequest, httpSession, context, systemSession, repository, userSession);

        try {
            exceptionThrowingInvoker.invoke(exchange, "test");
            fail("We mustn't get here");
        } catch (Exception e) {
            EasyMock.verify(servletRequest, httpSession, context, systemSession, repository, userSession);
            assertEquals(e, testException);
        }
    }

    @Test
    public void fallbackToEnglishLocale() {
        final CmsSessionContext context = EasyMock.createMock(CmsSessionContext.class);

        EasyMock.expect(context.getLocale()).andReturn(null);
        EasyMock.replay(context);

        final Locale locale = invoker.getLocale(context);

        assertThat("locale is not null", locale != null);
        assertThat("default locale is English", locale.getLanguage().equals("en"));
    }

    private Exchange prepareExchange() {
        final Message message = EasyMock.createMock(Message.class);
        final Exchange exchange = EasyMock.createMock(Exchange.class);

        EasyMock.expect(exchange.getInMessage()).andReturn(message);
        EasyMock.expect(message.get(AbstractHTTPDestination.HTTP_REQUEST)).andReturn(servletRequest);
        EasyMock.replay(message, exchange);

        return exchange;
    }

    private void assertForbidden(final Object result) {
        assertTrue(result instanceof MessageContentsList);
        final MessageContentsList mcl = (MessageContentsList) result;
        assertTrue(mcl.get(0) instanceof Response);
        final Response response = (Response) mcl.get(0);
        assertEquals(response.getStatus(), 403);
    }
}
