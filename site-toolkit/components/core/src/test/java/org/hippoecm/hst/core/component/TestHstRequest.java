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
package org.hippoecm.hst.core.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Enumeration;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerConfigurationImpl;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstComponentWindowImpl;
import org.hippoecm.hst.core.container.HstContainerURLImpl;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.request.HstRequestContextImpl;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Test;

public class TestHstRequest extends AbstractSpringTestCase {
    
    protected HttpServletRequest servletRequest;
    protected HstRequestContextImpl requestContext;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        this.servletRequest = getComponent(HttpServletRequest.class.getName());
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty(HstRequestImpl.CONTAINER_ATTR_NAME_PREFIXES_PROP_KEY, "COM.iiibbbmmm., com.sssuuunnn.");
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        this.requestContext = new HstRequestContextImpl(null);
        this.requestContext.setContainerConfiguration(containerConfiguration);
        HstURLFactory urlFactory = getComponent(HstURLFactory.class.getName());
        this.requestContext.setURLFactory(urlFactory);
        HstContainerURLImpl baseURL = new HstContainerURLImpl();
        this.requestContext.setBaseURL(baseURL);
    }
    
    @Test
    public void testRequestAttributes() {

        // Sets java servlet attributes
        this.servletRequest.setAttribute("javax.servlet.include.request_uri", "/jsp/included.jsp");
        // Sets attributes for portlet environment
        this.servletRequest.setAttribute("javax.portlet.request", "something");
        // Sets request context
        this.servletRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, this.requestContext);
        
        HstComponentWindow rootWindow = new HstComponentWindowImpl("news", "news", "", null, null, null, null);
        HstComponentWindow headWindow = new HstComponentWindowImpl("head", "h", "h", null, null, null, rootWindow);
        HstComponentWindow bodyWindow = new HstComponentWindowImpl("body", "b", "b", null, null, null, rootWindow);
        
        HstRequest hstRequestForRootWindow = new HstRequestImpl(this.servletRequest, this.requestContext, rootWindow, HstRequest.RENDER_PHASE);
        HstRequest hstRequestForHeadWindow = new HstRequestImpl(this.servletRequest, this.requestContext, headWindow, HstRequest.RENDER_PHASE);
        HstRequest hstRequestForBodyWindow = new HstRequestImpl(this.servletRequest, this.requestContext, bodyWindow, HstRequest.RENDER_PHASE);
        
        assertNotNull(this.servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT));
        assertNotNull(hstRequestForRootWindow.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT));
        assertNotNull(hstRequestForHeadWindow.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT));
        assertNotNull(hstRequestForBodyWindow.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT));
        
        hstRequestForRootWindow.setAttribute("name", "root");
        hstRequestForHeadWindow.setAttribute("name", "head");
        hstRequestForBodyWindow.setAttribute("name", "body");
        
        assertEquals("root", hstRequestForRootWindow.getAttribute("name"));
        assertEquals("head", hstRequestForHeadWindow.getAttribute("name"));
        assertEquals("body", hstRequestForBodyWindow.getAttribute("name"));
        
        assertEquals("/jsp/included.jsp", hstRequestForRootWindow.getAttribute("javax.servlet.include.request_uri"));
        assertEquals("/jsp/included.jsp", hstRequestForHeadWindow.getAttribute("javax.servlet.include.request_uri"));
        assertEquals("/jsp/included.jsp", hstRequestForBodyWindow.getAttribute("javax.servlet.include.request_uri"));
        
        assertEquals("something", hstRequestForRootWindow.getAttribute("javax.portlet.request"));
        assertEquals("something", hstRequestForHeadWindow.getAttribute("javax.portlet.request"));
        assertEquals("something", hstRequestForBodyWindow.getAttribute("javax.portlet.request"));
        
        // Remove an attribute from bodyWindow
        hstRequestForBodyWindow.removeAttribute("name");
        assertNull("The name attribute of body window request is still available! name: " + hstRequestForBodyWindow.getAttribute("name"), hstRequestForBodyWindow.getAttribute("name"));
    }
    
    private SortedSet getSortedAttributeNames(HttpServletRequest request) {
        SortedSet attrNames = new TreeSet();
        
        for (Enumeration enumParams = request.getAttributeNames(); enumParams.hasMoreElements(); ) {
            attrNames.add(enumParams.nextElement());
        }
        
        return attrNames;
    }
    
}
