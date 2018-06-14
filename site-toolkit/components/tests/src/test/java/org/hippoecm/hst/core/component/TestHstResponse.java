/*
 *  Copyright 2011-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.component;

import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.mock.core.container.MockHstComponentWindow;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * TestHstResponse
 * 
 * @version $Id$
 */
public class TestHstResponse {
    
    private MockHttpServletResponse servletResponse;
    private MockHttpServletRequest servletRequest;
    private HstServletResponseState rootResponseState;
    private HstResponse rootHstResponse;
    private HstResponseState leftHstResponseState;
    private HstResponse leftHstResponse;
    private HstResponseState rightHstResponseState;
    private HstResponse rightHstResponse;
    private MockHstRequestContext requestContext;
    private MockHstComponentWindow leftComponentWindow;
    
    @Before
    public void setUp() throws Exception {
        servletRequest = new MockHttpServletRequest();
        servletResponse = new MockHttpServletResponse();
        
        HstContainerURL baseURL = EasyMock.createNiceMock(HstContainerURL.class);
        EasyMock.replay(baseURL);
        
        HstContainerURLProvider containerURLProvider = EasyMock.createNiceMock(HstContainerURLProvider.class);
        EasyMock.expect(containerURLProvider.getParameterNameComponentSeparator()).andReturn(":").anyTimes();
        EasyMock.replay(containerURLProvider);
        
        HstURLFactory urlFactory = EasyMock.createNiceMock(HstURLFactory.class);
        EasyMock.expect(urlFactory.getContainerURLProvider()).andReturn(containerURLProvider).anyTimes();
        EasyMock.expect(urlFactory.isReferenceNamespaceIgnored()).andReturn(true).anyTimes();
        EasyMock.replay(urlFactory);

        requestContext = new MockHstRequestContext();
        requestContext.setBaseURL(baseURL);
        requestContext.setURLFactory(urlFactory);
        
        servletRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);

        MockHstComponentWindow rootComponentWindow = new MockHstComponentWindow();
        rootComponentWindow.bindResponseState(servletRequest, servletResponse);
        rootComponentWindow.setName("root");
        rootResponseState = (HstServletResponseState)rootComponentWindow.getResponseState();
        rootHstResponse = new HstResponseImpl(servletRequest, servletResponse, requestContext, rootComponentWindow, null);

        leftComponentWindow = new MockHstComponentWindow();
        leftComponentWindow.bindResponseState(servletRequest, rootHstResponse);
        leftComponentWindow.setName("left");
        leftHstResponseState = leftComponentWindow.getResponseState();
        leftHstResponse = new HstResponseImpl(servletRequest, rootHstResponse, requestContext, leftComponentWindow, rootHstResponse);

        MockHstComponentWindow rightComponentWindow = new MockHstComponentWindow();
        rightComponentWindow.bindResponseState(servletRequest, rootHstResponse);
        rightComponentWindow.setName("right");
        rightHstResponseState = rightComponentWindow.getResponseState();
        rightHstResponse = new HstResponseImpl(servletRequest, rootHstResponse, requestContext, rightComponentWindow, rootHstResponse);

