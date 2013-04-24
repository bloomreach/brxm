/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.contenttype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentTypeFieldImpl extends Sealable implements DocumentTypeField {

    private final String definingType;
    private final String name;
    private EffectiveNodeTypeItem nti;
    private String caption;
    private final boolean propertyField;
    private boolean derivedField;
    private final String fieldType;
    private final String itemType;
    private boolean primaryField;
    private boolean multiple;
    private boolean mandatory;
    private boolean autoCreated;
    private boolean protect;
    private boolean ordered;
    private List<String> validators = new ArrayList<String>();
    private Map<String, List<String>> fieldProperties = new HashMap<String, List<String>>();

    @Override
    protected void doSeal() {
        validators = Collections.unmodifiableList(validators);
        for (Map.Entry<String,List<String>> entry : fieldProperties.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        fieldProperties = Collections.unmodifiableMap(fieldProperties);
    }

    public DocumentTypeFieldImpl(String definingType, String name, String fieldType, String itemType) {
        this.definingType = definingType;
        this.name = name;
        this.fieldType = fieldType;
        this.itemType = itemType;
        this.propertyField = true;
    }

    public DocumentTypeFieldImpl(String definingType, String name, String fieldType) {
        this.definingType = definingType;
        this.name = name;
        this.fieldType = fieldType;
        this.itemType = fieldType;
        this.propertyField = false;
    }

    public DocumentTypeFieldImpl(String definingType, EffectiveNodeTypeProperty property) {
        this.definingType = definingType;
        this.nti = property;
        this.primaryField = false;
        this.propertyField = true;
        this.derivedField = true;
        this.name = property.getName();
        this.itemType = property.getType();
        this.fieldType = this.itemType;
        this.multiple = property.isMultiple();
        this.mandatory = property.isMandatory();
        this.autoCreated = property.isAutoCreated();
        this.protect = property.isProtected();
        this.ordered = false;
    }

    public DocumentTypeFieldImpl(String definingType, EffectiveNodeTypeChild child) {
        this.definingType = definingType;
        this.nti = child;
        this.primaryField = false;
        this.propertyField = false;
        this.derivedField = true;
        this.name = child.getName();
        this.itemType = child.getType();
        this.fieldType = this.itemType;
        this.multiple = child.isMultiple();
        this.mandatory = child.isMandatory();
        this.autoCreated = child.isAutoCreated();
        this.protect = child.isProtected();
        this.ordered = false;
    }

    public DocumentTypeFieldImpl(DocumentTypeFieldImpl other) {
        this.definingType = other.definingType;
        this.nti = other.nti;
        this.primaryField = other.primaryField;
        this.propertyField = other.propertyField;
        this.derivedField = other.derivedField;
        this.name = other.name;
        this.itemType = other.itemType;
        this.fieldType = other.fieldType;
        this.multiple = other.multiple;
        this.mandatory = other.mandatory;
        this.autoCreated = other.autoCreated;
        this.protect = other.protect;
        this.ordered = other.ordered;
        this.validators.addAll(other.validators);
        this.fieldProperties.putAll(other.fieldProperties);
    }

    @Override
    public EffectiveNodeTypeItem getEffectiveNodeTypeItem() {
        return nti;
    }

    public void setEffectiveNodeTypeItem(EffectiveNodeTypeItem nti) {
        checkSealed();
        this.nti = nti;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        checkSealed();
        this.caption = caption;
    }

    @Override
    public String getDefiningType() {
        return definingType;
    }

    @Override
    public boolean isPropertyField() {
        return propertyField;
    }

    @Override
    public boolean isDerivedField() {
        return derivedField;
    }

    @Override
    public String getFieldType() {
        return fieldType;
    }

    @Override
    public String getItemType() {
        return itemType;
    }

    @Override
    public boolean isPrimaryField() {
        return primaryField;
    }

    public void setPrimaryField(boolean primaryField) {
        checkSealed();
        this.primaryField = primaryField;
    }

    @Override
    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        checkSealed();
        this.multiple = multiple;
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        checkSealed();
        this.mandatory = mandatory;
    }

    @Override
    public boolean isAutoCreated() {
        return autoCreated;
    }

    public void setAutoCreated(boolean autoCreated) {
        checkSealed();
        this.autoCreated = autoCreated;
    }

    @Override
    public boolean isProtected() {
        return protect;
    }

    public void setProtected(boolean protect) {
        checkSealed();
        this.protect = protect;
    }

    @Override
    public boolean isOrdered() {
        return ordered;
    }

    public void setOrdered(boolean ordered) {
        checkSealed();
        this.ordered = ordered;
    }

    @Override
    public List<String> getValidators() {
        return validators;
    }

    @Override
    public Map<String, List<String>> getFieldProperties() {
        return fieldProperties;
    }
}
