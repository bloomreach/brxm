/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.DefinitionProperty;
import org.onehippo.cm.model.tree.PropertyOperation;
import org.onehippo.cm.model.tree.PropertyKind;
import org.onehippo.cm.model.tree.ValueFormatException;
import org.onehippo.cm.model.tree.ValueType;

import static java.util.Arrays.asList;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.onehippo.cm.model.Constants.META_CATEGORY_KEY;
import static org.onehippo.cm.model.Constants.META_KEY_PREFIX;

public class DefinitionPropertyImpl extends DefinitionItemImpl implements DefinitionProperty, Comparable<DefinitionProperty> {

    private PropertyKind kind;
    private ValueType valueType;
    private ValueImpl value;
    private List<ValueImpl> values;
    private PropertyOperation operation = PropertyOperation.REPLACE;

    public DefinitionPropertyImpl(final String name, final ValueImpl value, final DefinitionNodeImpl parent) {
        super(name, parent);
        this.kind = PropertyKind.SINGLE;
        this.valueType = value.getType();
        this.value = value;
        this.values = null;

        value.setParent(this);
    }

    public DefinitionPropertyImpl(final String name, final ValueType valueType, final List<ValueImpl> values, final DefinitionNodeImpl parent) {
        super(name, parent);
        this.kind = PropertyKind.LIST;
        this.valueType = valueType;
        this.value = null;
        this.values = new ArrayList<>(values);

        for (final ValueImpl value : this.values) {
            value.setParent(this);
        }
    }

    @Override
    public PropertyKind getKind() {
        return kind;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public boolean isMultiple() {
        return getKind().isMultiple();
    }

    @Override
    public ValueImpl getValue() throws ValueFormatException {
        if (value == null) {
            throw new ValueFormatException("Property contains multiple values");
        }
        return value;
    }

    @Override
    public List<ValueImpl> getValues() throws ValueFormatException {
        if (values == null) {
            throw new ValueFormatException("Property contains single value");
        }
        return values;
    }

    @Override
    public PropertyOperation getOperation() {
        return operation;
    }

    public void setOperation(final PropertyOperation operation) {
        this.operation = operation;
    }

    @Override
    public boolean isDeleted() {
        return getOperation() == PropertyOperation.DELETE;
    }

    public void setCategory(final ConfigurationItemCategory category) {
        if (category == ConfigurationItemCategory.CONTENT) {
            throw new IllegalArgumentException("Properties do not support " + META_CATEGORY_KEY + " value '"
                    + ConfigurationItemCategory.CONTENT + "'");
        }
        super.setCategory(category);
    }

    /**
     * Helper method to determine if this is a .meta:category: system property with no initial value on this def.
     */
    public boolean isEmptySystemProperty() {
        return getCategory() == ConfigurationItemCategory.SYSTEM
                && getValueType() == ValueType.STRING
                && getKind() != PropertyKind.SINGLE
                && getValues().size() == 0
                && getOperation() == PropertyOperation.REPLACE;
    }

    /**
     * In-place update the content of this definition with content from another definition. This is expected to be used
     * only by auto-export.
     * @param other a definition containing the new state that should be merged here
     */
    public void updateFrom(final DefinitionPropertyImpl other) {
        switch (other.operation) {
            case REPLACE:
            case OVERRIDE:

                // calling code will set other.operation as appropriate for final value
                this.operation = other.operation;

                // replace operation does not change value type or property type
                // but override does, and doing it for replace operation should be safe
                this.valueType = other.valueType;
                this.kind = other.kind;

                if (kind == PropertyKind.SINGLE) {
                    this.values = null;
                    this.value = other.value.clone();
                    value.setParent(this);
                } else {
                    this.value = null;

                    // cloneValues() sets parent and retains value source for us
                    this.values = other.cloneValues(this);
                }
                break;
            case ADD:
                // add operation does not change value type
                // add operation does not change operation here

                // an add operation is only valid for a property that is already multi-valued
                // (i.e. an add to a single-valued property would need to be an override instead)
                if (other.kind == PropertyKind.SINGLE) {
                    this.values.add(other.value.clone().setParent(this));
                }
                else {
                    this.values.addAll(other.cloneValues(this));
                }
                break;
            case DELETE:
                this.operation = PropertyOperation.DELETE;
                this.values = null;
                this.value = null;
                break;
        }
    }

    /**
     * Clone the values of this Property and set the parent of the newly-created clones to newParent.
     * @param newParent the parent of the new Values
     * @return the newly-created cloned Values
     */
    protected List<ValueImpl> cloneValues(final DefinitionPropertyImpl newParent) {
        final List<ValueImpl> cloned = new ArrayList<>();
        for (final ValueImpl value : values) {
            cloned.add(value.clone().setParent(newParent));
        }
        return cloned;
    }

    @Override
    public int compareTo(final DefinitionProperty o) {
        // first check object equality
        if (this == o) {
            return 0;
        }

        final String oName = o.getName();
        final String name = getName();
        if (name.startsWith(META_KEY_PREFIX) && !oName.startsWith(META_KEY_PREFIX)) {
            return -1;
        }
        if (!name.startsWith(META_KEY_PREFIX) && oName.startsWith(META_KEY_PREFIX)) {
            return 1;
        }
        if (name.equals(JCR_PRIMARYTYPE) && !oName.equals(JCR_PRIMARYTYPE)) {
            return -1;
        }
        if (!name.equals(JCR_PRIMARYTYPE) && oName.equals(JCR_PRIMARYTYPE)) {
            return 1;
        }
        if (name.equals(JCR_MIXINTYPES) && !oName.equals(JCR_MIXINTYPES)) {
            return -1;
        }
        if (!name.equals(JCR_MIXINTYPES) && oName.equals(JCR_MIXINTYPES)) {
            return 1;
        }
        if (name.equals(JCR_UUID) && !oName.equals(JCR_UUID)) {
            return -1;
        }
        if (!name.equals(JCR_UUID) && oName.equals(JCR_UUID)) {
            return 1;
        }

        final int byName = name.compareTo(oName);
        if (byName != 0) {
            return byName;
        }
        else {
            // final disambiguation via hashCode
            return Integer.compare(hashCode(), o.hashCode());
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"{path='"+ getPath()+"', "
                + ( getKind().isMultiple()? ("values=" + getValues()): ("value=" + getValue().toString()) )
                + "}";
    }
}
