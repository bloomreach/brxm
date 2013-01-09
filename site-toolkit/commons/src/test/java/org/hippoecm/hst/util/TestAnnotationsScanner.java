/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static java.lang.annotation.ElementType.METHOD;
import static org.junit.Assert.assertFalse;
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
    public void testDirectAnnotations() throws NoSuchMethodException {
        final Map<Method, Set<Annotation>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(DirectAnnotations.class);

        assertTrue(methodAnnotations.size() == 1);
        Method method = DirectAnnotations.class.getMethod("annotated");
        assertTrue(methodAnnotations.containsKey(method));
        final Set<Annotation> annotations = methodAnnotations.get(method);
        assertTrue(annotations.size() == 1);
        final Annotation annotation = annotations.iterator().next();

        assertTrue(annotation.annotationType() == TestAnno1.class);
    }

    @Test
    public void testClassesScannedAreCached() {
        final List<AnnotationsScanner.MethodAnnotations> firstScanned = AnnotationsScanner.getMethodAnnotationsList(DirectAnnotations.class);
        final List<AnnotationsScanner.MethodAnnotations> secondScanned = AnnotationsScanner.getMethodAnnotationsList(DirectAnnotations.class);
        assertTrue(firstScanned == secondScanned);
    }


    public class DirectAnnotations {
        @TestAnno1
        public void annotated() {
        }
    }

    @Test
    public void testOnlyPublicMethodAnnotations() throws NoSuchMethodException {
        final Map<Method, Set<Annotation>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(DirectPublicAndNonPublicMethodAnnotations.class);
        assertTrue(methodAnnotations.size() == 1);
        Method method = DirectPublicAndNonPublicMethodAnnotations.class.getMethod("annotated");
        assertTrue(methodAnnotations.containsKey(method));
        final Set<Annotation> annotations = methodAnnotations.get(method);
        assertTrue(annotations.size() == 1);
        final Annotation annotation = annotations.iterator().next();
        assertTrue(annotation.annotationType() == TestAnno1.class);
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
        final Map<Method, Set<Annotation>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(OverrideMeImpl.class);
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
    public void testSuperInterfaceAnnotations() throws NoSuchMethodException {
        final Map<Method, Set<Annotation>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(AnnotatedImpl.class);
        assertTrue(methodAnnotations.size() == 1);
        Method method = AnnotatedImpl.class.getMethod("annotated");
        assertTrue(methodAnnotations.containsKey(method));
        final Set<Annotation> annotations = methodAnnotations.get(method);
        assertTrue(annotations.size() == 2);
        final Set<Class<?>> annotationClasses = getAnnotationClassesSet(annotations);
        assertTrue(annotationClasses.contains(TestAnno1.class));
        assertTrue(annotationClasses.contains(TestAnno2.class));
    }
    
    @Test 
    public void testMethodAnnotationsDoesNotContainMethodInstanceFromSuper() throws NoSuchMethodException {
        final Map<Method, Set<Annotation>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(AnnotatedImpl.class);
        assertTrue(methodAnnotations.size() == 1);
        // FETCH METHOD FROM INTERFACE 'Annotated': this is a different instance than the method from 'AnnotatedImpl'
        Method method = Annotated.class.getMethod("annotated");
        assertFalse(methodAnnotations.containsKey(method));
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
    public void testSuperInterfaceAndClassAnnotations() throws NoSuchMethodException {
        final Map<Method, Set<Annotation>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(AnnotatedSubImpl.class);
        assertTrue(methodAnnotations.size() == 1);
        Method method = AnnotatedSubImpl.class.getMethod("annotated");
        assertTrue(methodAnnotations.containsKey(method));
        final Set<Annotation> annotations = methodAnnotations.get(method);
        assertTrue(annotations.size() == 3);
        final Set<Class<?>> annotationClasses = getAnnotationClassesSet(annotations);
        assertTrue(annotationClasses.contains(TestAnno1.class));
        assertTrue(annotationClasses.contains(TestAnno2.class));
        assertTrue(annotationClasses.contains(TestAnno3.class));
    }

    public class AnnotatedSubImpl extends AnnotatedImpl {
        @TestAnno3
        public void annotated() {

        }
    }


    @Test
    public void testOverloadMethodAnnotationsAreNOTCombined() throws NoSuchMethodException {
        final Map<Method, Set<Annotation>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(ClassWithMethodOverloading.class);
        assertTrue(methodAnnotations.size() == 3);
        {
            Method method1 = ClassWithMethodOverloading.class.getMethod("annotated");
            assertTrue(methodAnnotations.containsKey(method1));
            final Set<Annotation> annotations1 = methodAnnotations.get(method1);
            assertTrue(annotations1.size() == 1);
            assertTrue(annotations1.iterator().next().annotationType() == TestAnno1.class);
        }
        {
            Method method2 = ClassWithMethodOverloading.class.getMethod("annotated", String.class);
            assertTrue(methodAnnotations.containsKey(method2));
            final Set<Annotation> annotations2 = methodAnnotations.get(method2);
            assertTrue(annotations2.size() == 1);
            assertTrue(annotations2.iterator().next().annotationType() == TestAnno2.class);
        }
        {
            Method method3 = ClassWithMethodOverloading.class.getMethod("annotated", boolean.class);
            assertTrue(methodAnnotations.containsKey(method3));
            final Set<Annotation> annotations3 = methodAnnotations.get(method3);
            assertTrue(annotations3.size() == 1);
            assertTrue(annotations3.iterator().next().annotationType() == TestAnno3.class);
        }
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
    public void testOverloadMethodsAndSuperClassAnnotationsAreNOTCombined() throws NoSuchMethodException {
        final Map<Method, Set<Annotation>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(ClassWithMethodOverloadingAndSuperClass.class);
        assertTrue(methodAnnotations.size() == 4);
        Method method = ClassWithMethodOverloadingAndSuperClass.class.getMethod("annotated");
        assertTrue(methodAnnotations.containsKey(method));
        final Set<Annotation> annotations = methodAnnotations.get(method);
        assertTrue(annotations.size() == 1);
        assertTrue(annotations.iterator().next().annotationType() == TestAnno1.class);
    }

    public class ClassWithMethodOverloadingAndSuperClass extends ClassWithMethodOverloading {
        @TestAnno4
        public int annotated(int foo) {return 0;}
    }



    private Set<Class<?>> getAnnotationClassesSet(final Set<Annotation> annotations) {
        Set<Class<?>> annotationClassesSet = new HashSet<Class<?>>();
        for (Annotation annotation : annotations) {
            annotationClassesSet.add(annotation.annotationType());
        }
        return annotationClassesSet;
    }


}
