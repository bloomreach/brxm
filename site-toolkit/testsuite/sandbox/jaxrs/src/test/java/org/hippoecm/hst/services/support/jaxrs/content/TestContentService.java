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
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.core.container.Pipelines;
import org.hippoecm.hst.site.HstServices;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * TestContentService
 * 
 * @version $Id$
 */
public class TestContentService extends AbstractJaxrsSpringTestCase {
    
    private static final String PREVIEW_SITE_CONTENT_PATH = "/testpreview/testproject/hst:content";
    
    protected Pipelines pipelines;
    protected Pipeline jaxrsPipeline;
    protected ServletConfig servletConfig;
    protected ServletContext servletContext;
    protected HstContainerConfig hstContainerConfig;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        HstServices.setComponentManager(getComponentManager());
        
        pipelines = (Pipelines) getComponent(Pipelines.class.getName());
        jaxrsPipeline = this.pipelines.getPipeline("JaxrsPipeline");
        
        servletConfig = getComponent("jaxrsServiceServletConfig");
        servletContext = servletConfig.getServletContext();
        
        hstContainerConfig = new HstContainerConfig() {
            public ClassLoader getContextClassLoader() {
                return TestContentService.class.getClassLoader();
            }
            public ServletConfig getServletConfig() {
                return servletConfig;
            }
        };
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
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        Element root = document.getDocumentElement();
        assertEquals("folder", root.getNodeName());
        assertEquals("abababab-5fa8-48a8-b03b-4524373d992a", root.getAttribute("uuid"));
    }
    
    @Test
    public void testGetContentItem() throws Exception {
        /*
         * Retrieves document xml by path
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        Element root = document.getDocumentElement();
        assertEquals("document", root.getNodeName());
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("string(/document/nodes/node[@name='testproject:body']/@uri)");
        assertEquals("http://localhost:8085/testapp/preview/services/contentservice/Products/HippoCMS/HippoCMS/testproject:body",
                expr.evaluate(document));
        expr = xpath.compile("string(/document/properties/property[@name='testproject:title']/@uri)");
        assertEquals("http://localhost:8085/testapp/preview/services/contentservice/Products/HippoCMS/HippoCMS/testproject:title",
                expr.evaluate(document));
        
        /*
         * Retrieves property xml by path
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("string(/data/@beginIndex)");
        String value = expr.evaluate(document);
        assertTrue(NumberUtils.isNumber(value));
        assertEquals(0, NumberUtils.toInt(value));
        
        expr = xpath.compile("string(/data/@totalSize)");
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
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        /*
         * Check if the deleted document is not found.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
            "<document name=\"SolutionsPage2\">" +
            "<properties>" +
            "<property name=\"jcr:primaryType\" typeName=\"String\">" +
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
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        /*
         * Check if the created document has body child node.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        /*
         * Create a node under the context relative root node.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        content = "<node primaryNodeTypeName=\"hippostd:folder\" name=\"afolder\"/>";
        request.setContent(content.getBytes());
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        
        /*
         * Check if the created node
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        Element root = document.getDocumentElement();
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("string(/document/properties/property[@name='testproject:title']/values/value)");
        assertEquals("News Item 1", expr.evaluate(document).trim());
        expr = xpath.compile("string(/document/properties/property[@name='testproject:summary']/values/value)");
        assertEquals("Summary about news item 1", expr.evaluate(document));
        
        /*
         * Update properties 
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
            "<property name=\"testproject:title\" typeName=\"String\">" +
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
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
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
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        root = document.getDocumentElement();
        xpath = XPathFactory.newInstance().newXPath();
        expr = xpath.compile("string(/document/properties/property[@name='testproject:title']/values/value)");
        assertEquals("News Item 1 - updated", expr.evaluate(document));
        expr = xpath.compile("string(/document/properties/property[@name='testproject:summary']/values/value)");
        assertEquals("Summary about news item 1", expr.evaluate(document));
    }
    
    private void invokeJaxrsPipeline(HttpServletRequest request, HttpServletResponse response) throws ContainerException {
        jaxrsPipeline.beforeInvoke(hstContainerConfig, request, response);
        
        try {
            jaxrsPipeline.invoke(hstContainerConfig, request, response);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            jaxrsPipeline.afterInvoke(hstContainerConfig, request, response);
        }
    }
}
