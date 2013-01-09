/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.component.HstURLFactoryImpl;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.mock.core.container.MockHstComponentWindow;
import org.hippoecm.hst.site.request.HstRequestContextImpl;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;


public class TestContainerURLProvider extends AbstractSpringTestCase {

    protected HstURLFactory urlFactory;
    protected HstContainerURLProvider urlProvider;
    protected HstMutableRequestContext requestContext;
    
    protected MockHstComponentWindow rootWindow;
    protected MockHstComponentWindow leftChildWindow;
    protected MockHstComponentWindow rightChildWindow;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.urlFactory = getComponent(HstURLFactory.class.getName());
        this.requestContext = new HstRequestContextImpl(null);
        this.urlProvider = this.urlFactory.getContainerURLProvider();
        ((HstMutableRequestContext) this.requestContext).setURLFactory(urlFactory);
        
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
    public void testBasicContainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());
        
        // need to set the resolved mount on the requestContext
        setResolvedMount(requestContext);
        
        // request.getServletPath() = ""
        ((MockHttpServletRequest)request).setRequestURI(request.getContextPath() + request.getServletPath() + request.getPathInfo());
        
        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");
        
        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());

        assertNull("action window reference namespace is not null.", containerURL.getActionWindowReferenceNamespace());
        assertNull("resource window reference namespace is not null.", containerURL.getResourceWindowReferenceNamespace());
        assertEquals("The path info is wrong: " + containerURL.getPathInfo(), "/news/2008/08", containerURL.getPathInfo());
    }

    
    
    @Test
    public void testRenderContainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        setResolvedMount(requestContext);
        
        // request.getServletPath() = ""
        ((MockHttpServletRequest)request).setRequestURI(request.getContextPath() + request.getServletPath() + request.getPathInfo());
        
        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        requestContext.setBaseURL(containerURL);

        HstURL url = this.urlFactory.createURL(HstURL.RENDER_TYPE, "r1", containerURL, requestContext);
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");
        
        assertTrue("The url is wrong.", url.toString().contains("r1:param1=value1"));
        assertTrue("The url is wrong.", url.toString().contains("r1:param2=value2"));
        
        ((MockHttpServletRequest) request).setParameter("r1:param1", "value1");
        ((MockHttpServletRequest) request).setParameter("r1:param2", "value2");
        
        HstRequest hstRequestRightChildWindow = new HstRequestImpl(request, requestContext, rightChildWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequestRightChildWindow.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequestRightChildWindow.getParameter("param2"));
        
        HstRequest hstRequestLeftChildWindow = new HstRequestImpl(request, requestContext, leftChildWindow, HstRequest.RENDER_PHASE);
  
        assertNull("The parameter value must be null for left child window", hstRequestLeftChildWindow.getParameter("param1"));
        assertNull("The parameter value must be null for left child window", hstRequestLeftChildWindow.getParameter("param2"));
      
        HstRequest hstRequestRootWindow = new HstRequestImpl(request, requestContext, rootWindow, HstRequest.RENDER_PHASE);
        assertNull("The parameter value must be null for root window", hstRequestRootWindow.getParameter("param1"));
        assertNull("The parameter value must be null for root window", hstRequestRootWindow.getParameter("param2"));
        
        
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
        
        hstRequestRootWindow = new HstRequestImpl(request, requestContext, rootWindow, HstRequest.RENDER_PHASE);
        hstRequestLeftChildWindow = new HstRequestImpl(request, requestContext, leftChildWindow, HstRequest.RENDER_PHASE);
        hstRequestRightChildWindow = new HstRequestImpl(request, requestContext, rightChildWindow, HstRequest.RENDER_PHASE);
         
        // now, every window should be able to access the parameters as the parameters do not have a namespace
        assertEquals("The parameter value is wrong: param1", "value1", hstRequestRootWindow.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequestRootWindow.getParameter("param2"));
        assertNull("The parameter value is wrong: param1",  hstRequestRightChildWindow.getParameter("param1"));
        assertNull("The parameter value is wrong: param2", hstRequestRightChildWindow.getParameter("param2"));
        assertNull("The parameter value is wrong: param1", hstRequestLeftChildWindow.getParameter("param1"));
        assertNull("The parameter value is wrong: param2", hstRequestLeftChildWindow.getParameter("param2"));       
    }
    
    @Test
    public void testRenderContainerURLParameterMerging() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        setResolvedMount(requestContext);
        
        // request.getServletPath() = ""
        ((MockHttpServletRequest)request).setRequestURI(request.getContextPath() + request.getServletPath() + request.getPathInfo());
        ((MockHttpServletRequest)request).setQueryString("param=value1_emptyns&r2:param1=value1_r2&r1:param1=value1_r1");
        // when the queryString is parsed in HstRequestUtils, also the parameters need to be set
        ((MockHttpServletRequest)request).setParameter("param", "value1_emptyns");
        ((MockHttpServletRequest)request).setParameter("r1:param1", "value1_r1");
        ((MockHttpServletRequest)request).setParameter("r2:param1", "value1_r2");
        
        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        requestContext.setBaseURL(containerURL);

        HstURL url = this.urlFactory.createURL(HstURL.RENDER_TYPE, "r1", containerURL, requestContext);
        // A url created with 'r1' namespace should get the existing r1 parameters removed!
        assertFalse("The url is wrong.", url.toString().contains("r1:param1=value1_r1"));
        // but it should still contain the r2 one
        assertTrue("The url is wrong.", url.toString().contains("r2:param1=value1_r2"));
        // and the namespaceless
        assertTrue("The url is wrong.", url.toString().contains("param=value1_emptyns"));
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");
        assertTrue("The url is wrong.", url.toString().contains("r1:param1=value1"));
        assertFalse("The url is wrong.", url.toString().contains("r2:param2=value2"));
        
        url = this.urlFactory.createURL(HstURL.RENDER_TYPE, "r2", containerURL, requestContext);
      // A url created with 'r2' namespace should get the existing r2 parameters removed!
        assertFalse("The url is wrong.", url.toString().contains("r2:param1=value1_r2"));
        // but it should still contain the r1 one
        assertTrue("The url is wrong.", url.toString().contains("r1:param1=value1_r1"));
        // and the namespaceless
        assertTrue("The url is wrong.", url.toString().contains("param=value1_emptyns"));
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");
        assertTrue("The url is wrong.", url.toString().contains("r2:param1=value1"));
        assertFalse("The url is wrong.", url.toString().contains("r1:param2=value2"));
    }
    
    @Test
    public void testNamespacelessRenderContainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        ((MockHttpServletRequest)request).setRequestURI(request.getContextPath() + request.getServletPath() + request.getPathInfo());
        
        setResolvedMount(requestContext);
        
        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        requestContext.setBaseURL(containerURL);

        
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
        
        HstRequest hstRequestRightChildWindow = new HstRequestImpl(request, requestContext, rightChildWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequestRightChildWindow.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequestRightChildWindow.getParameter("param2"));
        
        // because namespaceless, every component should be able to read all request parameters 
        HstRequest hstRequestLeftChildWindow = new HstRequestImpl(request, requestContext, leftChildWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequestLeftChildWindow.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequestLeftChildWindow.getParameter("param2"));
        
        HstRequest hstRequestrootWindow = new HstRequestImpl(request, requestContext, rootWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequestrootWindow.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequestrootWindow.getParameter("param2"));
        
        
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

        hstRequestRightChildWindow = new HstRequestImpl(request, requestContext, rootWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequestRightChildWindow.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequestRightChildWindow.getParameter("param2"));    

        // because namespaceless, every component should be able to read all request parameters 
        hstRequestLeftChildWindow = new HstRequestImpl(request, requestContext, leftChildWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequestLeftChildWindow.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequestLeftChildWindow.getParameter("param2"));
        
        hstRequestrootWindow = new HstRequestImpl(request, requestContext, rootWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequestrootWindow.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequestrootWindow.getParameter("param2"));
        
    }
    
    @Test
    public void testActionContainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        ((MockHttpServletRequest)request).setRequestURI(request.getContextPath() + request.getServletPath() + request.getPathInfo());
        
        
        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");
        
        
        setResolvedMount(requestContext);
        
        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        requestContext.setBaseURL(containerURL);

        HstContainerURL actionURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
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

        ((MockHttpServletRequest)request).setRequestURI(request.getContextPath() + request.getServletPath() + request.getPathInfo());
        
        
        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");
        
        setResolvedMount(requestContext);

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        requestContext.setBaseURL(containerURL);

        
        HstContainerURL resourceURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
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
    
    @Test
    public void testComponentRenderingContainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        setResolvedMount(requestContext);
        
        ((MockHttpServletRequest)request).setRequestURI(request.getContextPath() + request.getServletPath() + request.getPathInfo());
        
        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
       
        requestContext.setBaseURL(containerURL);
        
        HstURL url = this.urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE, "r1", containerURL, requestContext);
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");  
        
        assertTrue("Type is wrong",url.getType().equals(HstURL.COMPONENT_RENDERING_TYPE));
        assertTrue("The url is wrong.", url.toString().contains("r1:param1=value1"));
        assertTrue("The url is wrong.", url.toString().contains("r1:param2=value2"));
        
        HstContainerURL componentRenderingURL = urlProvider.createURL(containerURL, url);
        
        final String componentRenderingWindowReferenceNamespace = "cr";
        componentRenderingURL.setComponentRenderingWindowReferenceNamespace("cr");
        
        String componentRenderingURLString = urlProvider.toURLString(componentRenderingURL, requestContext);
        
        String decodedURLPart =  HstURL.COMPONENT_RENDERING_TYPE + 
                              HstContainerURLProviderImpl.REQUEST_INFO_SEPARATOR + 
                              componentRenderingWindowReferenceNamespace;
        String encodedURLPart = URLEncoder.encode(decodedURLPart, componentRenderingURL.getCharacterEncoding());

        assertTrue("componentRenderingURLString is wrong", componentRenderingURLString.contains(encodedURLPart));
        assertTrue("componentRenderingURLString is wrong", componentRenderingURLString.contains(HstContainerURLProviderImpl.DEFAULT_HST_URL_NAMESPACE_PREFIX+encodedURLPart));
       
        ((MockHttpServletRequest) request).setParameter("r1:param1", "value1");
        ((MockHttpServletRequest) request).setParameter("r1:param2", "value2");
        
        assertTrue("The url is wrong.", url.toString().contains("r1:param1=value1"));
        assertTrue("The url is wrong.", url.toString().contains("r1:param2=value2"));
        
        ((MockHttpServletRequest) request).setParameter("r1:param1", "value1");
        ((MockHttpServletRequest) request).setParameter("r1:param2", "value2");
        
        HstRequest hstRequestRightChildWindow = new HstRequestImpl(request, requestContext, rightChildWindow, HstRequest.RENDER_PHASE);
        assertEquals("The parameter value is wrong: param1", "value1", hstRequestRightChildWindow.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequestRightChildWindow.getParameter("param2"));
        
        HstRequest hstRequestLeftChildWindow = new HstRequestImpl(request, requestContext, leftChildWindow, HstRequest.RENDER_PHASE);
  
        assertNull("The parameter value must be null for left child window", hstRequestLeftChildWindow.getParameter("param1"));
        assertNull("The parameter value must be null for left child window", hstRequestLeftChildWindow.getParameter("param2"));
      
        HstRequest hstRequestRootWindow = new HstRequestImpl(request, requestContext, rootWindow, HstRequest.RENDER_PHASE);
        assertNull("The parameter value must be null for root window", hstRequestRootWindow.getParameter("param1"));
        assertNull("The parameter value must be null for root window", hstRequestRootWindow.getParameter("param2"));
        
        url = this.urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE, "", containerURL, requestContext);
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");
        
        assertFalse("The url is wrong.", url.toString().contains(":param1=value1"));
        assertFalse("The url is wrong.", url.toString().contains(":param2=value2"));
        assertTrue("The url is wrong.", url.toString().contains("param1=value1"));
        assertTrue("The url is wrong.", url.toString().contains("param2=value2"));
        
        ((MockHttpServletRequest) request).removeAllParameters();
        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");
        
        hstRequestRootWindow = new HstRequestImpl(request, requestContext, rootWindow, HstRequest.RENDER_PHASE);
        hstRequestLeftChildWindow = new HstRequestImpl(request, requestContext, leftChildWindow, HstRequest.RENDER_PHASE);
        hstRequestRightChildWindow = new HstRequestImpl(request, requestContext, rightChildWindow, HstRequest.RENDER_PHASE);
         
        // now, every window should be able to access the parameters as the parameters do not have a namespace
        assertEquals("The parameter value is wrong: param1", "value1", hstRequestRootWindow.getParameter("param1"));
        assertEquals("The parameter value is wrong: param2", "value2", hstRequestRootWindow.getParameter("param2"));
        assertNull("The parameter value is wrong: param1",  hstRequestRightChildWindow.getParameter("param1"));
        assertNull("The parameter value is wrong: param2", hstRequestRightChildWindow.getParameter("param2"));
        assertNull("The parameter value is wrong: param1", hstRequestLeftChildWindow.getParameter("param1"));
        assertNull("The parameter value is wrong: param2", hstRequestLeftChildWindow.getParameter("param2"));       

        }
    
    @Test
    public void testComponentRenderingContainerURLParameterMerging() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        setResolvedMount(requestContext);
        
        // request.getServletPath() = ""
        ((MockHttpServletRequest)request).setRequestURI(request.getContextPath() + request.getServletPath() + request.getPathInfo());
        ((MockHttpServletRequest)request).setQueryString("param=value1_emptyns&r2:param1=value1_r2&r1:param1=value1_r1");
        // when the queryString is parsed in HstRequestUtils, also the parameters need to be set
        ((MockHttpServletRequest)request).setParameter("param", "value1_emptyns");
        ((MockHttpServletRequest)request).setParameter("r1:param1", "value1_r1");
        ((MockHttpServletRequest)request).setParameter("r2:param1", "value1_r2");
        
        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        requestContext.setBaseURL(containerURL);

        HstURL url = this.urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE, "r1", containerURL, requestContext);
        // A url created with 'r1' namespace should get the existing r1 parameters removed!
        assertFalse("The url is wrong.", url.toString().contains("r1:param1=value1_r1"));
        // but it should still contain the r2 one
        assertTrue("The url is wrong.", url.toString().contains("r2:param1=value1_r2"));
        // and the namespaceless
        assertTrue("The url is wrong.", url.toString().contains("param=value1_emptyns"));
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");
        assertTrue("The url is wrong.", url.toString().contains("r1:param1=value1"));
        assertFalse("The url is wrong.", url.toString().contains("r2:param2=value2"));
        
        url = this.urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE, "r2", containerURL, requestContext);
      // A url created with 'r2' namespace should get the existing r2 parameters removed!
        assertFalse("The url is wrong.", url.toString().contains("r2:param1=value1_r2"));
        // but it should still contain the r1 one
        assertTrue("The url is wrong.", url.toString().contains("r1:param1=value1_r1"));
        // and the namespaceless
        assertTrue("The url is wrong.", url.toString().contains("param=value1_emptyns"));
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");
        assertTrue("The url is wrong.", url.toString().contains("r2:param1=value1"));
        assertFalse("The url is wrong.", url.toString().contains("r1:param2=value2"));
    }
}