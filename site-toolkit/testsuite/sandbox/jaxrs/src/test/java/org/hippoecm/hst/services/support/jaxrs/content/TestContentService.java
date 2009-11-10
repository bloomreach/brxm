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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class TestContentService {

    private MockServletContext servletContext;
    private MockServletConfig servletConfig;
    private HttpServlet jaxrsServlet;
    
    @Before
    public void setUp() throws ServletException {
        jaxrsServlet = new CXFNonSpringJaxrsServlet();
        
        servletContext = new MockServletContext();
        servletContext.setServletContextName("testapp");
        servletContext.setContextPath("/testapp");
        
        servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("jaxrs.serviceClasses", "org.hippoecm.hst.services.support.jaxrs.demo.CustomerService org.hippoecm.hst.services.support.jaxrs.content.ContentService");
        
        jaxrsServlet.init(servletConfig);
    }
    
    @After
    public void tearDown() {
        jaxrsServlet.destroy();
    }
    
    @Test
    public void testDemo() throws ServletException, IOException {
        // retrieves customer json data...
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/customerservice/customers/123/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/customerservice/customers/123/");

        jaxrsServlet.service(request, response);
        
        assertEquals(200, response.getStatus());
        assertNotNull(response.getContentAsString());
        assertEquals("{\"Customer\":{\"id\":123,\"name\":\"John\"}}", response.getContentAsString());
        
        // updating the existing customer...
        request = new MockHttpServletRequest(servletContext);
        response = new MockHttpServletResponse();
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
        
        assertEquals(200, response.getStatus());
        
        // adding a new customer...
        request = new MockHttpServletRequest(servletContext);
        response = new MockHttpServletResponse();
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
        
        assertEquals(200, response.getStatus());
        assertNotNull(response.getContentAsString());
        assertTrue(response.getContentAsString().startsWith("{\"Customer\":"));
        assertTrue(response.getContentAsString().contains("\"name\":\"Jisung Park\""));
        
        // deleting a new customer...
        request = new MockHttpServletRequest(servletContext);
        response = new MockHttpServletResponse();
        request.setProtocol("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setMethod("DELETE");
        request.setRequestURI("/testapp/preview/services/customerservice/customers/123/");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/customerservice/customers/123/");

        response = new MockHttpServletResponse();
        
        jaxrsServlet.service(request, response);
        
        assertEquals(200, response.getStatus());
    }
    
    @Test
    public void testGetContentNodeByUUID() throws ServletException, IOException {
        // TODO:
    }
    
    @Test
    public void testGetContentItem() throws ServletException, IOException {
        // TODO:
    }
    
    @Test
    public void testDeleteContentNode() throws ServletException, IOException {
        // TODO:
    }
    
    @Test
    public void testCreateContentDocument() throws ServletException, IOException {
        // TODO:
    }
    
    @Test
    public void testCreateContentNode() throws ServletException, IOException {
        // TODO:
    }
    
    @Test
    public void testSetContentProperty() throws ServletException, IOException {
        // TODO:
    }
    
}
