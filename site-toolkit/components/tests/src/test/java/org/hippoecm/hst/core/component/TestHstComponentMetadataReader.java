/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.component;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hippoecm.hst.content.annotations.Persistable;
import org.junit.Before;
import org.junit.Test;

import static java.lang.annotation.ElementType.METHOD;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TestHstComponentMetadataReader
 */
public class TestHstComponentMetadataReader {

    private HstComponentMetadata metadataDetailComponent;
    private HstComponentMetadata metadataDetailComponentImplementingInterface;
    private HstComponentMetadata metadataDetailComponentOverloaded;
    
    @Before
    public void setup(){
        metadataDetailComponent = HstComponentMetadataReader.getHstComponentMetadata(DetailComponent.class);
        metadataDetailComponentImplementingInterface = HstComponentMetadataReader.getHstComponentMetadata(DetailComponentImplementingInterface.class);
        metadataDetailComponentOverloaded = HstComponentMetadataReader.getHstComponentMetadata(DetailComponentOverloaded.class);
    }


    @Test
    public void testMetadataDeclaredAnnotationReading() throws Exception {
        assertNotNull(metadataDetailComponent);
        assertFalse(metadataDetailComponent.hasMethodAnnotatedBy(Persistable.class.getName(), "doBeforeRender"));
        assertTrue(metadataDetailComponent.hasMethodAnnotatedBy(Persistable.class.getName(), "doAction"));
    }

    @Test
    public void testMetadataSuperClassAnnotationReading() throws Exception {
        assertTrue(metadataDetailComponent.hasMethodAnnotatedBy(Persistable.class.getName(), "doBeforeServeResource"));
    }

    @Test
    public void testMetadataSuperInterfaceAnnotationReading() throws Exception {
        // from BaseComponent
        assertTrue(metadataDetailComponentImplementingInterface.hasMethodAnnotatedBy(Persistable.class.getName(), "doBeforeServeResource"));
        // from super interface
        assertTrue(metadataDetailComponentImplementingInterface.hasMethodAnnotatedBy(Persistable.class.getName(), "doAction"));
    }

    @Test
    public void testMetadataSuperInterfaceTwoAnnotations() throws Exception {
        // from super interface
        assertTrue(metadataDetailComponentImplementingInterface.hasMethodAnnotatedBy(Persistable.class.getName(), "doAction"));
        assertTrue(metadataDetailComponentImplementingInterface.hasMethodAnnotatedBy(TestAnno1.class.getName(), "doAction"));
    }

    @Test
    public void testMetadataOverloadedMethodAnnotationsAreCombined() throws Exception {
        assertTrue(metadataDetailComponentOverloaded.hasMethodAnnotatedBy(Persistable.class.getName(), "doAction"));
        assertTrue(metadataDetailComponentOverloaded.hasMethodAnnotatedBy(TestAnno1.class.getName(), "doAction"));
        assertTrue(metadataDetailComponentOverloaded.hasMethodAnnotatedBy(TestAnno2.class.getName(), "doAction"));
    }

    public static class BaseComponent extends GenericHstComponent {
        @Persistable
        public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        }
    }

    public static class DetailComponent extends BaseComponent {
        @Persistable
        public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        }
    }

    public static class DetailComponentImplementingInterface extends BaseComponent implements InterfaceWithPersistableAnno {
        public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        }
    }

    public static class DetailComponentOverloaded extends BaseComponent implements InterfaceWithPersistableAnno {
        public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        }

        @TestAnno1
        public void doAction() {
        }

        @TestAnno2()
        public void doAction(boolean b) {
        }

    }

    public static interface InterfaceWithPersistableAnno {
        @Persistable
        @TestAnno1()
        public void doAction(HstRequest request, HstResponse response);
    }

    @Target(METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnno1 {
    }


    @Target(METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnno2 {
    }

}
