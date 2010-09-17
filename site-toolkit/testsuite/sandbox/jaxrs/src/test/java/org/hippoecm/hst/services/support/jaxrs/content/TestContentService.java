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
package org.hippoecm.hst.services.support.jaxrs.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.math.NumberUtils;
import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.core.container.Pipelines;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.site.HstServices;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * TestContentService
 * 
 * @version $Id$
 */
public class TestContentService extends AbstractJaxrsSpringTestCase {
    
    private static final String SITE_MOUNT_POINT = "/testpreview/testproject";
    
    protected Pipelines pipelines;
    protected Pipeline jaxrsPipeline;
    protected ServletConfig servletConfig;
    protected ServletContext servletContext;
    protected HstContainerConfig hstContainerConfig;
    protected ResolvedVirtualHost resolvedVirtualHost;
    protected SiteMount siteMount;
    protected ResolvedSiteMount resolvedSiteMount;
    protected HstMutableRequestContext requestContext;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        HstServices.setComponentManager(getComponentManager());
        
        pipelines = (Pipelines) getComponent(Pipelines.class.getName());
        jaxrsPipeline = this.pipelines.getPipeline("JaxrsPipeline");
        
        servletConfig = new MockServletConfig(new MockServletContext());
        servletContext = servletConfig.getServletContext();
        
        hstContainerConfig = new HstContainerConfig() {
            public ClassLoader getContextClassLoader() {
                return TestContentService.class.getClassLoader();
            }
            public ServletContext getServletContext() {
                return servletContext;
            }
        };
        
        resolvedVirtualHost = EasyMock.createNiceMock(ResolvedVirtualHost.class);
        EasyMock.expect(resolvedVirtualHost.getResolvedHostName()).andReturn("localhost").anyTimes();
        EasyMock.expect(resolvedVirtualHost.getPortNumber()).andReturn(8085).anyTimes();

        siteMount = EasyMock.createNiceMock(SiteMount.class);
        EasyMock.expect(siteMount.getMountPoint()).andReturn(SITE_MOUNT_POINT).anyTimes();
        
