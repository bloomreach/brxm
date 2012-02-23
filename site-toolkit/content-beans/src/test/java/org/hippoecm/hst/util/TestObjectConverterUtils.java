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
package org.hippoecm.hst.util;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.junit.Before;
import org.junit.Test;

/**
 * TestObjectConverterUtils
 * @version $Id$
 */
public class TestObjectConverterUtils {
    
    private URL annotationXmlUrl;
    
    @Before
    public void setUp() throws Exception {
        String beansAnnotatedClassResourcePath = "/" + getClass().getName().replace('.', '/') + "-beans-annotated-classes.xml";
        annotationXmlUrl = getClass().getResource(beansAnnotatedClassResourcePath);
        assertNotNull("Beans annotatated classes xml resource doesn't exist: " + beansAnnotatedClassResourcePath, annotationXmlUrl);
    }
    
    @Test
    public void testObjectConverterCreationWithURL() throws Exception {
        Collection<Class<? extends HippoBean>> annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(annotationXmlUrl);
        ObjectConverter objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        
        assertEquals(TextBean.class, objectConverter.getAnnotatedClassFor("test:textdocument"));
        assertEquals(CommentBean.class, objectConverter.getAnnotatedClassFor("test:comment"));
        
        assertEquals("test:textdocument", objectConverter.getPrimaryNodeTypeNameFor(TextBean.class));
        assertEquals("test:comment", objectConverter.getPrimaryNodeTypeNameFor(CommentBean.class));
    }
    
    @Test
    public void testObjectConverterCreationWithClasspathResourceScanner() throws Exception {
        String locationPattern = "classpath*:" + getClass().getPackage().getName().replace('.', '/') + "/**/*.class";
        
        Set<String> expectedAannotatedClassNames = new HashSet<String>();
        expectedAannotatedClassNames.add(DocumentInterface.class.getName());
        expectedAannotatedClassNames.add(AbstractBean.class.getName());
        expectedAannotatedClassNames.add(TextBean.class.getName());
        expectedAannotatedClassNames.add(CommentBean.class.getName());
        expectedAannotatedClassNames.add(PackageBean.class.getName());
        expectedAannotatedClassNames.add(PrivateBean.class.getName());
        ClasspathResourceScanner resourceScanner = createNiceMock(ClasspathResourceScanner.class);
        expect(resourceScanner.scanClassNamesAnnotatedBy(Node.class, false, locationPattern)).andReturn(expectedAannotatedClassNames).anyTimes();
        replay(resourceScanner);
        
        Collection<Class<? extends HippoBean>> annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(resourceScanner, locationPattern);
        ObjectConverter objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        
        assertNull(objectConverter.getAnnotatedClassFor("test:documentinterface"));
        assertNull(objectConverter.getAnnotatedClassFor("test:abstractdocument"));
        assertEquals(TextBean.class, objectConverter.getAnnotatedClassFor("test:textdocument"));
        assertEquals(CommentBean.class, objectConverter.getAnnotatedClassFor("test:comment"));
        assertNull(objectConverter.getAnnotatedClassFor("test:packagedocument"));
        assertNull(objectConverter.getAnnotatedClassFor("test:privatedocument"));
        
        assertNull("test:documentinterface", objectConverter.getPrimaryNodeTypeNameFor(DocumentInterface.class));
        assertNull("test:abstractdocument", objectConverter.getPrimaryNodeTypeNameFor(AbstractBean.class));
        assertEquals("test:textdocument", objectConverter.getPrimaryNodeTypeNameFor(TextBean.class));
        assertEquals("test:comment", objectConverter.getPrimaryNodeTypeNameFor(CommentBean.class));
        assertNull("test:packagedocument", objectConverter.getPrimaryNodeTypeNameFor(PackageBean.class));
        assertNull("test:privatedocument", objectConverter.getPrimaryNodeTypeNameFor(PrivateBean.class));
    }

    @Node(jcrType="test:documentinterface")
    public interface DocumentInterface extends HippoBean {
    }

    @Node(jcrType="test:abstractdocument")
    public static abstract class AbstractBean extends HippoDocument {
    }

    @Node(jcrType="test:textdocument")
    public static class TextBean extends AbstractBean {
    }

    @Node(jcrType="test:comment")
    public static class CommentBean extends AbstractBean {
    }

    @Node(jcrType="test:packagedocument")
    static class PackageBean extends AbstractBean {
    }

    @Node(jcrType="test:privatedocument")
    private static class PrivateBean extends AbstractBean {
    }
}
