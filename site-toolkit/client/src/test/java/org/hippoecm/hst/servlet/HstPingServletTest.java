/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.MutableHstManager;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

public class HstPingServletTest {

    protected final static Logger log = LoggerFactory.getLogger(HstPingServletTest.class);
    private HstPingServlet pingServlet;

    @Before
    public void setUp() throws Exception {
        pingServlet = new HstPingServlet();
        pingServlet.init(new MockServletConfig());
    }

    @After
    public void tearDown() throws Exception {
        pingServlet.destroy();
    }

    @Test
    public void okWhenServicesAreAvailableAndThereIsARepository() throws IOException, ServletException, RepositoryException {
        ComponentManager componentManager = createMock(ComponentManager.class);
        Repository mockRepo = createMock(Repository.class);
        Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        expect(componentManager.getComponent(Repository.class.getName())).andReturn(mockRepo);
        expect(componentManager.getComponent(Credentials.class.getName() + ".default"))
                .andReturn(creds);
        Session mockSession = createMock(Session.class);
        expect(mockRepo.login(creds)).andReturn(mockSession);

        Node rootNode = createMock(Node.class);
        expect(mockSession.getRootNode()).andReturn(rootNode);
        expect(rootNode.getNode("content/documents")).andReturn(null);

        mockSession.logout();
        expectLastCall();

        HstServices.setComponentManager(componentManager);
        ((MutableHstManager)componentManager.getComponent(HstManager.class.getName()))
                .setContextPath("/site");
        assertTrue(HstServices.isAvailable());

        replay(componentManager, mockRepo, mockSession, rootNode);
        try {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            pingServlet.doGet(request, response);
            String content = response.getContentAsString();
            assertTrue(content.contains("OK"));
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());

            verify(componentManager, mockRepo, mockSession, rootNode);
        } finally {
            HstServices.setComponentManager(null);
            ((MutableHstManager)componentManager.getComponent(HstManager.class.getName()))
                    .setContextPath(null);
        }
    }

    @Test
    public void sessionIsNotClosedWhenObtainedFromRequest() throws RepositoryException, IOException, ServletException {
        ComponentManager componentManager = createMock(ComponentManager.class);
        Session mockSession = createMock(Session.class);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();

        HstRequest hstRequest = createMock(HstRequest.class);
        request.setAttribute(ContainerConstants.HST_REQUEST, hstRequest);

        HstRequestContext hrc = createMock(HstRequestContext.class);
        expect(hstRequest.getRequestContext()).andReturn(hrc).times(2);
        expect(hrc.getSession()).andReturn(mockSession).times(2);
        expect(mockSession.isLive()).andReturn(true).times(2);

        Node rootNode = createMock(Node.class);
        expect(mockSession.getRootNode()).andReturn(rootNode);
        expect(rootNode.getNode("content/documents")).andReturn(null);

        HstServices.setComponentManager(componentManager);
        ((MutableHstManager)componentManager.getComponent(HstManager.class.getName()))
                .setContextPath("/site");
        assertTrue(HstServices.isAvailable());

        replay(componentManager, hstRequest, hrc, mockSession, rootNode);
        try {

            pingServlet.doGet(request, response);
            String content = response.getContentAsString();
            assertTrue(content.contains("OK"));
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());

            verify(componentManager, hstRequest, hrc, mockSession, rootNode);
        } finally {
            HstServices.setComponentManager(null);
            ((MutableHstManager)componentManager.getComponent(HstManager.class.getName()))
                    .setContextPath(null);
        }
    }

    @Test
    public void errorWhenServicesAreNotInitialized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        pingServlet.doGet(request, response);
        String content = response.getContentAsString();
        assertTrue(content.contains("not available"));
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }

    @Test
    public void errorWhenServicesAreAvailableButThereIsNoRepository() throws IOException, ServletException, RepositoryException {
        ComponentManager componentManager = createMock(ComponentManager.class);
        Repository mockRepo = createMock(Repository.class);
        expect(componentManager.getComponent(Repository.class.getName())).andReturn(null);

        HstServices.setComponentManager(componentManager);
        ((MutableHstManager)componentManager.getComponent(HstManager.class.getName()))
                .setContextPath("/site");
        assertTrue(HstServices.isAvailable());

        replay(componentManager, mockRepo);
        try {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            pingServlet.doGet(request, response);
            String content = response.getContentAsString();
            assertTrue(content.contains("not available"));
            assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());

            verify(componentManager, mockRepo);
        } finally {
            HstServices.setComponentManager(null);
            ((MutableHstManager)componentManager.getComponent(HstManager.class.getName()))
                    .setContextPath(null);
        }
    }


}
