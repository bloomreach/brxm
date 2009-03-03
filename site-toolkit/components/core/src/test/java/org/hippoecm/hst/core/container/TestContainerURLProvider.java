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

import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestContainerURLProvider extends AbstractSpringTestCase {

    protected HstContainerURLProvider urlProvider;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.urlProvider = getComponent(HstContainerURLProvider.class.getName());
    }
    
    @Test
    public void testActionContainerURL() throws UnsupportedEncodingException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName() + ".action");
        
        ((MockHttpServletRequest) request).addParameter("param1", "value1");
        ((MockHttpServletRequest) request).addParameter("param2", "value2");
        
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName() + ".action");

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response);
        
        assertNotNull("action window reference namespace is null.", containerURL.getActionWindowReferenceNamespace());
        assertNull("resource window reference namespace is not null.", containerURL.getResourceWindowReferenceNamespace());

        Map<String, String []> actionParams = (Map<String, String []>) containerURL.getActionParameterMap();
        assertNotNull("action param map is null.", actionParams);
        assertFalse("action param map is empty.", actionParams.isEmpty());
        assertEquals("the first action parameter is not 'ap1'.", "one", actionParams.get("ap1")[0]);
        assertEquals("the second action parameter is not 'ap2'.", "two", actionParams.get("ap2")[0]);
        
        assertEquals("The path info is wrong.", "/news/2008/08", containerURL.getPathInfo());
        
        Map<String, String []> params = (Map<String, String []>) containerURL.getParameterMap();
        assertNotNull("param map is null.", params);
        assertFalse("param map is empty.", params.isEmpty());
        assertEquals("the first parameter is not 'value1'.", "value1", params.get("param1")[0]);
        assertEquals("the second parameter is not 'value2'.", "value2", params.get("param2")[0]);
        
        String urlString = this.urlProvider.toURLString(containerURL);
        assertTrue("The url is wrong: " + urlString, urlString.startsWith("/site/content/_hstact_:news_p_h_t/2/"));
        assertTrue("The url is wrong: " + urlString, urlString.contains("/ap1/one/"));
        assertTrue("The url is wrong: " + urlString, urlString.contains("/ap2/two/"));
        assertTrue("The url is wrong: " + urlString, urlString.contains("param1=value1"));
        assertTrue("The url is wrong: " + urlString, urlString.contains("param2=value2"));
    }
    
    @Test
    public void testResourceContainerURL() throws UnsupportedEncodingException {
        HttpServletRequest request = getComponent(HttpServletRequest.class.getName() + ".resource");
        
        ((MockHttpServletRequest) request).addParameter("param1", "value1");
        ((MockHttpServletRequest) request).addParameter("param2", "value2");
        
        HttpServletResponse response = getComponent(HttpServletResponse.class.getName() + ".resource");

        HstContainerURL containerURL = this.urlProvider.parseURL(request, response);
        
        assertNull("action window reference namespace is not null.", containerURL.getActionWindowReferenceNamespace());
        assertNotNull("resource window reference namespace is null.", containerURL.getResourceWindowReferenceNamespace());
        
        assertEquals("resource id is wrong.", "myresource001", containerURL.getResourceId());

        assertEquals("The path info is wrong.", "/news/2008/08", containerURL.getPathInfo());
        
        Map<String, String []> params = (Map<String, String []>) containerURL.getParameterMap();
        assertNotNull("param map is null.", params);
        assertFalse("param map is empty.", params.isEmpty());
        assertEquals("the first parameter is not 'value1'.", "value1", params.get("param1")[0]);
        assertEquals("the second parameter is not 'value2'.", "value2", params.get("param2")[0]);
        
        String urlString = this.urlProvider.toURLString(containerURL);
        assertTrue("The url is wrong: " + urlString, urlString.startsWith("/site/content/_hstres_:news_p_h_t:myresource001/news/2008/08?"));
        assertTrue("The url is wrong: " + urlString, urlString.contains("param1=value1"));
        assertTrue("The url is wrong: " + urlString, urlString.contains("param2=value2"));
    }
}