        rootComponentWindow.getChildWindowMap().put(leftComponentWindow.getName(), leftComponentWindow);
        rootComponentWindow.getChildWindowMap().put(rightComponentWindow.getName(), rightComponentWindow);

    }
    
    @Test
    public void testAddHttpHeaders() throws Exception {
        leftHstResponse.addHeader("added-one", "left-one");
        assertTrue(leftHstResponse.containsHeader("added-one"));
        leftHstResponse.addHeader("added-two", "left-two");
        assertTrue(leftHstResponse.containsHeader("added-two"));
        
        rightHstResponse.addHeader("added-one", "right-one");
        assertTrue(rightHstResponse.containsHeader("added-one"));
        rightHstResponse.addHeader("added-two", "right-two");
        assertTrue(rightHstResponse.containsHeader("added-two"));
        
        rootHstResponse.addHeader("added-one", "root-one");
        assertTrue(rootHstResponse.containsHeader("added-one"));
        rootHstResponse.addHeader("added-two", "root-two");
        assertTrue(rootHstResponse.containsHeader("added-two"));
        
        rightHstResponseState.flush();
        leftHstResponseState.flush();
        rootResponseState.flush();
        
        assertTrue(servletResponse.containsHeader("added-one"));
        assertTrue(servletResponse.containsHeader("added-two"));
        
        assertEquals("left-one;right-one;root-one", StringUtils.join(new TreeSet<Object>(servletResponse.getHeaders("added-one")), ";"));
        assertEquals("left-two;right-two;root-two", StringUtils.join(new TreeSet<Object>(servletResponse.getHeaders("added-two")), ";"));
    }
    
    @Test
    public void testSetHttpHeaders() throws Exception {
        leftHstResponse.setHeader("set-one", "left-one");
        assertTrue(leftHstResponse.containsHeader("set-one"));
        
        rightHstResponse.setHeader("set-two", "right-two");
        assertTrue(rightHstResponse.containsHeader("set-two"));
        
        rootHstResponse.setHeader("set-one", "root-one");
        assertTrue(rootHstResponse.containsHeader("set-one"));
        rootHstResponse.setHeader("set-two", "root-two");
        assertTrue(rootHstResponse.containsHeader("set-two"));
        
        // when flushes, it adds headers into the parent response, rootHstResponse.
        // therefore, this replaces the rootResponseState's header
        rightHstResponseState.flush();
        // when flushes, it adds headers into the parent response, rootHstResponse.
        // therefore, this replaces the rootResponseState's header
        leftHstResponseState.flush();
        // when flushes, it adds headers into the servlet response, servletResponse.
        // in this case, the headers from the leftHstResponseState replaced the header.
        rootResponseState.flush();
        
        assertTrue(servletResponse.containsHeader("set-one"));
        assertTrue(servletResponse.containsHeader("set-two"));
        
        assertEquals("left-one", StringUtils.join(new TreeSet<Object>(servletResponse.getHeaders("set-one")), ";"));
        assertEquals("right-two", StringUtils.join(new TreeSet<Object>(servletResponse.getHeaders("set-two")), ";"));
    }

    @Test
    public void flush_flushes_unflushed_child_component_headers_as_well() throws Exception {
        leftHstResponse.addHeader("added-left-one", "left-one");
        assertTrue(leftHstResponse.containsHeader("added-left-one"));
        leftHstResponse.addHeader("added-left-two", "left-two");
        assertTrue(leftHstResponse.containsHeader("added-left-two"));

        rightHstResponse.addHeader("added-right-one", "right-one");
        assertTrue(rightHstResponse.containsHeader("added-right-one"));
        rightHstResponse.addHeader("added-right-two", "right-two");
        assertTrue(rightHstResponse.containsHeader("added-right-two"));

        rootResponseState.flush();

        assertTrue(servletResponse.containsHeader("added-left-one"));
        assertTrue(servletResponse.containsHeader("added-left-two"));
        assertTrue(servletResponse.containsHeader("added-right-one"));
        assertTrue(servletResponse.containsHeader("added-right-two"));

    }

    @Test
    public void flush_flushes_unflushed_grant_child_component_headers() throws Exception {
        MockHstComponentWindow subLeftComponentWindow = new MockHstComponentWindow();
        subLeftComponentWindow.bindResponseState(servletRequest, servletResponse);
        subLeftComponentWindow.setName("sub-left");
        HstResponse subLeftHstResponse = new HstResponseImpl(servletRequest, leftHstResponse, requestContext, subLeftComponentWindow, leftHstResponse);

        leftComponentWindow.getChildWindowMap().put(subLeftComponentWindow.getName(), subLeftComponentWindow);

        subLeftHstResponse.addHeader("added-sub-left-one", "left-one");
        assertTrue(subLeftHstResponse.containsHeader("added-sub-left-one"));
        subLeftHstResponse.addHeader("added-sub-left-two", "left-two");
        assertTrue(subLeftHstResponse.containsHeader("added-sub-left-two"));

        rootResponseState.flush();

        assertTrue(servletResponse.containsHeader("added-sub-left-one"));
        assertTrue(servletResponse.containsHeader("added-sub-left-two"));
    }

    @Test
    public void testEpilogue() throws Exception {
        rootHstResponse.addEpilogue(rootHstResponse.createComment("testing 1"));
        rootHstResponse.addEpilogue(rootHstResponse.createComment("testing 2"));

        assertEquals("testing 1;testing 2", rootResponseState.epilogueComments
                .stream()
                .map(Node::getTextContent)
                .collect(Collectors.joining(";")));
    }
    
}
