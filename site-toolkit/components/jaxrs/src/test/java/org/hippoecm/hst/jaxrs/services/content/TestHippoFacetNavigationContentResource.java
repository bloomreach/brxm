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

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * TestHippoFacetNavigationContentResource
 * 
 * @version $Id$
 **/
public class TestHippoFacetNavigationContentResource extends AbstractTestContentResource {
    
    @Test
    public void testGetFacetNavigationResource() throws Exception {
        
        log.debug("\n****** testGetFacetNavigationResource *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/facettestproducts/faceted/tag cloud browsing/tags/italian");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/facettestproducts/faceted/tag cloud browsing/tags/italian");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
    }
    
    @Test
    public void testGetFacetResultSetResource() throws Exception {
        
        log.debug("\n****** testGetFacetResultSetResource *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/facettestproducts/faceted/tag cloud browsing/tags/italian./facetresult");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/facettestproducts/faceted/tag cloud browsing/tags/italian./facetresult");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
    }
    
    @Test
    public void testGetRootFacetNavigationResource() throws Exception {
        
        log.debug("\n****** testGetRootFacetNavigationResource *******\n");
        
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/facettestproducts/faceted/tag cloud browsing/tags/italian./root");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/facettestproducts/faceted/tag cloud browsing/tags/italian./root");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
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
        request.setRequestURI("/testapp/preview/services/facettestproducts/faceted/tag cloud browsing/tags/italian./folders/tags/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/facettestproducts/faceted/tag cloud browsing/tags/italian./folders/tags/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
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
        request.setRequestURI("/testapp/preview/services/facettestproducts/faceted/tag cloud browsing/tags/italian./documents/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/facettestproducts/faceted/tag cloud browsing/tags/italian./documents/");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        if (log.isDebugEnabled()) {
            log.debug("Response Content:\n" + response.getContentAsString() + "\n");
        }
        
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
    }

}
