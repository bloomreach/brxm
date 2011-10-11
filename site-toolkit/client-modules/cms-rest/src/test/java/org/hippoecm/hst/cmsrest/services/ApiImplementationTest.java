/*
*  Copyright 2011 Hippo.
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
package org.hippoecm.hst.cmsrest.services;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.hippoecm.hst.cmsrest.Implements;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Checks that all classes annotated with {@link Implements} 'implement' the API interface referenced in the annotation.
 * See the {@link Implements} annotation for a description of the meaning of 'implements'.
 */
public class ApiImplementationTest {

    private Reflections reflections;

    @Before
    public void init() {
        String myPackageName = getClass().getPackage().getName();
        reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix(myPackageName)))
                .setUrls(ClasspathHelper.forPackage(myPackageName))
                .setScanners(new TypeAnnotationsScanner()));
    }

    @Test
    public void validateApiImplementations() {
        for (Class<?> implementation : reflections.getTypesAnnotatedWith(Implements.class)) {
            Implements implementsApi = implementation.getAnnotation(Implements.class);
            Class<?> api = implementsApi.value();
            validateApiImplementation(api, implementation);
        }
    }

    private void validateApiImplementation(Class<?> apiClass, Class<?> implClass) {
        assertEquals(implClass + " does not have the same @Path annotation as " + apiClass,
                apiClass.getAnnotation(Path.class).value(), implClass.getAnnotation(Path.class).value());

        for (Method apiMethod : apiClass.getMethods()) {
            // does the implementation have a method that matches the API method?
            Method implMethod = getImplementationMethod(apiMethod, implClass);
            if (implMethod == null) {
                fail(implClass + " does not implement " + signature(apiClass, apiMethod));
            }

            // do the generic return types of the implementation and API methods match?
            assertEquals(signature(implClass, implMethod) + " does not have the same return type as " + signature(apiClass, apiMethod),
                    apiMethod.getGenericReturnType(), implMethod.getGenericReturnType());

            // does the implementation method have all the annotations specified in the API method?
            List<Annotation> apiMethodAnnotations = Arrays.asList(apiMethod.getDeclaredAnnotations());
            List<Annotation> implMethodAnnotations = Arrays.asList(implMethod.getDeclaredAnnotations());
            if (!implMethodAnnotations.containsAll(apiMethodAnnotations)) {
                List<Annotation> missingAnnotations = new ArrayList<Annotation>(apiMethodAnnotations);
                missingAnnotations.removeAll(implMethodAnnotations);
                fail("Implementation method [1] misses some annotations [2] of an API method [3].\n"
                        + "[1] " + signature(implClass, implMethod) + "\n"
                        + "[2] " + missingAnnotations + "\n"
                        + "[3] " + signature(apiClass, apiMethod));
            }
        }
    }

    /**
     * Returns the method in the implementation class with the same signature as the given API method,
     * ignoring all parameters annotated with @Context.
     *
     * @param apiMethod the API method
     * @param implementation the implementation class
     * @return the matching method in the implementation, or null if no such method could be found.
     */
    private static Method getImplementationMethod(Method apiMethod, Class<?> implementation) {
        List<Class<?>> apiParamTypes = Arrays.asList(apiMethod.getParameterTypes());

        for (Method implMethod : implementation.getMethods()) {
            if (implMethod.getName().equals(apiMethod.getName())) {
                Class<?>[] implParamTypes= implMethod.getParameterTypes();
                Annotation[][] implParamAnnotations = implMethod.getParameterAnnotations();
                List<Class<?>> noContextImplParamTypes = new LinkedList<Class<?>>();
                for (int i = 0; i < implParamTypes.length; i++) {
                    if (!contains(implParamAnnotations[i], Context.class)) {
                        noContextImplParamTypes.add(implParamTypes[i]);
                    }
                }

                if (noContextImplParamTypes.equals(apiParamTypes)) {
                    return implMethod;
                }
            }
        }
        return null;
    }

    private static boolean contains(Annotation[] haystack, Class<? extends Annotation> needle) {
        for (Annotation item : haystack) {
            if (item.annotationType().equals(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String signature(Class<?> clazz, Method method) {
        StringBuilder b = new StringBuilder();
        b.append(clazz.getCanonicalName());
        b.append('#');
        b.append(method.getName());
        b.append('(');

        String concat = "";
        for (Class<?> paramType : method.getParameterTypes()) {
            b.append(concat);
            b.append(paramType.getCanonicalName());
            concat = ", ";
        }

        b.append(')');

        return b.toString();
    }

}
