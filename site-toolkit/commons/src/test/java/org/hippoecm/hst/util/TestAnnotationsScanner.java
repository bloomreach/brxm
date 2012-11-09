/*
 *  Copyright 2012 Hippo.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static java.lang.annotation.ElementType.METHOD;
import static org.junit.Assert.assertTrue;

public class TestAnnotationsScanner {


    @Target(METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnno1 {
    }


    @Target(METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnno2 {
    }

    @Target(METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnno3 {
    }

    @Target(METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnno4 {
    }

    @Test
    public void testDirectAnnotations() {
        final Map<String,Set<String>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(DirectAnnotations.class);
        assertTrue(methodAnnotations.size() == 1);
        assertTrue(methodAnnotations.containsKey("annotated"));
        final Set<String> annotations = methodAnnotations.get("annotated");
        assertTrue(annotations.size() == 1);
        assertTrue(annotations.iterator().next().equals(TestAnno1.class.getName()));
    }

    public class DirectAnnotations {
        @TestAnno1
        public void annotated() {
        }
    }

    @Test
    public void testOnlyPublicMethodAnnotations() {
        final Map<String,Set<String>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(DirectPublicAndNonPublicMethodAnnotations.class);
        assertTrue(methodAnnotations.size() == 1);
        assertTrue(methodAnnotations.containsKey("annotated"));
        final Set<String> annotations = methodAnnotations.get("annotated");
        assertTrue(annotations.size() == 1);
        assertTrue(annotations.contains(TestAnno1.class.getName()));
    }


    public class DirectPublicAndNonPublicMethodAnnotations {
        @TestAnno1
        public void annotated() {
        }

        @TestAnno1
        void packagePrivateAnnotated() {
        }

        @TestAnno1
        protected void protectedAnnotated() {
        }

        @TestAnno1
        private void privateAnnotated() {
        }
    }

    @Test
    public void testOverrideAnnotationSkipped() {
        final Map<String,Set<String>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(OverrideMeImpl.class);
        assertTrue(methodAnnotations.size() == 0);

    }

    public interface OverrideMe {
        void annotated();
    }

    public class OverrideMeImpl implements  OverrideMe {
        @Override
        public void annotated() {
        }
    }
    
    @Test
    public void testSuperInterfaceAnnotations() {
        final Map<String,Set<String>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(AnnotatedImpl.class);
        assertTrue(methodAnnotations.size() == 1);
        assertTrue(methodAnnotations.containsKey("annotated"));
        final Set<String> annotations = methodAnnotations.get("annotated");
        assertTrue(annotations.size() == 2);
        assertTrue(annotations.contains(TestAnno1.class.getName()));
        assertTrue(annotations.contains(TestAnno2.class.getName()));
    }

    public interface Annotated {
        @TestAnno1
        void annotated();
    }

    public class AnnotatedImpl implements Annotated {
        @Override
        @TestAnno2
        public void annotated() {

        }
    }

    @Test
    public void testSuperInterfaceAndClassAnnotations() {
        final Map<String,Set<String>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(AnnotatedSubImpl.class);
        assertTrue(methodAnnotations.size() == 1);
        assertTrue(methodAnnotations.containsKey("annotated"));
        final Set<String> annotations = methodAnnotations.get("annotated");
        assertTrue(annotations.size() == 3);
        assertTrue(annotations.contains(TestAnno1.class.getName()));
        assertTrue(annotations.contains(TestAnno2.class.getName()));
        assertTrue(annotations.contains(TestAnno3.class.getName()));
    }

    public class AnnotatedSubImpl extends AnnotatedImpl {
        @TestAnno3
        public void annotated() {

        }
    }


    @Test
    public void testMethodOverloadingAnnotationsAreCombined() {
        final Map<String,Set<String>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(ClassWithMethodOverloading.class);
        assertTrue(methodAnnotations.size() == 1);
        assertTrue(methodAnnotations.containsKey("annotated"));
        final Set<String> annotations = methodAnnotations.get("annotated");
        assertTrue(annotations.size() == 3);
        assertTrue(annotations.contains(TestAnno1.class.getName()));
        assertTrue(annotations.contains(TestAnno2.class.getName()));
        assertTrue(annotations.contains(TestAnno3.class.getName()));
    }

    public class ClassWithMethodOverloading {
        @TestAnno1
        public void annotated() {}
        @TestAnno2
        public String annotated(String foo) {return null;}
        @TestAnno3
        public void annotated(boolean foo) {}
    }

    @Test
    public void testMethodOverloadingAndSuperClassAnnotationsAreCombined() {
        final Map<String,Set<String>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(ClassWithMethodOverloadingAndSuperClass.class);
        assertTrue(methodAnnotations.size() == 1);
        assertTrue(methodAnnotations.containsKey("annotated"));
        final Set<String> annotations = methodAnnotations.get("annotated");
        assertTrue(annotations.size() == 4);
        assertTrue(annotations.contains(TestAnno1.class.getName()));
        assertTrue(annotations.contains(TestAnno2.class.getName()));
        assertTrue(annotations.contains(TestAnno3.class.getName()));
        assertTrue(annotations.contains(TestAnno4.class.getName()));
    }

    public class ClassWithMethodOverloadingAndSuperClass extends ClassWithMethodOverloading {
        @TestAnno4
        public int annotated(int foo) {return 0;}
    }
    
    @Test
    public void testClassesScannedAreCached() {
        final List<AnnotationsScanner.MethodAnnotations> firstScanned = AnnotationsScanner.getMethodAnnotationsList(DirectAnnotations.class);
        final List<AnnotationsScanner.MethodAnnotations> secondScanned = AnnotationsScanner.getMethodAnnotationsList(DirectAnnotations.class);
        assertTrue(firstScanned == secondScanned);
    }



}
