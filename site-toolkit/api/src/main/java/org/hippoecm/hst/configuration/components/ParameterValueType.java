/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.components;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Supported value types of dynamic component parameters are
 * <ul>
 *     <li>text</li>
 *     <li>integer</li>
 *     <li>decimal</li>
 *     <li>boolean</li>
 *     <li>datetime</li>
 * </ul>
 * These value types should be matched with the types in org.hippoecm.hst.pagecomposer.jaxrs.model.ParameterType 
 */ 
public enum ParameterValueType {

    STRING("text", new Class[] { String.class }),
    INTEGER("integer", new Class[] { Long.class, long.class, Integer.class, int.class, Short.class, short.class }),
    DECIMAL("decimal", new Class[] { Double.class, double.class, Float.class, float.class }),
    BOOLEAN("boolean", new Class[] { Boolean.class, boolean.class }),
    DATE("datetime", new Class[] { Date.class, Calendar.class });

    final String type;
    Set<Class<?>> supportedReturnTypes = null;

    ParameterValueType(String type, Class<?>[] supportedReturnTypes) {
        this.type = type;
        if (supportedReturnTypes != null) {
            this.supportedReturnTypes = new LinkedHashSet<Class<?>>(Arrays.asList(supportedReturnTypes));
        }
    }

    public Class<?> getDefaultReturnType() {
        if (supportedReturnTypes != null && supportedReturnTypes.iterator().hasNext()) {
            return supportedReturnTypes.iterator().next();
        }
        return String.class;
    }

    private boolean supportsReturnType(Class<?> returnType) {
        if (supportedReturnTypes == null) {
            return false;
        }
        return supportedReturnTypes.contains(returnType);
    }

    public static ParameterValueType getValueType(final Method method) {
        for (ParameterValueType type : ParameterValueType.values()) {
            if (type.supportsReturnType(method.getReturnType())) {
                return type;
            }
        }
        return ParameterValueType.STRING;
    }

    public static Optional<ParameterValueType> getValueType(final String valueType) {
        for (ParameterValueType type : ParameterValueType.values()) {
            if (type.type.equals(valueType)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

}
