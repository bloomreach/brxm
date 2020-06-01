/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.services.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.hippoecm.hst.jaxrs.model.content.TextPageRepresentation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * TestTextPageContentResource
 * 
 * @version $Id$
 **/
public class TestTextPageContentResource extends AbstractTestContentResource {
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testGetTextPageResourceAsJSon() throws Exception {

        log.debug("\n****** testGetTextPageResource *******\n");

        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setRequestURI("/site/preview/services/Products/HippoCMS");
        request.setContextPath("/site");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS");
        // request json
        request.addHeader("Accept", "application/json");
        request.setContent(new byte[0]);

        MockHttpServletResponse response = new MockHttpServletResponse();

        invokeJaxrsPipeline(request, response);
        final String contentAsString = response.getContentAsString();
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + contentAsString + "\n");
        }

        log.info("contentAsString {}", contentAsString);
        assertFalse("Expected com.fasterxml.jackson.annotation.JsonIgnore to be honored, through JacksonAnnotationIntrospector", contentAsString.contains(TextPageRepresentation.IGNORED));
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
    }
    @Test
    public void testGetTextPageResource() throws Exception {
        
        log.debug("\n****** testGetTextPageResource *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setRequestURI("/site/preview/services/Products/HippoCMS");
        request.setContextPath("/site");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);

        final String contentAsString = response.getContentAsString();
        if (log.isDebugEnabled()) {

            log.debug("Response Content:\n" + contentAsString + "\n");
        }
        assertTrue("Expected com.fasterxml.jackson.annotation.JsonIgnore to be ignored, JaxbAnnotationIntrospector", contentAsString.contains(TextPageRepresentation.IGNORED));
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
    }
    
    @Test
    public void testGetProtectedTextPageResource() throws Exception {
        
        log.debug("\n****** testGetProtectedTextPageResource *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setRequestURI("/site/preview/services/Products/HippoCMS./protected/");
        request.setContextPath("/site");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./protected/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setRequestURI("/site/preview/services/Products/HippoCMS./permitall/");
        request.setContextPath("/site");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./permitall/");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        
        request = new MockHttpServletRequest(servletContext);
        request.addUserRole("manager");
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setRequestURI("/site/preview/services/Products/HippoCMS./protected/");
        request.setContextPath("/site");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./protected/");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());

        request = new MockHttpServletRequest(servletContext);
        request.addUserRole("manager");
        request.addUserRole("admin");
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setRequestURI("/site/preview/services/Products/HippoCMS./denyall/");
        request.setContextPath("/site");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./denyall/");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testUpdateTextPageResource() throws Exception {
        
        log.debug("\n****** testUpdateTextPageResource *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setRequestURI("/site/preview/services/Products/HippoCMS");
        request.setContextPath("/site");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        String originalTextPageXml = response.getContentAsString();
        String updatingTextPageXml = originalTextPageXml.replace("CMS", "Content_Management_System");
        
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("PUT");
        request.setRequestURI("/site/preview/services/Products/HippoCMS");
        request.setContextPath("/site");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS");
        request.setContentType("text/xml");
        request.setContent(updatingTextPageXml.getBytes());
        
        response = new MockHttpServletResponse();

        invokeJaxrsPipelineAsAdmin(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }

        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        assertWithPreviewUserContentContains("/testcontent/documents/testproject/Products/HippoCMS/HippoCMS/testproject:body",
                "hippostd:content",
                "Content_Management_System");
        
        // revert the temporary changes
        
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("PUT");
        request.setRequestURI("/site/preview/services/Products/HippoCMS");
        request.setContextPath("/site");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS");
        request.setContentType("text/xml");
        request.setContent(originalTextPageXml.getBytes());
        
        response = new MockHttpServletResponse();

        invokeJaxrsPipelineAsAdmin(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
    }
}
