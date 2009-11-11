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
import java.lang.reflect.Method;

import javax.jcr.Session;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.proxy.Invoker;
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.proxy.ProxyFactory;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.After;
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
public class TestContentService extends AbstractHstTestCase {
    
    private static final String PREVIEW_SITE_CONTENT_PATH = "/testpreview/testproject/hst:content";
    
    private MockServletContext servletContext;
    private MockServletConfig servletConfig;
    private HttpServlet jaxrsServlet;
    private Session session;
    private HstRequestContext hstRequestContext;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        jaxrsServlet = new CXFNonSpringJaxrsServlet();
        
        servletContext = new MockServletContext();
        servletContext.setServletContextName("testapp");
        servletContext.setContextPath("/testapp");
        
        servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("jaxrs.serviceClasses", "org.hippoecm.hst.services.support.jaxrs.demo.CustomerService org.hippoecm.hst.services.support.jaxrs.content.ContentService");
        
        jaxrsServlet.init(servletConfig);
        
        session = getSession();
        
        assertNotNull(session);
        
        ProxyFactory factory = new ProxyFactory();
        Invoker invoker = new Invoker() {
            public Object invoke(Object proxy, Method method, Object [] args) throws Throwable {
                if ("getSession".equals(method.getName())) {
                    return session;
                }
                throw new UnsupportedOperationException("Unsupported: " + method);
            }
        };
        
        hstRequestContext = (HstRequestContext) factory.createInvokerProxy(Thread.currentThread().getContextClassLoader(), invoker, new Class [] { HstRequestContext.class });
    }
    
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        
        if (session != null) {
            try {
                session.logout();
            } catch (Exception ignore) {
            }
        }
        
        jaxrsServlet.destroy();
    }
    
    @Test
    public void testDemo() throws Exception {
        /* 
         * retrieves customer json data...
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/customerservice/customers/123/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/customerservice/customers/123/");
        
        MockHttpServletResponse response = new MockHttpServletResponse();

        jaxrsServlet.service(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getContentAsString());
        assertEquals("{\"Customer\":{\"id\":123,\"name\":\"John\"}}", response.getContentAsString());
        
        /*
         * updating the existing customer...
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("PUT");
        request.setRequestURI("/testapp/preview/services/customerservice/customers/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/customerservice/customers/");
        request.setContentType("application/json");
        request.setContent("{\"Customer\":{\"id\":123,\"name\":\"John Doe\"}}".getBytes());

        response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        /*
         * adding a new customer...
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("POST");
        request.setRequestURI("/testapp/preview/services/customerservice/customers/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/customerservice/customers/");
        request.setContentType("application/json");
        request.setContent("{\"Customer\":{\"name\":\"Jisung Park\"}}".getBytes());

        response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getContentAsString());
        assertTrue(response.getContentAsString().startsWith("{\"Customer\":"));
        assertTrue(response.getContentAsString().contains("\"name\":\"Jisung Park\""));
        
        /*
         * deleting a new customer...
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("DELETE");
        request.setRequestURI("/testapp/preview/services/customerservice/customers/123/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/customerservice/customers/123/");

        response = new MockHttpServletResponse ();
        
        jaxrsServlet.service(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testGetContentNodeByUUID() throws Exception {
        /*
         * Retrieves folder xml by uuid
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/uuid/abababab-5fa8-48a8-b03b-4524373d992a/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/uuid/abababab-5fa8-48a8-b03b-4524373d992a/");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);
        
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
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Products/HippoCMS");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Products/HippoCMS");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        Element root = document.getDocumentElement();
        assertEquals("document", root.getNodeName());
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("string(/document/node[@name='testproject:body']/@uri)");
        assertEquals("http://localhost:8085/testapp/preview/services/contentservice/Products/HippoCMS/HippoCMS/testproject:body",
                expr.evaluate(document));
        expr = xpath.compile("string(/document/property[@name='testproject:title']/@uri)");
        assertEquals("http://localhost:8085/testapp/preview/services/contentservice/Products/HippoCMS/HippoCMS/testproject:title",
                expr.evaluate(document));
        
        /*
         * Retrieves property xml by path
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Products/HippoCMS/HippoCMS/testproject:title");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Products/HippoCMS/HippoCMS/testproject:title");
        
        response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        root = document.getDocumentElement();
        assertEquals("property", root.getNodeName());
        expr = xpath.compile("string(/property/value)");
        assertEquals("Hippo CMS", expr.evaluate(document));
        
        /*
         * Retrieves child node xml of a document by path
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Products/HippoCMS/HippoCMS/testproject:body");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Products/HippoCMS/HippoCMS/testproject:body");
        
        response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        root = document.getDocumentElement();
        assertEquals("node", root.getNodeName());
    }
    
    @Test
    public void testDeleteContentNode() throws Exception {
        /*
         * Delete a document by path
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("DELETE");
        request.setRequestURI("/testapp/preview/services/contentservice/Solutions/SolutionsPage");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Solutions/SolutionsPage");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        /*
         * Check if the deleted document is not found.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Solutions/SolutionsPage");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Solutions/SolutionsPage");
        
        response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);

        assertTrue(Response.Status.OK.getStatusCode() != response.getStatus());
    }
    
    @Test
    public void testCreateContentNode() throws Exception {
        /*
         * Create a textpage document
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
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
            "<property name=\"jcr:primaryType\" typeName=\"String\">" +
            "<value>testproject:textpage</value>" +
            "</property>" +
            "</document>";
        request.setContent(content.getBytes());
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);
        
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        
        /*
         * Check if the created document is found.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Solutions/SolutionsPage2");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Solutions/SolutionsPage2");
        
        response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        /*
         * Check if the created document has body child node.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/Solutions/SolutionsPage2/SolutionsPage2/testproject:body");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/Solutions/SolutionsPage2/SolutionsPage2/testproject:body");
        
        response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        /*
         * Create a node under the context relative root node.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
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
        
        jaxrsServlet.service(request, response);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        
        /*
         * Check if the created node
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/contentservice/afolder");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/contentservice/afolder");
        
        response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);

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
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
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
        
        jaxrsServlet.service(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        Element root = document.getDocumentElement();
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("string(/document/property[@name='testproject:title']/value)");
        assertEquals("News Item 1", expr.evaluate(document).trim());
        expr = xpath.compile("string(/document/property[@name='testproject:summary']/value)");
        assertEquals("Summary about news item 1", expr.evaluate(document));
        
        /*
         * Update properties 
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
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
            "<value>News Item 1 - updated</value>" +
            "</property>";
        request.setContent(content.getBytes());
        
        response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        /*
         * Check the changes again.
         */
        request = new MockHttpServletRequest(servletContext);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, hstRequestContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("http");
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
        
        jaxrsServlet.service(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        root = document.getDocumentElement();
        xpath = XPathFactory.newInstance().newXPath();
        expr = xpath.compile("string(/document/property[@name='testproject:title']/value)");
        assertEquals("News Item 1 - updated", expr.evaluate(document));
        expr = xpath.compile("string(/document/property[@name='testproject:summary']/value)");
        assertEquals("Summary about news item 1", expr.evaluate(document));
    }
    
}
