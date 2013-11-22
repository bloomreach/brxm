/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class AnnotationUtilsTest {

    public static final int TOTAL_METHODS = 2;

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
