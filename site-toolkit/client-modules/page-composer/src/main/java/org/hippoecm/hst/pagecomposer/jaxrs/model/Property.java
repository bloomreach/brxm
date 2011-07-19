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

class Property {
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

    Property() {
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

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getValue() {
        return value;
    }

    void setValue(String value) {
        this.value = value;
    }

    String getType() {
        return type;
    }

    void setType(String type) {
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

    String getLabel() {
        return label;
    }

    void setLabel(String label) {
        this.label = label;
    }

    String getDefaultValue() {
        return defaultValue;
    }

    void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    boolean isRequired() {
        return required;
    }

    void setRequired(boolean required) {
        this.required = required;
    }

    void setDocType(final String docType) {
        this.docType = docType;
    }

    String getDocType() {
        return this.docType;
    }

    boolean isAllowCreation() {
        return allowCreation;
    }

    void setAllowCreation(boolean allowCreation) {
        this.allowCreation = allowCreation;
    }

    String getDocLocation() {
        return docLocation;
    }

    void setDocLocation(String docLocation) {
        this.docLocation = docLocation;
    }

}
