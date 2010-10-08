/*
 *  Copyright 2010 Hippo.
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

import java.io.ByteArrayInputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.container.HstContainerRequest;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.core.container.Pipelines;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.jaxrs.services.AbstractJaxrsSpringTestCase;
import org.hippoecm.hst.site.HstServices;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;

/**
 * TestContentService
 * 
 * @version $Id$
 **/
public class TestNodeResource extends AbstractJaxrsSpringTestCase {
    
    private static final String SITE_MOUNT_POINT = "/hst:hst/hst:sites/testproject-preview";
    
    protected Pipelines pipelines;
    protected Pipeline jaxrsPipeline;
    protected ServletConfig servletConfig;
    protected ServletContext servletContext;
    protected HstContainerConfig hstContainerConfig;
    protected ResolvedVirtualHost resolvedVirtualHost;
    protected SiteMount siteMount;
    protected ResolvedSiteMount resolvedSiteMount;
    protected HstMutableRequestContext requestContext;
    protected HstURLFactory urlFactory;
    protected HstContainerURLProvider urlProvider;
    
    protected String[] getConfigurations() {
        return new String[] { "org/hippoecm/hst/jaxrs/services/content/TestContentServices.xml" };
    }
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        HstServices.setComponentManager(getComponentManager());
        
        pipelines = (Pipelines) getComponent(Pipelines.class.getName());
        jaxrsPipeline = this.pipelines.getPipeline("JaxrsPipeline");
        
        servletConfig = new MockServletConfig(new MockServletContext() { public String getRealPath(String path) { return null; } });
        servletContext = servletConfig.getServletContext();
        
        hstContainerConfig = new HstContainerConfig() {
            public ClassLoader getContextClassLoader() {
                return TestNodeResource.class.getClassLoader();
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
        
        urlFactory = getComponent(HstURLFactory.class.getName());
        urlProvider = this.urlFactory.getContainerURLProvider();        
    }
    
    @Test
    public void testRepositoryNodeResource() throws Exception {
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
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testRepositoryNodeResourceChildren() throws Exception {
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
        request.setRequestURI("/testapp/preview/services/News/2009/April./children");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/News/2009/April./children");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertEquals(3, document.getDocumentElement().getChildNodes().getLength());
    }
    
    @Test
    public void testRepositoryNodeResourceProperty() throws Exception {
        /*
         * Retrieves property xml by path
         */
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS./property/testproject:title");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS");
        request.setContent(new byte[0]);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        String name = XPathFactory.newInstance().newXPath().compile("//name").evaluate(document);
        assertEquals("testproject:title", name);
        
        /*
         * Sets property by path and form parameter
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("POST");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS./property/testproject:title");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS");
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setContent("pv=Hippo CMS2".getBytes());
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        String title = XPathFactory.newInstance().newXPath().compile("//value").evaluate(document);
        assertEquals("Hippo CMS2", title);
        
        /*
         * Restore the original title of the property by path and form parameter
         */
        request = new MockHttpServletRequest(servletContext);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("POST");
        request.setRequestURI("/testapp/preview/services/Products/HippoCMS./property/testproject:title");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/Products/HippoCMS");
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setContent("pv=Hippo CMS".getBytes());
        
        response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        title = XPathFactory.newInstance().newXPath().compile("//value").evaluate(document);
        assertEquals("Hippo CMS", title);
    }
    
    private void invokeJaxrsPipeline(HttpServletRequest request, HttpServletResponse response) throws ContainerException {
    	request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
    	HstContainerRequest cr = new HstContainerRequestImpl(request, "./");
    	requestContext.setPathSuffix(cr.getPathSuffix());
    	requestContext.setBaseURL(urlProvider.parseURL(cr, response, requestContext.getResolvedSiteMount()));
        jaxrsPipeline.beforeInvoke(hstContainerConfig, requestContext, cr, response);
        
        try {
            jaxrsPipeline.invoke(hstContainerConfig, requestContext, cr, response);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            jaxrsPipeline.afterInvoke(hstContainerConfig, requestContext, cr, response);
        }
    }
}
