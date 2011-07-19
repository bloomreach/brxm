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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.configuration.components.Parameter;
import org.hippoecm.hst.configuration.components.ParametersInfo;

/**
 * Process the (deprecated) old-style parameter annotations in the package org.hippoecm.hst.components.
 * This class is solely used to be backwards compatible with the deprecated old annotations until they are removed
 * completely from the code base.
 *
 * @see {@link Parameter}
 * @see {@link ParametersInfo}
 */
class OldParametersInfoProcessor {

    static List<Property> getProperties(ParametersInfo parameterInfo) {
        List<Property> properties = new ArrayList<Property>();

        final Class classType = parameterInfo.type();
        if (classType == null) {
            return properties;
        }

        for (Method method : parameterInfo.type().getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                Parameter propAnnotation = method.getAnnotation(Parameter.class);
                Property prop = new Property();
                prop.setName(propAnnotation.name());
                prop.setDefaultValue(propAnnotation.defaultValue());
                prop.setDescription(propAnnotation.description());
                prop.setType(propAnnotation.typeHint());
                prop.setDocType(propAnnotation.docType());
                prop.setRequired(propAnnotation.required());
                if (propAnnotation.displayName().equals("")) {
                    prop.setLabel(propAnnotation.name());
                } else {
                    prop.setLabel(propAnnotation.displayName());
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
