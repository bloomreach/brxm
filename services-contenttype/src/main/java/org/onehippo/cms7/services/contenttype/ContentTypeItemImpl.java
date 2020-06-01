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

public class ContentTypeItemImpl extends Sealable implements ContentTypeItem {

    private final String definingType;
    private final String name;
    private EffectiveNodeTypeItem nti;
    private final boolean property;
    private boolean derivedItem;
    private final String itemType;
    private final String effectiveType;
    private boolean primaryItem;
    private boolean multiple;
    private boolean mandatory;
    private boolean autoCreated;
    private boolean protect;
    private boolean ordered;
    private boolean multiTyped;
    private List<EffectiveNodeTypeItem> multiTypes = Collections.emptyList();

    private List<String> validators = new ArrayList<String>();
    private Map<String, List<String>> itemProperties = new HashMap<String, List<String>>();

    @Override
    protected void doSeal() {
        validators = Collections.unmodifiableList(validators);
        for (Map.Entry<String,List<String>> entry : itemProperties.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        for (EffectiveNodeTypeItem item : multiTypes) {
            ((Sealable)item).seal();
        }
        itemProperties = Collections.unmodifiableMap(itemProperties);
    }

    protected ContentTypeItemImpl(String definingType, String name, String itemType, String effectiveType) {
        this.definingType = definingType;
        this.name = name;
        this.itemType = itemType;
        this.effectiveType = effectiveType;
        this.property = true;
    }

    protected ContentTypeItemImpl(String definingType, String name, String itemType) {
        this.definingType = definingType;
        this.name = name;
        this.itemType = itemType;
        this.effectiveType = itemType;
        this.property = false;
    }

    protected ContentTypeItemImpl(EffectiveNodeTypeProperty property) {
        this.definingType = property.getDefiningType();
        this.nti = property;
        this.primaryItem = false;
        this.property = true;
        this.derivedItem = true;
        this.name = property.getName();
        this.effectiveType = property.getType();
        this.itemType = this.effectiveType;
        this.multiple = property.isMultiple();
        this.mandatory = property.isMandatory();
        this.autoCreated = property.isAutoCreated();
        this.protect = property.isProtected();
        this.ordered = false;
    }

    protected ContentTypeItemImpl(EffectiveNodeTypeChild child) {
        this.definingType = child.getDefiningType();
        this.nti = child;
        this.primaryItem = false;
        this.property = false;
        this.derivedItem = true;
        this.name = child.getName();
        this.effectiveType = child.getType();
        this.itemType = this.effectiveType;
        this.multiple = child.isMultiple();
        this.mandatory = child.isMandatory();
        this.autoCreated = child.isAutoCreated();
        this.protect = child.isProtected();
        this.ordered = false;
    }

    protected ContentTypeItemImpl(ContentTypeItemImpl other) {
        this.definingType = other.definingType;
        this.nti = other.nti;
        this.primaryItem = other.primaryItem;
        this.property = other.property;
        this.derivedItem = other.derivedItem;
        this.name = other.name;
        this.effectiveType = other.effectiveType;
        this.itemType = other.itemType;
        this.multiple = other.multiple;
        this.mandatory = other.mandatory;
        this.autoCreated = other.autoCreated;
        this.protect = other.protect;
        this.ordered = other.ordered;
        this.validators.addAll(other.validators);
        this.itemProperties.putAll(other.itemProperties);
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
    public String getDefiningType() {
        return definingType;
    }

    @Override
    public boolean isProperty() {
        return property;
    }

    @Override
    public boolean isDerivedItem() {
        return derivedItem;
    }

    @Override
    public String getItemType() {
        return itemType;
    }

    @Override
    public String getEffectiveType() {
        return effectiveType;
    }

    @Override
    public boolean isPrimaryItem() {
        return primaryItem;
    }

    public void setPrimaryItem(boolean primaryItem) {
        checkSealed();
        this.primaryItem = primaryItem;
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
    public Map<String, List<String>> getItemProperties() {
        return itemProperties;
    }

    @Override
    public boolean isMultiTyped() {
        return !multiTypes.isEmpty();
    }

    public List<EffectiveNodeTypeItem> getMultiTypes() {
        return multiTypes;
    }

    public void setMultiPropertyTypes(List<EffectiveNodeTypeProperty> types) {
        checkSealed();
        if (types != null) {
            multiTypes = Collections.unmodifiableList(new ArrayList<EffectiveNodeTypeItem>(types));
        }
        else {
            multiTypes = Collections.emptyList();
        }
        multiTyped = !multiTypes.isEmpty();
    }

    public void setMultiChildTypes(List<EffectiveNodeTypeChild> types) {
        checkSealed();
        if (types != null) {
            multiTypes = Collections.unmodifiableList(new ArrayList<EffectiveNodeTypeItem>(types));
        }
        else {
            multiTypes = Collections.emptyList();
        }
        multiTyped = !multiTypes.isEmpty();
    }
}
