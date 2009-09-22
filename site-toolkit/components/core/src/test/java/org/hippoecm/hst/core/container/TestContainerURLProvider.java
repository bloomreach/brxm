/*
 *  Copyright 2008 Hippo.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.component.HstURLFactoryImpl;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.mock.MockHstComponentWindow;
import org.hippoecm.hst.site.request.HstRequestContextImpl;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestContainerURLProvider extends AbstractSpringTestCase {

    protected HstURLFactory urlFactory;
    protected HstContainerURLProvider urlProvider;
    protected HstRequestContext requestContext;
    
    protected MockHstComponentWindow rootWindow;
    protected MockHstComponentWindow leftChildWindow;
    protected MockHstComponentWindow rightChildWindow;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.urlFactory = getComponent(HstURLFactory.class.getName());
        this.requestContext = new HstRequestContextImpl(null);
        this.urlProvider = this.urlFactory.getContainerURLProvider(requestContext);
        ((HstRequestContextImpl) this.requestContext).setURLFactory(urlFactory);
        
        rootWindow = new MockHstComponentWindow();
        rootWindow.setReferenceName("root");
        rootWindow.setReferenceNamespace("");
        
        leftChildWindow = new MockHstComponentWindow();
        leftChildWindow.setReferenceName("left");
        leftChildWindow.setReferenceNamespace("l1");
        leftChildWindow.setParentWindow(rootWindow);
        
        rightChildWindow = new MockHstComponentWindow();
        rightChildWindow.setReferenceName("right");
        rightChildWindow.setReferenceNamespace("r1");
        rightChildWindow.setParentWindow(rootWindow);
    }
    
    @Test
    public void testBasicCotnainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");
        
        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext);

        assertNull("action window reference namespace is not null.", containerURL.getActionWindowReferenceNamespace());
        assertNull("resource window reference namespace is not null.", containerURL.getResourceWindowReferenceNamespace());
        assertEquals("The path info is wrong: " + containerURL.getPathInfo(), "/news/2008/08", containerURL.getPathInfo());
    }
    
    @Test
    public void testRenderContainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext);
        ((HstRequestContextImpl) requestContext).setBaseURL(containerURL);

        HstURL url = this.urlFactory.createURL(HstURL.RENDER_TYPE, "r1", containerURL, requestContext);
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");
        
        assertTrue("The url is wrong.", url.toString().contains("r1:param1=value1"));
        assertTrue("The url is wrong.", url.toString().contains("r1:param2=value2"));
        
        ((MockHttpServletRequest) request).setParameter("r1:param1", "value1");
        ((MockHttpServletRequest) request).setParameter("r1:param2", "value2");
        
        HstRequest hstRequest = new HstRequestImpl(request, requestContext, rightChildWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequest.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequest.getParameter("param2"));
        
        url = this.urlFactory.createURL(HstURL.RENDER_TYPE, "", containerURL, requestContext);
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");
        
        assertFalse("The url is wrong.", url.toString().contains(":param1=value1"));
        assertFalse("The url is wrong.", url.toString().contains(":param2=value2"));
        assertTrue("The url is wrong.", url.toString().contains("param1=value1"));
        assertTrue("The url is wrong.", url.toString().contains("param2=value2"));
        
        ((MockHttpServletRequest) request).removeAllParameters();
        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");

        hstRequest = new HstRequestImpl(request, requestContext, rootWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequest.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequest.getParameter("param2"));        
    }
    
    @Test
    public void testNamespacelessRenderContainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext);
        ((HstRequestContextImpl) requestContext).setBaseURL(containerURL);

        ((HstURLFactoryImpl) this.urlFactory).setReferenceNamespaceIgnored(true);
        
        HstURL url = this.urlFactory.createURL(HstURL.RENDER_TYPE, "r1", containerURL, requestContext);
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");
        
        assertFalse("The url is wrong.", url.toString().contains(":param1=value1"));
        assertFalse("The url is wrong.", url.toString().contains(":param2=value2"));
        assertTrue("The url is wrong.", url.toString().contains("param1=value1"));
        assertTrue("The url is wrong.", url.toString().contains("param2=value2"));
        
        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");
        
        HstRequest hstRequest = new HstRequestImpl(request, requestContext, rightChildWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequest.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequest.getParameter("param2"));
        
        url = this.urlFactory.createURL(HstURL.RENDER_TYPE, "", containerURL, requestContext);
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");
        
        assertFalse("The url is wrong.", url.toString().contains(":param1=value1"));
        assertFalse("The url is wrong.", url.toString().contains(":param2=value2"));
        assertTrue("The url is wrong.", url.toString().contains("param1=value1"));
        assertTrue("The url is wrong.", url.toString().contains("param2=value2"));
        
        ((MockHttpServletRequest) request).removeAllParameters();
        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");

        hstRequest = new HstRequestImpl(request, requestContext, rootWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequest.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequest.getParameter("param2"));        
    }
    
    @Test
    public void testActionContainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");
        
        HstContainerURL actionURL = this.urlProvider.parseURL(request, response, requestContext);
        actionURL.setActionWindowReferenceNamespace("b");
        actionURL.setActionParameter("ap1", "one");
        actionURL.setActionParameter("ap2", "two");
        String actionURLPathInfo = this.urlProvider.toURLString(actionURL, requestContext);
        actionURLPathInfo = actionURLPathInfo.substring("/site/content".length());
        ((MockHttpServletRequest) request).setPathInfo(actionURLPathInfo);
        
        assertNotNull("action window reference namespace is null.", actionURL.getActionWindowReferenceNamespace());
        assertNull("resource window reference namespace is not null.", actionURL.getResourceWindowReferenceNamespace());

        Map<String, String []> actionParams = (Map<String, String []>) actionURL.getActionParameterMap();
        assertNotNull("action param map is null.", actionParams);
        assertFalse("action param map is empty.", actionParams.isEmpty());
        assertEquals("the first action parameter is not 'ap1'.", "one", actionParams.get("ap1")[0]);
        assertEquals("the second action parameter is not 'ap2'.", "two", actionParams.get("ap2")[0]);
    }
    
    @Test
    public void testResourceContainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");
        
        HstContainerURL resourceURL = this.urlProvider.parseURL(request, response, requestContext);
        resourceURL.setResourceWindowReferenceNamespace("b");
        resourceURL.setResourceId("myresource001");
        String resourceURLPathInfo = this.urlProvider.toURLString(resourceURL, requestContext);
        resourceURLPathInfo = resourceURLPathInfo.substring("/site/content".length());
        ((MockHttpServletRequest) request).setPathInfo(resourceURLPathInfo);
        
        assertNull("action window reference namespace is not null.", resourceURL.getActionWindowReferenceNamespace());
        assertNotNull("resource window reference namespace is null.", resourceURL.getResourceWindowReferenceNamespace());
        assertEquals("resource id is wrong.", "myresource001", resourceURL.getResourceId());
        assertEquals("The path info is wrong.", "/news/2008/08", resourceURL.getPathInfo());
    }
    
}