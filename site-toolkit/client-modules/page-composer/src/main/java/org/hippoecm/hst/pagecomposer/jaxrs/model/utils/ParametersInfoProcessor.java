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
package org.hippoecm.hst.pagecomposer.jaxrs.model.utils;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class ParametersInfoProcessor {

    private static Logger log = LoggerFactory.getLogger(ParametersInfoProcessor.class);

    public static List<Property> getProperties(ParametersInfo parameterInfo) {
        return getProperties(parameterInfo, null);
    }

    public static List<Property> getProperties(ParametersInfo parameterInfo, Locale locale) {
        final List<Property> properties = new ArrayList<Property>();

        final Class classType = parameterInfo.type();
        if (classType == null) {
            return properties;
        }

        ResourceBundle resourceBundle = null;
        if (locale != null) {
            try {
                resourceBundle = ResourceBundle.getBundle(parameterInfo.type().getName(), locale);
            } catch (MissingResourceException missingResourceException) {
                log.warn("Could not find a resource bundle for class '{}', locale '{}'. The template composer properties panel will show displayName values instead of internationalised labels.", new Object[]{parameterInfo.type().getName(), locale});
            }
        }

        for (Method method : classType.getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                final Parameter propAnnotation = method.getAnnotation(Parameter.class);
                final Property prop = new ComponentWrapper.Property();
                prop.setName(propAnnotation.name());
                prop.setDefaultValue(propAnnotation.defaultValue());
                prop.setDescription(propAnnotation.description());
                prop.setRequired(propAnnotation.required());
                if (resourceBundle != null && resourceBundle.containsKey(propAnnotation.name())) {
                    prop.setLabel(resourceBundle.getString(propAnnotation.name()));
                } else {
                    if (propAnnotation.displayName().equals("")) {
                        prop.setLabel(propAnnotation.name());
                    } else {
                        prop.setLabel(propAnnotation.displayName());
                    }
                }

                String type = null;

                for (Annotation annotation : method.getAnnotations()) {
                    if (annotation == propAnnotation) {
                        continue;
                    }
                    if (annotation.annotationType() == DocumentLink.class) {
                        type = ComponentWrapper.ParameterType.DOCUMENT;
                        DocumentLink documentLink = (DocumentLink) annotation;
                        prop.setDocType(documentLink.docType());
                        prop.setDocLocation(documentLink.docLocation());
                        prop.setAllowCreation(documentLink.allowCreation());
                    } else if (annotation.annotationType() == Color.class) {
                        type = ComponentWrapper.ParameterType.COLOR;
                    }
                }

                if (type == null) {
                    type = getReturnType(method);
                }
                prop.setType(type);

                // Set the value to be default value before setting it with original value
                prop.setValue(propAnnotation.defaultValue());
                properties.add(prop);
            }
        }

        return properties;
    }

    private static String getReturnType(final Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == Date.class) {
            return ComponentWrapper.ParameterType.DATE;
        } else if (returnType == Long.class || returnType == long.class 
                || returnType == Integer.class || returnType == int.class 
                || returnType == Short.class || returnType == short.class) {
            return ComponentWrapper.ParameterType.NUMBER;
        } else if (returnType == Boolean.class || returnType == boolean.class) {
            return ComponentWrapper.ParameterType.BOOLEAN;
        } else if (returnType == String.class) {
            return ComponentWrapper.ParameterType.STRING;
        } else {
            return "UNKNOWN";
        }
    }

}
