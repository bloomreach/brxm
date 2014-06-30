/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.utils.annotations;

import java.lang.reflect.Method;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class AnnotationUtilsTest {

    private static final Logger log = LoggerFactory.getLogger(AnnotationUtilsTest.class);
    public static final int TOTAL_METHODS = 2;


    @Test
    public void testXmlAdaptorAnnotation() throws Exception {
        final String javaFile = GlobalUtils.readStreamAsText(getClass().getResourceAsStream("/AnnotationTestClass.txt"));
        assertNotNull("Expected to find  /AnnotationTestClass.txt file", javaFile);
        final String annotationName = "HippoHtmlAdapter";
        final String importPath = "org.onehippo.cms7.essentials.components.rest.adapters";
        String annotated = AnnotationUtils.addXmlAdaptorAnnotation(javaFile, HippoHtml.class, new AnnotationUtils.AdapterWrapper(importPath, annotationName));
        int nrOfItems = StringUtils.countMatches(annotated, annotationName);
        assertEquals(1, nrOfItems);
        annotated = AnnotationUtils.addXmlAdaptorAnnotation(javaFile, HippoHtml.class, new AnnotationUtils.AdapterWrapper(importPath, annotationName));
        nrOfItems = StringUtils.countMatches(annotated, annotationName);
        assertEquals(1, nrOfItems);
    }

    @Test
    public void testXmlRootAnnotation() throws Exception {
        final String javaFile = GlobalUtils.readStreamAsText(getClass().getResourceAsStream("/AnnotationTestClass.txt"));
        assertNotNull("Expected to find  /AnnotationTestClass.txt file", javaFile);
        String annotated = AnnotationUtils.addXmlRootAnnotation(javaFile, "testdocument");
        int nrOfItems = StringUtils.countMatches(annotated, XmlRootElement.class.getName());
        assertEquals(1, nrOfItems);
        annotated = AnnotationUtils.addXmlRootAnnotation(javaFile, "testdocument");
        nrOfItems = StringUtils.countMatches(annotated, XmlRootElement.class.getName());
        assertEquals(1, nrOfItems);
        nrOfItems = StringUtils.countMatches(annotated, "XmlRootElement");
        // import and annotation
        assertEquals(2, nrOfItems);
    }


    @Test
    public void testMethodAnnotation() throws Exception {
        final String javaFile = GlobalUtils.readStreamAsText(getClass().getResourceAsStream("/AnnotationTestClass.txt"));
        assertNotNull("Expected to find  /AnnotationTestClass.txt file", javaFile);
        String annotated = AnnotationUtils.addXmlElementAnnotation(javaFile);
        int nrOfItems = StringUtils.countMatches(annotated, XmlElement.class.getName());
        assertEquals(1, nrOfItems);
        nrOfItems = StringUtils.countMatches(annotated, "@XmlElement");
        assertEquals(8, nrOfItems);
        annotated = AnnotationUtils.addXmlElementAnnotation(annotated);
        nrOfItems = StringUtils.countMatches(annotated, XmlElement.class.getName());
        assertEquals(1, nrOfItems);
        nrOfItems = StringUtils.countMatches(annotated, "@XmlElement");
        assertEquals(8, nrOfItems);
    }

    @Test
    public void testAddClassAnnotation() throws Exception {
        final String javaFile = GlobalUtils.readStreamAsText(getClass().getResourceAsStream("/AnnotationTestClass.txt"));
        assertNotNull("Expected to find  /AnnotationTestClass.txt file", javaFile);
        String annotated = AnnotationUtils.addXmlAccessNoneAnnotation(javaFile);
        annotated = AnnotationUtils.addXmlAccessNoneAnnotation(annotated);
        log.info("annotated {}", annotated);
        int nrOfItems = StringUtils.countMatches(annotated, XmlAccessType.class.getName());
        assertEquals(1, nrOfItems);
        nrOfItems = StringUtils.countMatches(annotated, XmlAccessType.class.getSimpleName());
        assertEquals(2, nrOfItems);

    }

    public void addClass(final AST ast, final SingleMemberAnnotation xmlAccessAnnotation) {
        final TypeLiteral typeLiteral = ast.newTypeLiteral();
        typeLiteral.setType(ast.newSimpleType(ast.newName("XmlAccessType.NONE")));
        xmlAccessAnnotation.setValue(typeLiteral);
    }

    @Test
    public void testFindClass() throws Exception {
        Class<Object> clazz = AnnotationUtils.findClass(AnnotationUtilsTest.class.getName());
        assertTrue(clazz != null);
    }

    @Test
    public void testGetClassMethods() throws Exception {
        Collection<Method> methods = AnnotationUtils.getMethods(AnnotationUtilsTest.class);
        //NOTE: total nr of methods depends on java version, so we at least can epect ours:
        assertTrue("Expected at least" + TOTAL_METHODS, methods.size() > TOTAL_METHODS);
    }
}