        resolvedSiteMount = EasyMock.createNiceMock(ResolvedSiteMount.class);
        EasyMock.expect(resolvedSiteMount.getResolvedVirtualHost()).andReturn(resolvedVirtualHost).anyTimes();
        EasyMock.expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount).anyTimes();
        EasyMock.expect(resolvedSiteMount.getResolvedMountPath()).andReturn("/preview/services").anyTimes();
        
        EasyMock.replay(resolvedVirtualHost);
        EasyMock.replay(siteMount);
        EasyMock.replay(resolvedSiteMount);
        
        requestContext = ((HstRequestContextComponent)getComponent(HstRequestContextComponent.class.getName())).create(false);
        requestContext.setServletContext(servletContext);
        requestContext.setResolvedSiteMount(resolvedSiteMount);
    }
    
    @Test
    public void testDemo() throws Exception {
        /* 
         * retrieves customer json data...
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/customerservice/customers/123/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/customerservice/customers/123/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();

        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getContentAsString());
        assertTrue(response.getContentAsString().contains("123"));
        assertTrue(response.getContentAsString().contains("John"));
        
        /*
         * updating the existing customer...
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("PUT");
        request.setRequestURI("/testapp/preview/services/customerservice/customers/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/customerservice/customers/");
        request.setContentType("application/json");
        request.setContent("{\"id\":123,\"name\":\"John Doe\"}".getBytes());

        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        /*
         * adding a new customer...
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("POST");
        request.setRequestURI("/testapp/preview/services/customerservice/customers/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/customerservice/customers/");
        request.setContentType("application/json");
        request.setContent("{\"name\":\"Jisung Park\"}".getBytes());

        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getContentAsString());
        assertTrue(response.getContentAsString().contains("Jisung Park"));
        
        /*
         * deleting a new customer...
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("DELETE");
        request.setRequestURI("/testapp/preview/services/customerservice/customers/123/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/customerservice/customers/123/");
        request.setContent(new byte[0]);

        response = new MockHttpServletResponse ();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testGetContentNodeByUUID() throws Exception {
        /*
         * Retrieves folder xml by uuid
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/uuid/abababab-5fa8-48a8-b03b-4524373d992a/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/uuid/abababab-5fa8-48a8-b03b-4524373d992a/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        Element root = document.getDocumentElement();
        assertEquals("folder", root.getNodeName());
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("string(/folder/uuid)");
        assertEquals("abababab-5fa8-48a8-b03b-4524373d992a", expr.evaluate(document));
    }
    
    @Test
    public void testGetContentItem() throws Exception {
        /*
         * Retrieves document xml by path
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Products/HippoCMS");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Products/HippoCMS");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        Element root = document.getDocumentElement();
        assertEquals("document", root.getNodeName());
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("string(/document/children/node[string(./name)='testproject:body']/uri)");
        assertEquals("http://localhost:8085/testapp/preview/services/contentservice/Products/HippoCMS/HippoCMS/testproject:body",
                expr.evaluate(document));
        expr = xpath.compile("string(/document/properties/property[string(./name)='testproject:title']/uri)");
        assertEquals("http://localhost:8085/testapp/preview/services/contentservice/Products/HippoCMS/HippoCMS/testproject:title",
                expr.evaluate(document));
        
        /*
         * Retrieves property xml by path
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Products/HippoCMS/HippoCMS/testproject:title");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Products/HippoCMS/HippoCMS/testproject:title");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        root = document.getDocumentElement();
        assertEquals("property", root.getNodeName());
        expr = xpath.compile("string(/property/values/value)");
        assertEquals("Hippo CMS", expr.evaluate(document));
        
        /*
         * Retrieves child node xml of a document by path
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Products/HippoCMS/HippoCMS/testproject:body");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Products/HippoCMS/HippoCMS/testproject:body");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        root = document.getDocumentElement();
        assertEquals("node", root.getNodeName());
    }
    
    @Test
    public void testQueryContentItems() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/query/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/query/");
        request.setQueryString("query=cms");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("string(/data/beginIndex)");
        String value = expr.evaluate(document);
        assertTrue(NumberUtils.isNumber(value));
        assertEquals(0, NumberUtils.toInt(value));
        
        expr = xpath.compile("string(/data/totalSize)");
        value = expr.evaluate(document);
        assertTrue(NumberUtils.isNumber(value));
        assertTrue(NumberUtils.toInt(value) > 0);
        
        expr = xpath.compile("count(/data/documents/document)");
        value = expr.evaluate(document);
        assertTrue(NumberUtils.isNumber(value));
        assertTrue(NumberUtils.toInt(value) > 0);
    }
    
    @Test
    public void testDeleteContentNode() throws Exception {
        /*
         * Delete a document by path
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("DELETE");
        request.setRequestURI("/testapp/preview/services/contentservice/Solutions/SolutionsPage");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Solutions/SolutionsPage");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        /*
         * Check if the deleted document is not found.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Solutions/SolutionsPage");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Solutions/SolutionsPage");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);

        assertTrue(Response.Status.OK.getStatusCode() != response.getStatus());
    }
    
    @Test
    public void testCreateContentNode() throws Exception {
        /*
         * Create a textpage document
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("POST");
        request.setRequestURI("/testapp/preview/services/contentservice/Solutions");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Solutions");
        request.setContentType("application/xml");
        String content = 
            "<document>" +
            "<name>SolutionsPage2</name>" +
            "<properties>" +
            "<property>" +
            "<name>jcr:primaryType</name>" +
            "<typeName>String</typeName>" +
            "<values>" +
            "<value>testproject:textpage</value>" +
            "</values>" +
            "</property>" +
            "</properties>" +
            "</document>";
        request.setContent(content.getBytes());
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        
        /*
         * Check if the created document is found.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Solutions/SolutionsPage2");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Solutions/SolutionsPage2");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        /*
         * Check if the created document has body child node.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Solutions/SolutionsPage2/SolutionsPage2/testproject:body");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Solutions/SolutionsPage2/SolutionsPage2/testproject:body");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        /*
         * Create a node under the context relative root node.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("POST");
        request.setRequestURI("/testapp/preview/services/contentservice/node/jcr:root/testcontent/documents/testproject");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/node/jcr:root/testcontent/documents/testproject");
        request.setContentType("application/xml");
        content = "<node><primaryNodeTypeName>hippostd:folder</primaryNodeTypeName><name>afolder</name></node>";
        request.setContent(content.getBytes());
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        
        /*
         * Check if the created node
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/afolder");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/afolder");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);

        assertTrue(Response.Status.OK.getStatusCode() == response.getStatus());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        Element root = document.getDocumentElement();
        assertEquals("folder", root.getNodeName());
    }
    
    @Test
    public void testUpdateContentItem() throws Exception {
        /*
         * Retrieves document xml by path
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/News/News1/News1");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/News/News1/News1");
        request.setQueryString("pv=testproject:title&pv=testproject:summary");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        Element root = document.getDocumentElement();
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("string(/document/properties/property[string(./name)='testproject:title']/values/value)");
        assertEquals("News Item 1", expr.evaluate(document).trim());
        expr = xpath.compile("string(/document/properties/property[string(./name)='testproject:summary']/values/value)");
        assertEquals("Summary about news item 1", expr.evaluate(document));
        
        /*
         * Update properties 
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("PUT");
        request.setRequestURI("/testapp/preview/services/contentservice/News/News1/News1");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/News/News1/News1");
        request.setQueryString(null);
        request.setContentType("application/xml");
        String content = 
            "<property>" +
            "<name>testproject:title</name>" +
            "<typeName>String</typeName>" +
            "<values>" +
            "<value>News Item 1 - updated</value>" +
            "</values>" +
            "</property>";
        request.setContent(content.getBytes());
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        /*
         * Check the changes again.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/News/News1/News1");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/News/News1/News1");
        request.setQueryString("pv=testproject:title&pv=testproject:summary");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        root = document.getDocumentElement();
        xpath = XPathFactory.newInstance().newXPath();
        expr = xpath.compile("string(/document/properties/property[string(./name)='testproject:title']/values/value)");
        assertEquals("News Item 1 - updated", expr.evaluate(document));
        expr = xpath.compile("string(/document/properties/property[string(./name)='testproject:summary']/values/value)");
        assertEquals("Summary about news item 1", expr.evaluate(document));
    }
    
    private void invokeJaxrsPipeline(HttpServletRequest request, HttpServletResponse response) throws ContainerException {
    	request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        jaxrsPipeline.beforeInvoke(hstContainerConfig, requestContext, request, response);
        
        try {
            jaxrsPipeline.invoke(hstContainerConfig, requestContext, request, response);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            jaxrsPipeline.afterInvoke(hstContainerConfig, requestContext, request, response);
        }
    }
}
