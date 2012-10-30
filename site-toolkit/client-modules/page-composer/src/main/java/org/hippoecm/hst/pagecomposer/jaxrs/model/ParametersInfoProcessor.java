/*
 *  Copyright 2011-2012 Hippo.
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
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParametersInfoProcessor {

    static final Logger log = LoggerFactory.getLogger(ParametersInfoProcessor.class);

    public List<ContainerItemComponentPropertyRepresentation> getProperties(ParametersInfo parameterInfo, Locale locale,
            String currentMountCanonicalContentPath) {
        final List<ContainerItemComponentPropertyRepresentation> properties = new ArrayList<ContainerItemComponentPropertyRepresentation>();

        final Class<?> classType = parameterInfo.type();
        if (classType == null) {
            return properties;
        }

        ResourceBundle resourceBundle = null;
        final String typeName = parameterInfo.type().getName();
        try {
            if (locale != null) {
                resourceBundle = ResourceBundle.getBundle(typeName, locale);
            } else {
                resourceBundle = ResourceBundle.getBundle(typeName);
            }
        } catch (MissingResourceException missingResourceException) {
            log.error("Could not find a resource bundle for class '{}', locale '{}'. The template composer properties panel will show displayName values instead of internationalised labels.", new Object[]{typeName, locale});
        }

        for (Method method : classType.getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                final Parameter propAnnotation = method.getAnnotation(Parameter.class);
                final ContainerItemComponentPropertyRepresentation prop = new ContainerItemComponentPropertyRepresentation();
                prop.setName(propAnnotation.name());
                prop.setDefaultValue(propAnnotation.defaultValue());
                prop.setDescription(propAnnotation.description());
                prop.setRequired(propAnnotation.required());
                prop.setHiddenInChannelManager(propAnnotation.hideInChannelManager());
                if (resourceBundle != null && resourceBundle.containsKey(propAnnotation.name())) {
                    prop.setLabel(resourceBundle.getString(propAnnotation.name()));
                } else {
                    if (propAnnotation.displayName().equals("")) {
                        prop.setLabel(propAnnotation.name());
                    } else {
                        prop.setLabel(propAnnotation.displayName());
                    }
                }

                final Annotation annotation = ParameterType.getTypeAnnotation(method);
                if (annotation instanceof DocumentLink) {
                    // for DocumentLink we need some extra processing
                    final DocumentLink documentLink = (DocumentLink) annotation;
                    prop.setDocType(documentLink.docType());
                    prop.setDocLocation(documentLink.docLocation());
                    prop.setAllowCreation(documentLink.allowCreation());
                } else if (annotation instanceof JcrPath) {
                    // for JcrPath we need some extra processing too
                    final JcrPath jcrPath = (JcrPath) annotation;
                    prop.setPickerConfiguration(jcrPath.pickerConfiguration());
                    prop.setPickerInitialPath(jcrPath.pickerInitialPath());
                    prop.setPickerRootPath(currentMountCanonicalContentPath);
                    prop.setPickerPathIsRelative(jcrPath.isRelative());
                    prop.setPickerRemembersLastVisited(jcrPath.pickerRemembersLastVisited());
                    prop.setPickerSelectableNodeTypes(jcrPath.pickerSelectableNodeTypes());
                } else if (annotation instanceof DropDownList) {
                    final DropDownList dropDownList = (DropDownList)annotation;
                    final String values[] = dropDownList.value();
                    final String[] displayValues = new String[values.length];

                    for (int i = 0; i < values.length; i++) {
                        final String resourceKey = propAnnotation.name() + "/" + values[i];
                        if (resourceBundle != null && resourceBundle.containsKey(resourceKey)) {
                            displayValues[i] = resourceBundle.getString(resourceKey);
                        } else {
                            displayValues[i] = values[i];
                        }
                    }

                    prop.setDropDownListValues(values);
                    prop.setDropDownListDisplayValues(displayValues);
                }

                final ParameterType type = ParameterType.getType(method, annotation);
                prop.setType(type);

                // Set the value to be default value before setting it with original value
                properties.add(prop);
            }
        }

        return properties;
    }

}
