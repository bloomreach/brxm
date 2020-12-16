/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.easymock.EasyMock;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactoryImpl;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class ContainerURLProviderIT extends AbstractContainerURLProviderIT {

    @Test
    public void testBasicContainerURL() throws UnsupportedEncodingException, ContainerException {
        MockHttpServletRequest request = mockRequest();
        HttpServletResponse response = mockResponse();

        // need to set the resolved mount on the requestContext
        setResolvedMount(requestContext);

        request.setParameter("param1", "value1");
        request.setParameter("param2", "value2");

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());

        assertNull("action window reference namespace is not null.", containerURL.getActionWindowReferenceNamespace());
        assertNull("resource window reference namespace is not null.", containerURL.getResourceWindowReferenceNamespace());
        assertEquals("The path info is wrong: " + containerURL.getPathInfo(), "/news/2008/08", containerURL.getPathInfo());
    }

    @Test
    public void testRenderContainerURL() throws UnsupportedEncodingException, ContainerException {
        MockHttpServletRequest request = mockRequest();
        HttpServletResponse response = mockResponse();

        setResolvedMount(requestContext);

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        requestContext.setBaseURL(containerURL);

        HstURL url = this.urlFactory.createURL(HstURL.RENDER_TYPE, "r1", containerURL, requestContext);
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");

        assertTrue("The url is wrong.", url.toString().contains("r1:param1=value1"));
        assertTrue("The url is wrong.", url.toString().contains("r1:param2=value2"));

        request.setParameter("r1:param1", "value1");
        request.setParameter("r1:param2", "value2");

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

        request.removeAllParameters();
        request.setParameter("param1", "value1");
        request.setParameter("param2", "value2");

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
        MockHttpServletRequest request = mockRequest();
        HttpServletResponse response = mockResponse();

        setResolvedMount(requestContext);

        request.setQueryString("param=value1_emptyns&r2:param1=value1_r2&r1:param1=value1_r1");
        // when the queryString is parsed in HstRequestUtils, also the parameters need to be set
        request.setParameter("param", "value1_emptyns");
        request.setParameter("r1:param1", "value1_r1");
        request.setParameter("r2:param1", "value1_r2");

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
        MockHttpServletRequest request = mockRequest();
        HttpServletResponse response = mockResponse();

        setResolvedMount(requestContext);

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        requestContext.setBaseURL(containerURL);

        try {
            ((HstURLFactoryImpl) this.urlFactory).setReferenceNamespaceIgnored(true);

            HstURL url = this.urlFactory.createURL(HstURL.RENDER_TYPE, "r1", containerURL, requestContext);
            url.setParameter("param1", "value1");
            url.setParameter("param2", "value2");

            assertFalse("The url is wrong.", url.toString().contains(":param1=value1"));
            assertFalse("The url is wrong.", url.toString().contains(":param2=value2"));
            assertTrue("The url is wrong.", url.toString().contains("param1=value1"));
            assertTrue("The url is wrong.", url.toString().contains("param2=value2"));

            request.setParameter("param1", "value1");
            request.setParameter("param2", "value2");

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

            request.removeAllParameters();
            request.setParameter("param1", "value1");
            request.setParameter("param2", "value2");

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
        } finally {
            ((HstURLFactoryImpl) this.urlFactory).setReferenceNamespaceIgnored(false);
        }
    }

    @Test
    public void testActionContainerURL() throws UnsupportedEncodingException, ContainerException {
        MockHttpServletRequest request = mockRequest();
        HttpServletResponse response = mockResponse();

        request.setParameter("param1", "value1");
        request.setParameter("param2", "value2");

        setResolvedMount(requestContext);

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        requestContext.setBaseURL(containerURL);

        HstContainerURL actionURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        actionURL.setActionWindowReferenceNamespace("b");
        actionURL.setActionParameter("ap1", "one");
        actionURL.setActionParameter("ap2", "two");
        String actionURLPathInfo = this.urlProvider.toURLString(actionURL, requestContext);
        actionURLPathInfo = actionURLPathInfo.substring("/site/content".length());
        request.setPathInfo(actionURLPathInfo);

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
        MockHttpServletRequest request = mockRequest();
        HttpServletResponse response = mockResponse();

        request.setParameter("param1", "value1");
        request.setParameter("param2", "value2");

        setResolvedMount(requestContext);

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        requestContext.setBaseURL(containerURL);

        HstContainerURL resourceURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());
        resourceURL.setResourceWindowReferenceNamespace("b");
        resourceURL.setResourceId("myresource001");
        String resourceURLPathInfo = this.urlProvider.toURLString(resourceURL, requestContext);
        resourceURLPathInfo = resourceURLPathInfo.substring("/site/content".length());
        request.setPathInfo(resourceURLPathInfo);

        assertNull("action window reference namespace is not null.", resourceURL.getActionWindowReferenceNamespace());
        assertNotNull("resource window reference namespace is null.", resourceURL.getResourceWindowReferenceNamespace());
        assertEquals("resource id is wrong.", "myresource001", resourceURL.getResourceId());
        assertEquals("The path info is wrong.", "/news/2008/08", resourceURL.getPathInfo());
    }

    @Test
    public void testComponentRenderingContainerURL() throws UnsupportedEncodingException, ContainerException {
        MockHttpServletRequest request = mockRequest();
        HttpServletResponse response = mockResponse();

        setResolvedMount(requestContext);

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response, requestContext.getResolvedMount());

        requestContext.setBaseURL(containerURL);

        HstURL url = this.urlFactory.createURL(HstURL.COMPONENT_RENDERING_TYPE, "r1", containerURL, requestContext);
        url.setParameter("param1", "value1");
        url.setParameter("param2", "value2");  

        assertTrue("Type is wrong",url.getType().equals(HstURL.COMPONENT_RENDERING_TYPE));
        assertTrue("The url is wrong.", url.toString().contains("r1:param1=value1"));
        assertTrue("The url is wrong.", url.toString().contains("r1:param2=value2"));

        HstContainerURL componentRenderingURL = urlProvider.createURL(containerURL, url);

        componentRenderingURL.setComponentRenderingWindowReferenceNamespace("cr");

        String componentRenderingURLString = urlProvider.toURLString(componentRenderingURL, requestContext);

        assertTrue("componentRenderingURLString is wrong: " + componentRenderingURLString, 
                componentRenderingURLString.contains("?_hn:type=" + HstURL.COMPONENT_RENDERING_TYPE));
        assertTrue("componentRenderingURLString is wrong: " + componentRenderingURLString, 
                componentRenderingURLString.contains("&_hn:ref=" + componentRenderingURL.getComponentRenderingWindowReferenceNamespace()));

        request.setParameter("r1:param1", "value1");
        request.setParameter("r1:param2", "value2");

        assertTrue("The url is wrong.", url.toString().contains("r1:param1=value1"));
        assertTrue("The url is wrong.", url.toString().contains("r1:param2=value2"));

        request.setParameter("r1:param1", "value1");
        request.setParameter("r1:param2", "value2");

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

        request.removeAllParameters();
        request.setParameter("param1", "value1");
        request.setParameter("param2", "value2");

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
        MockHttpServletRequest request = mockRequest();
        HttpServletResponse response = mockResponse();

        setResolvedMount(requestContext);

        request.setQueryString("param=value1_emptyns&r2:param1=value1_r2&r1:param1=value1_r1");
        // when the queryString is parsed in HstRequestUtils, also the parameters need to be set
        request.setParameter("param", "value1_emptyns");
        request.setParameter("r1:param1", "value1_r1");
        request.setParameter("r2:param1", "value1_r2");

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

    @Test
    public void testParseURLsWithRequestPathAndQueryParams() throws Exception {
        ResolvedMount resolvedMount = EasyMock.createNiceMock(ResolvedMount.class);
        EasyMock.expect(resolvedMount.getResolvedMountPath()).andReturn("").anyTimes();
        HstContainerURL baseURL = EasyMock.createNiceMock(HstContainerURL.class);

        EasyMock.replay(resolvedMount);
        EasyMock.replay(baseURL);

        requestContext.setResolvedMount(resolvedMount);
        requestContext.setBaseURL(baseURL);

        String requestPath = "/news";

        Map<String, String[]> queryParams = new HashMap<String, String[]>();
        queryParams.put("_hn:type", new String [] { "action" });
        queryParams.put("_hn:ref", new String [] { "r1" });
        queryParams.put("p1", new String [] { "v1" });
        queryParams.put("p2", new String [] { "v2" });

        HstContainerURL containerURL = urlProvider.parseURL(requestContext, requestContext.getResolvedMount(), requestPath, queryParams);

        assertEquals("r1", containerURL.getActionWindowReferenceNamespace());
        assertNull(containerURL.getResourceWindowReferenceNamespace());
        assertNull(containerURL.getResourceId());
        assertNull(containerURL.getComponentRenderingWindowReferenceNamespace());
        assertNull(containerURL.getParameter("_hn:type"));
        assertNull(containerURL.getParameter("_hn:ref"));
        assertNull(containerURL.getParameter("p-non-existing"));
        assertEquals("v1", containerURL.getParameter("p1"));
        assertEquals("v2", containerURL.getParameter("p2"));

        queryParams = new HashMap<String, String[]>();
        queryParams.put("_hn:type", new String [] { "resource" });
        queryParams.put("_hn:ref", new String [] { "r1" });
        queryParams.put("_hn:rid", new String [] { "resid" });
        queryParams.put("p1", new String [] { "v1" });
        queryParams.put("p2", new String [] { "v2" });

        containerURL = urlProvider.parseURL(requestContext, requestContext.getResolvedMount(), requestPath, queryParams);

        assertNull(containerURL.getActionWindowReferenceNamespace());
        assertEquals("r1", containerURL.getResourceWindowReferenceNamespace());
        assertEquals("resid", containerURL.getResourceId());
        assertNull(containerURL.getComponentRenderingWindowReferenceNamespace());

        queryParams = new HashMap<String, String[]>();
        queryParams.put("_hn:type", new String [] { "component-rendering" });
        queryParams.put("_hn:ref", new String [] { "r1" });
        queryParams.put("p1", new String [] { "v1" });
        queryParams.put("p2", new String [] { "v2" });

        containerURL = urlProvider.parseURL(requestContext, requestContext.getResolvedMount(), requestPath, queryParams);

        assertNull(containerURL.getActionWindowReferenceNamespace());
        assertNull(containerURL.getResourceWindowReferenceNamespace());
        assertEquals("r1", containerURL.getComponentRenderingWindowReferenceNamespace());
    }

}