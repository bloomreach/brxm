/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.component.support.bean;

import java.awt.Color;
import java.lang.reflect.Method;

import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.proxy.ProxyFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TestBaseHstComponent
 * @version $Id$
 */
public class TestBaseHstComponent {
    
    private MockServletContext servletContext;
    private ComponentConfiguration componentConfig;
    private HstRequestContext requestContext;
    
    @Before
    public void setUp() throws Exception {
        servletContext = new MockServletContext(new ClassPathXmlApplicationContext());

        componentConfig = createNiceMock(ComponentConfiguration.class);
        replay(componentConfig);
        
        requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getParameterInfoProxyFactory()).andReturn(new HstParameterInfoProxyFactoryImpl()).anyTimes();
        
        replay(requestContext);

        ModifiableRequestContextProvider.set(requestContext);
    }

    @After
    public void tearDown() {
        ModifiableRequestContextProvider.set(null);
    }

    @Test
    public void testGetParametersInfo() throws Exception {

        // create a dummy component configuration for test.
        ProxyFactory factory = new ProxyFactory();
        ComponentConfiguration compConfig = (ComponentConfiguration) factory.createInvokerProxy(new Invoker() {
            public Object invoke(Object o, Method m, Object[] args) throws Throwable {
                if (args == null) {
                    return null;
                } else {
                    if ("pagesize".equals(args[0])) {
                        return "10";
                    } else if ("description".equals(args[0])) {
                        return "Test description";
                    } else if ("color".equals(args[0])) {
                        return "#ff0000";
                    }
                }
                return null;
            }
        }, new Class [] { ComponentConfiguration.class });
        
        // now initialize the component with the component configuration
        ANewsArticleComponent component = new ANewsArticleComponent();
        component.init(servletContext, compConfig);
        
        // do testing now..
        MockHstRequest hstRequest = new MockHstRequest();
        // set dummy requestcontext
        hstRequest.setRequestContext(requestContext);
        
        
        ANewsArticleComponentParametersInfo paramsInfo = component.getComponentParametersInfo(hstRequest);
        assertNotNull(paramsInfo);
        assertEquals(10, paramsInfo.getPageSize());
        assertEquals("Test description", paramsInfo.getDescription());
        assertEquals(Color.RED, paramsInfo.getColor());
        
        // should return same object for the same hstRequest.
        assertTrue(paramsInfo == component.getComponentParametersInfo(hstRequest));

        assertEquals("testValue", paramsInfo.getHiddenPropertyInChannelManagerUI());
    }
    
    @Node(jcrType="test:textdocument")
    public static class TextBean extends HippoDocument {
    }
    
    @Node(jcrType="test:comment")
    public static class CommentBean extends HippoDocument {
    }

}
