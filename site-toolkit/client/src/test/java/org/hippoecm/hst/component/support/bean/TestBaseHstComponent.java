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
package org.hippoecm.hst.component.support.bean;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.mock.MockHstRequest;
import org.hippoecm.hst.proxy.ProxyFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext;

/**
 * TestBaseHstComponent
 * @version $Id$
 */
public class TestBaseHstComponent {
    
    private MockServletContext servletContext;
    private String beansAnnotatedClassResourcePath;
    private String annotationClassesLocationFilter;
    private ComponentConfiguration componentConfig;
    private HstRequestContext requestContext;
    
    @Before
    public void setUp() throws Exception {
        servletContext = new MockServletContext(new ClassPathXmlApplicationContext());
        beansAnnotatedClassResourcePath = "/" + getClass().getName().replace('.', '/') + "-beans-annotated-classes.xml";
        assertNotNull(servletContext.getResource(beansAnnotatedClassResourcePath));
        
        annotationClassesLocationFilter = "classpath*:" + getClass().getPackage().getName().replace('.', '/') + "/**/*.class";
        
        componentConfig = createNiceMock(ComponentConfiguration.class);
        replay(componentConfig);
        
        requestContext = createNiceMock(HstRequestContext.class);
        replay(requestContext);
    }
    
    @Test
    public void testObjectConverterCreationWithXML() throws Exception {
        servletContext.addInitParameter(BaseHstComponent.BEANS_ANNOTATED_CLASSES_CONF_PARAM, beansAnnotatedClassResourcePath);
        
        BaseHstComponent comp = new BaseHstComponent();
        comp.init(servletContext, componentConfig);
        assertNotNull("ObjectConverter is not created during init().", comp.objectConverter);
        
        assertEquals(TextBean.class, comp.objectConverter.getAnnotatedClassFor("test:textdocument"));
        assertEquals(CommentBean.class, comp.objectConverter.getAnnotatedClassFor("test:comment"));
        assertNull(comp.objectConverter.getAnnotatedClassFor("test:bookmark"));
        
        assertEquals("test:textdocument", comp.objectConverter.getPrimaryNodeTypeNameFor(TextBean.class));
        assertEquals("test:comment", comp.objectConverter.getPrimaryNodeTypeNameFor(CommentBean.class));
        assertNull(comp.objectConverter.getPrimaryNodeTypeNameFor(BookmarkBean.class));
    }
    
    @Test
    public void testObjectConverterCreationWithXMLAndLocalAnnotatedClasses() throws Exception {
        servletContext.addInitParameter(BaseHstComponent.BEANS_ANNOTATED_CLASSES_CONF_PARAM, beansAnnotatedClassResourcePath);
        
        BaseHstComponent comp1 = new BaseHstComponent();
        comp1.init(servletContext, componentConfig);
        assertNotNull("ObjectConverter is not created during init().", comp1.objectConverter);
        
        assertNull(comp1.objectConverter.getAnnotatedClassFor("test:bookmark"));
        assertNull(comp1.objectConverter.getPrimaryNodeTypeNameFor(BookmarkBean.class));
        
        BaseHstComponent comp2 = new BaseHstComponent();
        comp2.init(servletContext, componentConfig);
        assertTrue("The ObjectConverter should be shared between components which don't have locally annotated beans.", comp1.objectConverter == comp2.objectConverter);
        
        BaseHstComponent comp3 = new BaseHstComponent() {
            @Override
            protected List<Class<? extends HippoBean>> getLocalAnnotatedClasses() {
                List<Class<? extends HippoBean>> list = new ArrayList<Class<? extends HippoBean>>();
                list.add(BookmarkBean.class);
                return list;
            }
        };
        
        comp3.init(servletContext, componentConfig);
        assertFalse("The ObjectConverter should be different from the globally shared one.", comp1.objectConverter == comp3.objectConverter);
        
        assertEquals(BookmarkBean.class, comp3.objectConverter.getAnnotatedClassFor("test:bookmark"));
        assertEquals("test:bookmark", comp3.objectConverter.getPrimaryNodeTypeNameFor(BookmarkBean.class));
    }
    
    @Test
    public void testObjectConverterCreationWithScanner() throws Exception {
        servletContext.addInitParameter(BaseHstComponent.BEANS_ANNOTATED_CLASSES_CONF_PARAM, annotationClassesLocationFilter);
        
        BaseHstComponent comp = new BaseHstComponent();
        comp.init(servletContext, componentConfig);
        assertNotNull("ObjectConverter is not created during init().", comp.objectConverter);
        
        assertEquals(TextBean.class, comp.objectConverter.getAnnotatedClassFor("test:textdocument"));
        assertEquals(CommentBean.class, comp.objectConverter.getAnnotatedClassFor("test:comment"));
        assertEquals(BookmarkBean.class, comp.objectConverter.getAnnotatedClassFor("test:bookmark"));
        
        assertEquals("test:textdocument", comp.objectConverter.getPrimaryNodeTypeNameFor(TextBean.class));
        assertEquals("test:comment", comp.objectConverter.getPrimaryNodeTypeNameFor(CommentBean.class));
        assertEquals("test:bookmark", comp.objectConverter.getPrimaryNodeTypeNameFor(BookmarkBean.class));
    }
    
    @Test
    public void testGetParametersInfo() throws Exception {
        // set dummy beans-annotated-classes configuration
        servletContext.addInitParameter(BaseHstComponent.BEANS_ANNOTATED_CLASSES_CONF_PARAM, "classpath*:org/hippoecm/hst/component/support/beans/*.class");
        
        // create a dummy component configuration for test.
        ProxyFactory factory = new ProxyFactory();
        ComponentConfiguration compConfig = (ComponentConfiguration) factory.createInvokerProxy(new Invoker() {
            public Object invoke(Object o, Method m, Object[] args) throws Throwable {
                if ("pagesize".equals(args[0])) {
                    return "10";
                } else if ("description".equals(args[0])) {
                    return "Test description";
                } else if ("color".equals(args[0])) {
                    return "#ff0000";
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
        
        ANewsArticleComponentParametersInfo paramsInfo = component.getParametersInfo(hstRequest);
        assertNotNull(paramsInfo);
        assertEquals(10, paramsInfo.getPageSize());
        assertEquals("Test description", paramsInfo.getDescription());
        assertEquals(Color.RED, paramsInfo.getColor());
        
        // should return same object for the same hstRequest.
        assertTrue(paramsInfo == component.getParametersInfo(hstRequest));
    }
    
    @Node(jcrType="test:textdocument")
    public static class TextBean extends HippoDocument {
    }
    
    @Node(jcrType="test:comment")
    public static class CommentBean extends HippoDocument {
    }
    
    @Node(jcrType="test:bookmark")
    public static class BookmarkBean extends HippoDocument {
    }
}
