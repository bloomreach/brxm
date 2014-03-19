/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import com.google.gson.JsonElement;

public class ContainerItemComponentPropertyRepresentation {
    private String name;
    private String value;
    private ParameterType type;
    private String label;
    private String defaultValue;
    private String description;
    private boolean required;
    private String groupLabel;

    private String docType;
    private boolean allowCreation;
    private String docLocation;

    private String pickerConfiguration;
    private String pickerInitialPath;
    private String pickerRootPath;

    private boolean pickerPathIsRelative;
    private boolean pickerRemembersLastVisited;
    private String[] pickerSelectableNodeTypes;

    private String[] dropDownListValues;
    private String[] dropDownListDisplayValues;

    private boolean hiddenInChannelManager;

    public ContainerItemComponentPropertyRepresentation() {
        name = "";
        value = "";
        type = ParameterType.STRING;
        label = "";
        defaultValue = "";
        description = "";
        required = false;
        docType = "";
        allowCreation = false;
        docLocation = "";
        hiddenInChannelManager = false;
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
        return type.xtype;
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

    public String getGroupLabel() {
        return groupLabel;
    }

    public void setGroupLabel(final String groupLabel) {
        this.groupLabel = groupLabel;
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

    public String getPickerConfiguration() {
        return pickerConfiguration;
    }

    public void setPickerConfiguration(final String pickerConfiguration) {
        this.pickerConfiguration = pickerConfiguration;
    }

    public String getPickerInitialPath() {
        return pickerInitialPath;
    }

    public void setPickerInitialPath(final String pickerInitialPath) {
        this.pickerInitialPath = pickerInitialPath;
    }

    public String getPickerRootPath() {
        return pickerRootPath;
    }

    public void setPickerRootPath(final String pickerRootPath) {
        this.pickerRootPath = pickerRootPath;
    }

    public boolean isPickerPathIsRelative() {
        return pickerPathIsRelative;
    }

    public void setPickerPathIsRelative(final boolean pickerPathIsRelative) {
        this.pickerPathIsRelative = pickerPathIsRelative;
    }

    public boolean isPickerRemembersLastVisited() {
        return pickerRemembersLastVisited;
    }

    public void setPickerRemembersLastVisited(final boolean pickerRemembersLastVisited) {
        this.pickerRemembersLastVisited = pickerRemembersLastVisited;
    }

    public String[] getPickerSelectableNodeTypes() {
        return pickerSelectableNodeTypes;
    }

    public void setPickerSelectableNodeTypes(final String[] pickerSelectableNodeTypes) {
        this.pickerSelectableNodeTypes = pickerSelectableNodeTypes;
    }

    public String[] getDropDownListValues() {
        return dropDownListValues;
    }

    public void setDropDownListValues(String[] dropDownValues) {
        this.dropDownListValues = dropDownValues;
    }

    public String[] getDropDownListDisplayValues() {
        return dropDownListDisplayValues;
    }

    public void setDropDownListDisplayValues(String[] dropDownListDisplayValues) {
        this.dropDownListDisplayValues = dropDownListDisplayValues;
    }

    public boolean isHiddenInChannelManager() {
        return hiddenInChannelManager;
    }

    public void setHiddenInChannelManager(final boolean hiddenInChannelManager) {
        this.hiddenInChannelManager = hiddenInChannelManager;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ContainerItemComponentPropertyRepresentation that = (ContainerItemComponentPropertyRepresentation) o;

        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
