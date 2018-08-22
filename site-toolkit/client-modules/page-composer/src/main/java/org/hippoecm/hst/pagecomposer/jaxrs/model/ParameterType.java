/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For now, the supported UI xtypes (used for rendering the UI view) are
 * <ul>
 *     <li>textfield</li>
 *     <li>numberfield</li>
 *     <li>checkbox</li>
 *     <li>datefield</li>
 *     <li>colorfield : see also {@link Color}</li>
 * </ul>
 * ParameterType.UNKNOWN will result in xtype "textfield"
 */
public enum ParameterType {

    STRING("textfield", new Class[]{String.class}, null),
    VALUE_FROM_LIST("combo", new Class[]{String.class}, DropDownList.class),
    NUMBER("numberfield", new Class[]{Long.class, long.class, Integer.class, int.class, Short.class, short.class},
           null),
    BOOLEAN("checkbox", new Class[]{Boolean.class, boolean.class}, null),
    DATE("datefield", new Class[]{Date.class, Calendar.class}, null),
    COLOR("colorfield", new Class[]{String.class}, Color.class),
    JCR_PATH("linkpicker", new Class[]{String.class}, JcrPath.class),
    UNKNOWN("textfield", null, null);

    private static final Logger log = LoggerFactory.getLogger(ParameterType.class);

    final String xtype;
    HashSet<Class<?>> supportedReturnTypes = null;
    final Class<?> annotationType;

    ParameterType(String xtype, Class<?>[] supportedReturnTypes, Class<?> annotationType) {
        this.xtype = xtype;
        if (supportedReturnTypes != null) {
            this.supportedReturnTypes = new HashSet<Class<?>>(Arrays.asList(supportedReturnTypes));
        }
        this.annotationType = annotationType;
    }

    private boolean supportsReturnType(Class<?> returnType) {
        if (supportedReturnTypes == null) {
            return false;
        }
        return supportedReturnTypes.contains(returnType);
    }

    public static Annotation getTypeAnnotation(final Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            for (ParameterType type : ParameterType.values()) {
                if (annotation.annotationType() == type.annotationType) {
                    if (type.supportsReturnType(method.getReturnType())) {
                        return annotation;
                    } else {
                        log.warn("return type '{}' of method '{}' is not supported for annotation '{}'.",
                                 new String[]{method.getReturnType().getName(), method.getName(), type.annotationType.getName()});
                    }
                }
            }
        }
        return null;
    }

    static ParameterType getType(final Method method, Annotation annotation) {
        if (annotation != null) {
            for (ParameterType type : ParameterType.values()) {
                if (annotation.annotationType() == type.annotationType) {
                    return type;
                }
            }
        }
        for (ParameterType type : ParameterType.values()) {
            if (type.supportsReturnType(method.getReturnType())) {
                return type;
            }
        }
        return ParameterType.UNKNOWN;
    }

}
