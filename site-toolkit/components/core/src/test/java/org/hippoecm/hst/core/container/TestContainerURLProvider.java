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
package org.hippoecm.hst.core.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestContainerURLProvider extends AbstractSpringTestCase {

    protected HstURLFactory urlFactory;
    protected HstContainerURLProvider urlProvider;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.urlFactory = getComponent(HstURLFactory.class.getName());
        this.urlProvider = this.urlFactory.getServletUrlProvider();
    }
    
    @Test
    public void testBasicCotnainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");
        
        HstContainerURL containerURL = this.urlProvider.parseURL(request, response);

        assertNull("action window reference namespace is not null.", containerURL.getActionWindowReferenceNamespace());
        assertNull("resource window reference namespace is not null.", containerURL.getResourceWindowReferenceNamespace());
        assertEquals("The path info is wrong: " + containerURL.getPathInfo(), "/news/2008/08", containerURL.getPathInfo());
    }
    
    @Test
    public void testActionContainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");
        
        HstContainerURL actionURL = this.urlProvider.parseURL(request, response);
        actionURL.setActionWindowReferenceNamespace("b");
        actionURL.setActionParameter("ap1", "one");
        actionURL.setActionParameter("ap2", "two");
        String actionURLPathInfo = this.urlProvider.toURLString(actionURL, null);
        actionURLPathInfo = actionURLPathInfo.substring("/site/content".length());
        ((MockHttpServletRequest) request).setPathInfo(actionURLPathInfo);
        
        assertNotNull("action window reference namespace is null.", actionURL.getActionWindowReferenceNamespace());
        assertNull("resource window reference namespace is not null.", actionURL.getResourceWindowReferenceNamespace());

        Map<String, String []> actionParams = (Map<String, String []>) actionURL.getActionParameterMap();
        assertNotNull("action param map is null.", actionParams);
        assertFalse("action param map is empty.", actionParams.isEmpty());
        assertEquals("the first action parameter is not 'ap1'.", "one", actionParams.get("ap1")[0]);
        assertEquals("the second action parameter is not 'ap2'.", "two", actionParams.get("ap2")[0]);
    }
    
    @Test
    public void testResourceContainerURL() throws UnsupportedEncodingException, ContainerException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName());
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName());

        ((MockHttpServletRequest) request).setParameter("param1", "value1");
        ((MockHttpServletRequest) request).setParameter("param2", "value2");
        
        HstContainerURL resourceURL = this.urlProvider.parseURL(request, response);
        resourceURL.setResourceWindowReferenceNamespace("b");
        resourceURL.setResourceId("myresource001");
        String resourceURLPathInfo = this.urlProvider.toURLString(resourceURL, null);
        resourceURLPathInfo = resourceURLPathInfo.substring("/site/content".length());
        ((MockHttpServletRequest) request).setPathInfo(resourceURLPathInfo);
        
        assertNull("action window reference namespace is not null.", resourceURL.getActionWindowReferenceNamespace());
        assertNotNull("resource window reference namespace is null.", resourceURL.getResourceWindowReferenceNamespace());
        assertEquals("resource id is wrong.", "myresource001", resourceURL.getResourceId());
        assertEquals("The path info is wrong.", "/news/2008/08", resourceURL.getPathInfo());
    }
}