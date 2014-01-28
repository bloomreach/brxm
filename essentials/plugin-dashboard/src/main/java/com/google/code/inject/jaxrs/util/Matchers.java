/*
 * Copyright 2012 Jakub Bocheński (kuba.bochenski@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.inject.jaxrs.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.ws.rs.Path;

import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.apache.cxf.jaxrs.utils.AnnotationUtils.getAnnotatedMethod;
import static org.apache.cxf.jaxrs.utils.AnnotationUtils.getHttpMethodValue;
import static org.apache.cxf.jaxrs.utils.AnnotationUtils.getMethodAnnotation;

public final class Matchers {

    private Matchers() {
    }

    public static Matcher<Method> resourceMethod() {

        return new AbstractMatcher<Method>() {
            @Override
            public boolean matches(Method m) {
                return isResourceMethod(m);

            }
        };
    }

    public static Matcher<Method> resourceMethod(
            final Class<? extends Annotation> annotation) {

        return new AbstractMatcher<Method>() {
            @Override
            public boolean matches(Method m) {
                return isResourceMethod(m)
                        && getAnnotatedMethod(m).getAnnotation(annotation) != null;

            }
        };
    }

    private static boolean isResourceMethod(Method m) {
        final Method annotatedMethod = getAnnotatedMethod(m);

        final int mod = m.getModifiers();
        if (!isPublic(mod) || isStatic(mod)) {
            return false;
        }

        return getHttpMethodValue(annotatedMethod) != null
                || getMethodAnnotation(annotatedMethod, Path.class) != null;
    }
}