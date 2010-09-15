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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;

import javax.servlet.ServletContext;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.junit.Before;
import org.junit.Test;

/**
 * TestBaseHstComponent
 * @version $Id$
 */
public class TestBaseHstComponent {
    
    private URL annotationXmlUrl;
    private String annotationClassesLocationFilter;
    
    @Before
    public void setUp() throws Exception {
        String beansAnnotatedClassResourcePath = "/" + getClass().getName().replace('.', '/') + "-beans-annotated-classes.xml";
        annotationXmlUrl = getClass().getResource(beansAnnotatedClassResourcePath);
        assertNotNull("Beans annotatated classes xml resource doesn't exist: " + beansAnnotatedClassResourcePath, annotationXmlUrl);
        
        annotationClassesLocationFilter = "classpath*:" + getClass().getPackage().getName().replace('.', '/') + "/**/*.class";
    }
    
    @Test
    public void testObjectConverterCreationWithXML() throws Exception {
        ServletContext servletContext = createNiceMock(ServletContext.class);
        expect(servletContext.getInitParameter(BaseHstComponent.BEANS_ANNOTATED_CLASSES_CONF_PARAM)).andReturn(BaseHstComponent.DEFAULT_BEANS_ANNOTATED_CLASSES_CONF).anyTimes();
        expect(servletContext.getResource(BaseHstComponent.DEFAULT_BEANS_ANNOTATED_CLASSES_CONF)).andReturn(annotationXmlUrl).anyTimes();
        
        ComponentConfiguration componentConfig = createNiceMock(ComponentConfiguration.class);
        
        replay(servletContext);
        replay(componentConfig);
        
        BaseHstComponent comp = new BaseHstComponent();
        comp.init(servletContext, componentConfig);
        assertNotNull("ObjectConverter is not created during init().", comp.objectConverter);
        
        assertEquals(TextBean.class, comp.objectConverter.getAnnotatedClassFor("test:textdocument"));
        assertEquals(CommentBean.class, comp.objectConverter.getAnnotatedClassFor("test:comment"));
        
        assertEquals("test:textdocument", comp.objectConverter.getPrimaryNodeTypeNameFor(TextBean.class));
        assertEquals("test:comment", comp.objectConverter.getPrimaryNodeTypeNameFor(CommentBean.class));
    }
    
    @Test
    public void testObjectConverterCreationWithScanner() throws Exception {
        ServletContext servletContext = createNiceMock(ServletContext.class);
        expect(servletContext.getInitParameter(BaseHstComponent.BEANS_ANNOTATED_CLASSES_CONF_PARAM)).andReturn(annotationClassesLocationFilter).anyTimes();
        
        ComponentConfiguration componentConfig = createNiceMock(ComponentConfiguration.class);
        
        replay(servletContext);
        replay(componentConfig);
        
        BaseHstComponent comp = new BaseHstComponent();
        comp.init(servletContext, componentConfig);
        assertNotNull("ObjectConverter is not created during init().", comp.objectConverter);
        
        assertEquals(TextBean.class, comp.objectConverter.getAnnotatedClassFor("test:textdocument"));
        assertEquals(CommentBean.class, comp.objectConverter.getAnnotatedClassFor("test:comment"));
        
        assertEquals("test:textdocument", comp.objectConverter.getPrimaryNodeTypeNameFor(TextBean.class));
        assertEquals("test:comment", comp.objectConverter.getPrimaryNodeTypeNameFor(CommentBean.class));
    }
    
    @Node(jcrType="test:textdocument")
    public static class TextBean extends HippoDocument {
    }
    
    @Node(jcrType="test:comment")
    public static class CommentBean extends HippoDocument {
    }
}
