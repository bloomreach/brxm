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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hippoecm.hst.configuration.components.Color;
import org.hippoecm.hst.configuration.components.DocumentLink;
import org.hippoecm.hst.configuration.components.Parameter;
import org.hippoecm.hst.configuration.components.ParameterType;
import org.hippoecm.hst.configuration.components.ParametersInfo;

public class ParametersInfoProcessor {

    public static List<Property> getProperties(ParametersInfo parameterInfo) {
        List<Property> properties = new ArrayList<Property>();
        Method[] methods = parameterInfo.type().getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Parameter.class)) {
                Parameter propAnnotation = method.getAnnotation(Parameter.class);
                Property prop = new Property();
                prop.setName(propAnnotation.name());
                prop.setDefaultValue(propAnnotation.defaultValue());
                prop.setDescription(propAnnotation.description());
                prop.setRequired(propAnnotation.required());
                if (propAnnotation.displayName().equals("")) {
                    prop.setLabel(propAnnotation.name());
                } else {
                    prop.setLabel(propAnnotation.displayName());
                }

                boolean hasType = false;

                // Backwards compatible type processing
                String type = propAnnotation.typeHint();
                if (!"".equals(type)) {
                    hasType = true;
                }
                if (ParameterType.DOCUMENT.equals(type)) {
                    prop.setDocType(propAnnotation.docType());
                    prop.setDocLocation(propAnnotation.docLocation());
                    prop.setAllowCreation(propAnnotation.allowCreation());
                }

                Annotation[] annotations = method.getAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation == propAnnotation) {
                        continue;
                    }
                    if (annotation.annotationType() == DocumentLink.class) {
                        type = ParameterType.DOCUMENT;
                        hasType = true;
                        DocumentLink documentLink = (DocumentLink) annotation;
                        prop.setDocType(documentLink.docType());
                        prop.setDocLocation(documentLink.docLocation());
                        prop.setAllowCreation(documentLink.allowCreation());
                    } else if (annotation.annotationType() == Color.class) {
                        type = ParameterType.COLOR;
                        hasType = true;
                    }
                }
                if (!hasType) {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == Date.class) {
                        type = ParameterType.DATE;
                    } else if (returnType == Long.class || returnType == Integer.class) {
                        type = ParameterType.NUMBER;
                    } else if (returnType == Boolean.class) {
                        type = ParameterType.BOOLEAN;
                    } else if (returnType == String.class) {
                        type = ParameterType.STRING;
                    }
                }
                prop.setType(type);

                //Set the value to be default value before setting it with original value
                prop.setValue(propAnnotation.defaultValue());
                properties.add(prop);
            }
        }
        return properties;
    }
}
