/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.pagecomposer.rest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.pagecomposer.annotations.Parameter;
import org.hippoecm.hst.pagecomposer.annotations.ParameterInfo;
import org.hippoecm.hst.pagecomposer.annotations.ParameterType;


/**
 * Component node wrapper that will be mapped automatically by the JAXRS (Jettison or JAXB) for generating the JSON/XML
 */
@XmlRootElement(name = "component")
public class ComponentWrapper {

    private static final String HST_PARAMETERNAMES = "hst:parameternames";
    private static final String HST_COMPONENTCLASSNAME = "hst:componentclassname";
    private static final String HST_PARAMETERVALUES = "hst:parametervalues";

    private List<Property> properties;
    private Boolean success = false;
    private Map<String, String> hstParameters;

    /**
     * Constructs a component node wrapper
     *
     * @param node JcrNode for a component.
     * @throws RepositoryException    Thrown if the reposiotry exception occurred during reading of the properties.
     * @throws ClassNotFoundException thrown when this class can't instantiate the component class.
     */
    public ComponentWrapper(Node node) throws RepositoryException, ClassNotFoundException {
        properties = new ArrayList<Property>();
        //Get the parameter names and values from the component node.
        if (node.hasProperty(HST_PARAMETERNAMES) && node.hasProperty(HST_PARAMETERVALUES)) {
            hstParameters = new HashMap<String, String>();
            Value[] paramNames = node.getProperty(HST_PARAMETERNAMES).getValues();
            Value[] paramValues = node.getProperty(HST_PARAMETERVALUES).getValues();
            for (int i = 0; i < paramNames.length; i++) {
                hstParameters.put(paramNames[i].getString(), paramValues[i].getString());
            }
        }

        //Get the properties via annotation on the component class
        String componentClassName = null;
        if (node.hasProperty(HST_COMPONENTCLASSNAME)) {
            componentClassName = node.getProperty(HST_COMPONENTCLASSNAME).getString();
        }

        if (componentClassName != null) {
            Class componentClass = Thread.currentThread().getContextClassLoader().loadClass(componentClassName);
            if (componentClass.isAnnotationPresent(ParameterInfo.class)) {
                ParameterInfo parameterInfo = (ParameterInfo) componentClass.getAnnotation(ParameterInfo.class);
                Field[] fields = parameterInfo.className().getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Parameter.class)) {
                        Parameter propAnnotation = field.getAnnotation(Parameter.class);
                        Property prop = new Property();
                        prop.setName(propAnnotation.name());
                        prop.setDefaultValue(propAnnotation.defaultValue());
                        prop.setDescription(propAnnotation.description());
                        prop.setType(propAnnotation.type());
                        prop.setRequired(propAnnotation.required());
                        if (propAnnotation.label().equals("")) {
                            prop.setLabel(propAnnotation.name());
                        } else {
                            prop.setLabel(propAnnotation.label());
                        }
                        if (hstParameters != null && hstParameters.get(propAnnotation.name()) != null) {
                            prop.setValue(hstParameters.get(propAnnotation.name()));
                        }

                        properties.add(prop);
                    }
                }

            }
        }
        this.success = true;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    class Property {
        private String name;
        private String value;
        private ParameterType type;
        private String label;
        private String defaultValue;
        private String description;
        private boolean required;

        Property() {
            name = "";
            value = "";
            type = ParameterType.STRING;
            label = "";
            defaultValue = "";
            description = "";
            required = false;
        }

        Property(String name, String value, ParameterType type, String label, String defaultValue, String description) {
            this.name = name;
            this.value = value;
            this.type = type;
            this.label = label;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public ParameterType getType() {
            return type;
        }

        public void setType(ParameterType type) {
            this.type = type;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }
}
