/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.channels;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods to retrieve methods and fields via reflection.
 */
final class ReflectionUtil {

    private static final Logger log = LoggerFactory.getLogger(ReflectionUtil.class);

    private ReflectionUtil() {
        // prevent instantiation
    }

    /**
     * Returns the string value of a field in an object via its getter method.
     *
     * @param o the object
     * @param fieldName the field to retrieve
     * @return the string value of the field, an empty string if the getter returned null, or null if no getter method
     * for this field exists.
     */
    static String getStringValue(Object o, String fieldName) {
        Method method = getGetterMethodForField(o.getClass(), fieldName);
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(o);
            return result == null ? StringUtils.EMPTY : result.toString();
        } catch (IllegalAccessException e) {
            log.error("Cannot access value of field '" + fieldName + "'", e);
        } catch (InvocationTargetException e) {
            log.error("Cannot invoke getter of field '" + fieldName + "'", e);
        }
        return null;
    }

    /**
     * Returns the getter method for a field in a class.
     *
     * @param clazz the class
     * @param fieldName the field in the class
     * @return the getter method for the given field, or null if no such getter method exists
     */
    static Method getGetterMethodForField(Class<?> clazz, String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }
        String capitalisedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        Method getter = getGetterMethod(clazz, "get" + capitalisedFieldName);
        if (getter == null) {
            getter = getGetterMethod(clazz, "is" + capitalisedFieldName);
        }

        return getter;
    }

    /**
     * Returns the getter method with the given name, or null if no such method exists.
     * A getter method should have zero parameters and not return void.
     *
     * @param clazz a class
     * @param methodName the name of a method in the given class
     * @return the getter method with the given name, or null if no such method exists.
     */
    static Method getGetterMethod(Class<?> clazz, String methodName) {
        try {
            Method getter = clazz.getMethod(methodName);
            if (getter.getParameterTypes().length == 0 && getter.getReturnType() != Void.class) {
                return getter;
            }
        } catch (NoSuchMethodException ignored) {
            // no getter method found, continue
        }
        return null;
    }


}
