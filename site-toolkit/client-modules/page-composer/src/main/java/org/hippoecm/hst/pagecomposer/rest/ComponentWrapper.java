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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "component")
public class ComponentWrapper {
    private List<Property> properties;
    private Boolean success;
    private Value[] parameterNames;
    private Value[] parameterValues;

    public ComponentWrapper(Node node) {
        this.success = true;
        properties = new ArrayList<Property>();

        PropertyIterator nodePropsIterator = null;
        try {
            nodePropsIterator = node.getProperties();
            while (nodePropsIterator.hasNext()) { //Do some silly hacking to get the properties and set them to JSON
                javax.jcr.Property jcrProp = nodePropsIterator.nextProperty();
                if (jcrProp.getType() == PropertyType.STRING) {  //Take only String properties others are not much of a use to us, I guess
                    if (!jcrProp.getDefinition().isMultiple()) {
                        properties.add(new Property(jcrProp.getName(), jcrProp.getValue().getString()));
                    }

                }
                if (jcrProp.getName().equals("hst:parameternames")) {
                    parameterNames = jcrProp.getValues();
                }
                if (jcrProp.getName().equals("hst:parametervalues")) {
                    parameterValues = jcrProp.getValues();
                }
            }
            if(parameterNames != null) {
                //Process Parameter Names and values
                for (int i = 0; i < parameterNames.length; i++) {
                    properties.add(new Property(parameterNames[i].getString(), parameterValues[i].getString()));
                }
            }
        } catch (RepositoryException e) {
            e.printStackTrace();  //TODO Fix me
            this.success = false;
        }

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
        private Object value;

        Property(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public String getName() {

            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
