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

public class Property {
    private String name;
    private String value;
    private String type;
    private String label;
    private String defaultValue;
    private String description;
    private boolean required;

    private String docType;
    private boolean allowCreation;
    private String docLocation;

    public Property() {
        name = "";
        value = "";
        type = ComponentWrapper.ParameterType.STRING;
        label = "";
        defaultValue = "";
        description = "";
        required = false;
        docType = "";
        allowCreation = false;
        docLocation = "";
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type.equals(ComponentWrapper.ParameterType.DATE)) {
            this.type = "datefield";
        } else if (type.equals(ComponentWrapper.ParameterType.BOOLEAN)) {
            this.type = "checkbox";
        } else if (type.equals(ComponentWrapper.ParameterType.NUMBER)) {
            this.type = "numberfield";
        } else if (type.equals(ComponentWrapper.ParameterType.DOCUMENT)) {
            this.type = "combo";
        } else if (type.equals(ComponentWrapper.ParameterType.COLOR)) {
            this.type = "colorfield";
        } else {
            this.type = "textfield";
        }
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

    public void setDocType(final String docType) {
        this.docType = docType;
    }

    public String getDocType() {
        return this.docType;
    }

    public boolean isAllowCreation() {
        return allowCreation;
    }

    public void setAllowCreation(boolean allowCreation) {
        this.allowCreation = allowCreation;
    }

    public String getDocLocation() {
        return docLocation;
    }

    public void setDocLocation(String docLocation) {
        this.docLocation = docLocation;
    }

}
