/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerConfigurationImpl;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstComponentWindowImpl;
import org.hippoecm.hst.core.container.HstContainerURLImpl;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.proxy.ProxyFactory;
import org.hippoecm.hst.site.request.HstRequestContextImpl;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class HstRequestIT extends AbstractSpringTestCase {
    
    protected HttpServletRequest servletRequest;
    protected HstMutableRequestContext requestContext;

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
        // Sets request context
        this.servletRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, this.requestContext);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("name", "news");
        props.put("referenceName", "news");
        HstComponentWindow rootWindow = new HstComponentWindowImpl(createHstComponentConfigurationProxy(props), null, null, null, null, "", null);
        
        props = new HashMap<String, Object>();
        props.put("name", "head");
        props.put("referenceName", "h");
        HstComponentWindow headWindow = new HstComponentWindowImpl(createHstComponentConfigurationProxy(props), null, null, null, null, "h", null);
        
        props = new HashMap<String, Object>();
        props.put("body", "body");
        props.put("referenceName", "b");
        HstComponentWindow bodyWindow = new HstComponentWindowImpl(createHstComponentConfigurationProxy(props), null, null, null, null, "b", null);
        
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
        
        // Remove an attribute from bodyWindow
        hstRequestForBodyWindow.removeAttribute("name");
        assertNull("The name attribute of body window request is still available! name: " + hstRequestForBodyWindow.getAttribute("name"), hstRequestForBodyWindow.getAttribute("name"));
    }
    
    @Test
    public void testLocales() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertNotNull(request.getLocale());
        List<?> locales = EnumerationUtils.toList(request.getLocales());
        assertFalse(locales.isEmpty());
        
        HstRequestContextImpl rc = new HstRequestContextImpl(null);
        HstURLFactory urlFactory = getComponent(HstURLFactory.class.getName());
        rc.setURLFactory(urlFactory);
        
        HstRequest hstRequest = new HstRequestImpl(request, rc, null, HstRequest.RENDER_PHASE);
        
        assertEquals(request.getLocale(), hstRequest.getLocale());
        List<?> hstLocales = EnumerationUtils.toList(hstRequest.getLocales());
        assertNotNull(hstLocales);
        assertEquals(locales.size(), hstLocales.size());
        
        for (Object loc : locales) {
            assertTrue(hstLocales.contains(loc));
        }
        
        rc.setPreferredLocale(Locale.KOREAN);
        List<Locale> newLocales = new ArrayList<Locale>();
        newLocales.add(Locale.KOREAN);
        newLocales.addAll((List<Locale>) locales);
        rc.setLocales(newLocales);
        
        assertEquals(Locale.KOREAN, hstRequest.getLocale());
        assertEquals(Locale.KOREAN, hstRequest.getLocales().nextElement());
    }

    private static HstComponentConfiguration createHstComponentConfigurationProxy(Map<String, Object> propsInput) {
        ProxyFactory proxyFactory = new ProxyFactory();
        final Map<String, Object> props = propsInput;
        
        Invoker invoker = new Invoker() {
            public Object invoke(Object object, Method method, Object [] args) throws Throwable {
                String methodName = method.getName();
                if (methodName.startsWith("get")) {
                    String propName = methodName.substring(3);
                    propName = propName.substring(0, 1).toLowerCase() + propName.substring(1);
                    return props.get(propName);
                }
                return null;
            }
        };
        
        return (HstComponentConfiguration) proxyFactory.createInvokerProxy(Thread.currentThread().getContextClassLoader(), invoker, new Class [] { HstComponentConfiguration.class });
    }
}
