/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.dynamic;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestDynamicBeanUtils {

    @Test
    public void testCreateJavaClassName() {
        final String documentTypeName = "myproject:newsdocument";
        final String className = DynamicBeanUtils.createJavaClassName(documentTypeName);
        assertEquals("Newsdocument", className);
    }

    @Test
    public void testCreateValidMethodNameFromValidPropertyName() {
        final String propertyName = "myproject:title"; 
        final String getterName = DynamicBeanUtils.createMethodName(propertyName);
        assertEquals("getTitle", getterName);
    }

    @Test
    public void testCreateMethodNameFromInvalidPropertyName() {
        final String propertyName = "myproject:!introduction1&";
        final String getterName = DynamicBeanUtils.createMethodName(propertyName);
        assertEquals("getIntroduction1", getterName);
    }

}
