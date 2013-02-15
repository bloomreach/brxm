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
package org.onehippo.cms7.services.search.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.onehippo.cms7.services.search.annotation.Field;
import org.onehippo.cms7.services.search.annotation.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentBeanPropertyUtils {

    private static Logger log = LoggerFactory.getLogger(ContentBeanPropertyUtils.class);

    private ContentBeanPropertyUtils() {
    }

    public static PropertyDescriptor getIdentifierPropertyDescriptor(Class<?> contentBeanType) {
        try {
            PropertyDescriptor [] propDescs = PropertyUtils.getPropertyDescriptors(contentBeanType);

            if (propDescs != null) {
                for (PropertyDescriptor propDesc : propDescs) {
                    Method readMethod = PropertyUtils.getReadMethod(propDesc);

                    if (readMethod != null) {
                        Identifier identifier = readMethod.getAnnotation(Identifier.class);

                        if (identifier != null) {
                            return propDesc;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find field getter.", e);
        }

        return null;
    }

    public static PropertyDescriptor getIdentifierPropertyDescriptor(Object contentBean) {
        return getIdentifierPropertyDescriptor(contentBean.getClass());
    }

    public static Set<Method> getFieldPropertyReadMethods(Class<?> contentBeanType) {
        Set<Method> fieldPropReadMethods = new HashSet<Method>();

        PropertyDescriptor [] propDescs = PropertyUtils.getPropertyDescriptors(contentBeanType);

        if (propDescs != null) {
            for (PropertyDescriptor propDesc : propDescs) {
                Method readMethod = propDesc.getReadMethod();
                Field field = readMethod.getAnnotation(Field.class);

                if (field != null) {
                    fieldPropReadMethods.add(readMethod);
                }
            }
        }

        return fieldPropReadMethods;
    }

    public static Set<Method> getFieldPropertyReadMethods(Object contentBean) {
        return getFieldPropertyReadMethods(contentBean.getClass());
    }

    /**
     * Returns property name or null if it is not a property getter or setter method.
     * @param propertyMethod
     * @return
     */
    public static String getPropertyName(Method propertyMethod) {
        String methodName = propertyMethod.getName();
        Class<?> [] paramTypes = propertyMethod.getParameterTypes();
        Class<?> returnType = propertyMethod.getReturnType();

        if (methodName.startsWith("get") && paramTypes.length == 0) {
            return getCamelString(methodName.substring(3));
        } else if (methodName.startsWith("is") && paramTypes.length == 0 && (returnType == boolean.class || returnType == Boolean.class)) {
            return getCamelString(methodName.substring(2));
        } else if (methodName.startsWith("set") && paramTypes.length == 1) {
            return getCamelString(methodName.substring(3));
        } else {
            return null;
        }
    }

    private static String getCamelString(String s) {
        char firstChar = s.charAt(0);

        if (Character.isUpperCase(firstChar)) {
            StringBuilder sb = new StringBuilder(s);
            sb.setCharAt(0, Character.toLowerCase(firstChar));
            s = sb.toString();
        }

        return s;
    }
}
