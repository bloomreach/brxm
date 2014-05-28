/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container.session;

import static org.junit.Assert.assertSame;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.MutableHstManager;
import org.hippoecm.hst.container.event.HttpSessionCreatedEvent;
import org.hippoecm.hst.container.event.HttpSessionDestroyedEvent;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.session.HttpSessionEventPublisher;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

/**
 * TestHttpSessionEventPublisher
 */
public class TestHttpSessionEventPublisher {

    @After
    public void tearDown() throws Exception {
        HstServices.setComponentManager(null);
    }

    @Test
    public void testHttpSessionCreated() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        final MockHttpSession httpSession = new MockHttpSession(servletContext);
        HttpSessionEvent httpSessionEvent = new HttpSessionEvent(httpSession);

        ComponentManager componentManager = EasyMock.createMock(ComponentManager.class);
        HstServices.setComponentManager(componentManager);
        componentManager.publishEvent(EasyMock.anyObject(HttpSessionCreatedEvent.class));

        EasyMock.expectLastCall().andAnswer(new IAnswer<HttpSessionCreatedEvent>() {
            public HttpSessionCreatedEvent answer() throws Throwable {
                HttpSessionCreatedEvent event = (HttpSessionCreatedEvent) EasyMock.getCurrentArguments()[0];
                assertSame(httpSession, event.getSession());
                return event;
            }
        }).once();

        EasyMock.replay(componentManager);
 
        HttpSessionListener listener = new HttpSessionEventPublisher();
        listener.sessionCreated(httpSessionEvent);

        EasyMock.verify(componentManager);
    }

    @Test
    public void testHttpSessionDestroyed() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        final MockHttpSession httpSession = new MockHttpSession(servletContext);
        HttpSessionEvent httpSessionEvent = new HttpSessionEvent(httpSession);

        ComponentManager componentManager = EasyMock.createMock(ComponentManager.class);
        HstServices.setComponentManager(componentManager);
        componentManager.publishEvent(EasyMock.anyObject(HttpSessionDestroyedEvent.class));

        EasyMock.expectLastCall().andAnswer(new IAnswer<HttpSessionDestroyedEvent>() {
            public HttpSessionDestroyedEvent answer() throws Throwable {
                HttpSessionDestroyedEvent event = (HttpSessionDestroyedEvent) EasyMock.getCurrentArguments()[0];
                assertSame(httpSession, event.getSession());
                return event;
            }
        }).once();

        EasyMock.replay(componentManager);

        HttpSessionListener listener = new HttpSessionEventPublisher();
        listener.sessionDestroyed(httpSessionEvent);

        EasyMock.verify(componentManager);

    }
}
