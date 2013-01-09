/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * TestHippoDocumentContentResource
 * 
 * @version $Id$
 **/
public class TestHippoDocumentContentResource extends AbstractTestContentResource {
    
    @Test
    public void testGetDocumentResource() throws Exception {
        
        log.debug("\n****** testGetDocumentResource *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
    }
    
    @Test
    public void testGetDocumentHtmlResource() throws Exception {
        
        log.debug("\n****** testGetDocumentHtmlResource *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS./html/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./html/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
    }
    
    @Test
    public void testUpdateDocumentHtmlResource() throws Exception {
        
        log.debug("\n****** testUpdateDocumentHtmlResource *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS./html/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./html/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        String xhtml = response.getContentAsString();
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("PUT");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS./html/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./html/");
        request.setContentType("text/xml");
        request.setContent(xhtml.replace("CMS", "Content_Management_System").getBytes());
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        
        xhtml = response.getContentAsString();
        assertTrue(xhtml != null && !xhtml.contains("CMS") && xhtml.contains("Content_Management_System"));
        
        // revert the change
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("PUT");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS./html/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./html/");
        request.setContentType("text/xml");
        request.setContent(xhtml.replace("Content_Management_System", "CMS").getBytes());
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
    }
    
    @Test
    public void testGetDocumentHtmlContent() throws Exception {
        
        log.debug("\n****** testGetDocumentHtmlContent *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS./html/content/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./html/content/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
    }
    
    @Test
    public void testUpdateDocumentHtmlContent() throws Exception {
        
        log.debug("\n****** testUpdateDocumentHtmlContent *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS./html/content/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./html/content/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        String html = response.getContentAsString();
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("PUT");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS./html/content/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./html/content/");
        request.setContentType("text/html");
        request.setContent(html.replace("CMS", "Content_Management_System").getBytes());
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        
        html = response.getContentAsString();
        assertTrue(html != null && !html.contains("CMS") && html.contains("Content_Management_System"));
        
        // revert the change
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("PUT");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS./html/content/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS./html/content/");
        request.setContentType("text/html");
        request.setContent(html.replace("Content_Management_System", "CMS").getBytes());
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
    }
}
