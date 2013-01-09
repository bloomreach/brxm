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

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * TestHippoFolderContentResource
 * 
 * @version $Id$
 **/
public class TestHippoFolderContentResource extends AbstractTestContentResource {
    
    @Test
    public void testGetFolderResources() throws Exception {
        
        log.debug("\n****** testGetFolderResources *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/News/2009/April./folders/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/News/2009/April./folders/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        int folderNodeCount = Integer.parseInt(XPathFactory.newInstance().newXPath().compile("count(//folder)").evaluate(document));
        assertTrue(folderNodeCount > 0);
        int documentNodeCount = Integer.parseInt(XPathFactory.newInstance().newXPath().compile("count(//document)").evaluate(document));
        assertEquals(0, documentNodeCount);
    }
    
    @Test
    public void testGetFolderResource() throws Exception {
        
        log.debug("\n****** testGetFolderResource *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/News/2009/April./folders/Day5/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/News/2009/April./folders/Day5/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        String name = XPathFactory.newInstance().newXPath().compile("//folder/name").evaluate(document);
        assertEquals("Day5", name);
    }
    
    @Test
    public void testCreateAndDeleteFolderResource() throws Exception {
        
        log.debug("\n****** testCreateAndDeleteFolderResource *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("POST");
        request.setRequestURI("/testapp/preview/services/News/2009/April./folders/unittestfolder1/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/News/2009/April./folders/unittestfolder1/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("DELETE");
        request.setRequestURI("/testapp/preview/services/News/2009/April./folders/unittestfolder1/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/News/2009/April./folders/unittestfolder1/");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
    }
    
    @Test
    public void testGetDocumentResources() throws Exception {
        
        log.debug("\n****** testGetDocumentResources *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/News/2009/April./documents/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/News/2009/April./documents/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        int folderNodeCount = Integer.parseInt(XPathFactory.newInstance().newXPath().compile("count(//folder)").evaluate(document));
        assertEquals(0, folderNodeCount);
        int documentNodeCount = Integer.parseInt(XPathFactory.newInstance().newXPath().compile("count(//document)").evaluate(document));
        assertTrue(documentNodeCount > 0);
    }
    
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
        request.setRequestURI("/testapp/preview/services/Products./documents/HippoCMS");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products./documents/HippoCMS");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        String name = XPathFactory.newInstance().newXPath().compile("/document/name/text()").evaluate(document);
        assertEquals("HippoCMS", name);
    }
    
    @Test
    public void testCreateAndDeleteDocumentResource() throws Exception {
        
        log.debug("\n****** testCreateAndDeleteDocumentResource *******\n");
        
        log.debug("Create a document first.");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("POST");
        request.setRequestURI("/testapp/preview/services/Products./documents/newdocumentfortest/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products./documents/newdocumentfortest/");
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setContent("type=testproject:textpage".getBytes());
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());

        log.debug("Retrieve the created document.");
        
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/Products./documents/newdocumentfortest");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products./documents/newdocumentfortest");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        log.debug("Delete the created document.");
        
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("DELETE");
        request.setRequestURI("/testapp/preview/services/Products./documents/newdocumentfortest/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products./documents/newdocumentfortest/");
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setContent(new byte[0]);
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
    }
    
}
