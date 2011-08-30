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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.hippoecm.hst.configuration.components.Parameter;
import org.hippoecm.hst.configuration.components.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process the (deprecated) old-style parameter annotations in the package org.hippoecm.hst.components.
 * This class is solely used to be backwards compatible with the deprecated old annotations until they are removed
 * completely from the code base.
 *
 * @see {@link Parameter}
 * @see {@link ParametersInfo}
 * @deprecated
 */
@Deprecated
public class OldParametersInfoProcessor {

     private static Logger log = LoggerFactory.getLogger(OldParametersInfoProcessor.class);

    public static List<Property> getProperties(ParametersInfo parameterInfo) {
        return getProperties(parameterInfo, null);
    }

    public static List<Property> getProperties(ParametersInfo parameterInfo, Locale locale) {
        List<Property> properties = new ArrayList<Property>();

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

        for (Method method : parameterInfo.type().getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                Parameter propAnnotation = method.getAnnotation(Parameter.class);
                Property prop =  new ComponentWrapper.Property();
                prop.setName(propAnnotation.name());
                prop.setDefaultValue(propAnnotation.defaultValue());
                prop.setDescription(propAnnotation.description());
                prop.setType(propAnnotation.typeHint());
                prop.setDocType(propAnnotation.docType());
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
                prop.setDocLocation(propAnnotation.docLocation());
                prop.setAllowCreation(propAnnotation.allowCreation());

                // Initialize each value with the default value before later on setting it with the original value
                prop.setValue(propAnnotation.defaultValue());

                properties.add(prop);
            }
        }

        return properties;
    }
}
