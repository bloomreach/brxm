/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl;

import org.onehippo.cm.model.DefinitionProperty;
import org.onehippo.cm.model.PropertyOperation;
import org.onehippo.cm.model.PropertyType;
import org.onehippo.cm.model.ValueFormatException;
import org.onehippo.cm.model.ValueType;

public class DefinitionPropertyImpl extends DefinitionItemImpl implements DefinitionProperty {

    private PropertyType propertyType;
    private ValueType valueType;
    private ValueImpl value;
    private ValueImpl[] values;
    private PropertyOperation operation = PropertyOperation.REPLACE;

    public DefinitionPropertyImpl(final String name, final ValueImpl value, final DefinitionNodeImpl parent) {
        super(name, parent);
        this.propertyType = PropertyType.SINGLE;
        this.valueType = value.getType();
        this.value = value;
        this.values = null;

        value.setParent(this);
    }

    public DefinitionPropertyImpl(final String name, final ValueType valueType, final ValueImpl[] values, final DefinitionNodeImpl parent) {
        super(name, parent);
        this.propertyType = PropertyType.LIST;
        this.valueType = valueType;
        this.value = null;
        this.values = values;

        for (ValueImpl value : values) {
            value.setParent(this);
        }
    }

    @Override
    public PropertyType getType() {
        return propertyType;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public ValueImpl getValue() throws ValueFormatException {
        if (value == null) {
            throw new ValueFormatException("Property contains multiple values");
        }
        return value;
    }

    @Override
    public ValueImpl[] getValues() throws ValueFormatException {
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

    /**
     * In-place update the content of this definition with content from another definition.
     * @param other
     */
    public void updateFrom(final DefinitionPropertyImpl other) {
        if (other.operation == PropertyOperation.REPLACE) {
            this.propertyType = other.propertyType;

            // replace operation does not change value type

            // todo merge correctly with all operations, existing and new
            // todo should override from old local def stay in place?
            this.operation = other.operation;

            if (propertyType == PropertyType.SINGLE) {
                this.values = null;
                this.value = other.value.clone();
                value.setParent(this);

                // migrate resources from old module to new module
                value.setForeignSource(other.getDefinition().getSource());
            } else {
                this.value = null;
                this.values = other.cloneValues(this);
            }
        }
        else if (other.operation == PropertyOperation.ADD) {
            // todo simplify this if add is not allowed to change multiplicity
            if (this.propertyType == PropertyType.SINGLE) {
                if (other.propertyType == PropertyType.SINGLE) {
                    ValueImpl[] tmp = new ValueImpl[2];
                    tmp[0] = other.value.clone();
                    tmp[1] = other.value.clone();
                    tmp[1].setForeignSource(other.getDefinition().getSource());
                    tmp[1].setParent(this);
                    this.values = tmp;
                } else {
                    ValueImpl[] tmp = new ValueImpl[1 + other.values.length];
                    tmp[0] = this.value.clone();
                    ValueImpl[] tmp2 = other.cloneValues(this);
                    System.arraycopy(tmp2, 0, tmp, 1, tmp2.length);
                    this.values = tmp;
                }

                // after an add, we always have a multi-value property
                this.propertyType = PropertyType.LIST;
                this.value = null;
            }
            else {
                if (other.propertyType == PropertyType.SINGLE) {
                    ValueImpl[] tmp = new ValueImpl[values.length + 1];
                    System.arraycopy(this.values, 0, tmp, 0, values.length);
                    tmp[tmp.length - 1] = other.value.clone();
                    tmp[tmp.length - 1].setForeignSource(other.getDefinition().getSource());
                    tmp[tmp.length - 1].setParent(this);
                    this.values = tmp;
                } else {
                    ValueImpl[] tmp = new ValueImpl[values.length + other.values.length];
                    System.arraycopy(this.values, 0, tmp, 0, values.length);
                    ValueImpl[] tmp2 = other.cloneValues(this);
                    System.arraycopy(tmp2, 0, tmp, this.values.length, tmp2.length);
                    this.values = tmp;
                }
            }

            // add operation does not change value type
            // add operation does not change operation here
        }
    }

    /**
     * Clone the values array of this Property and set the parent of the newly-created clones to newParent.
     * @param newParent the parent of the new Values
     * @return the newly-created cloned Values
     */
    protected ValueImpl[] cloneValues(final DefinitionPropertyImpl newParent) {
        final ValueImpl[] cloned = new ValueImpl[values.length];

        for (int i = 0; i < values.length; i++) {
            cloned[i] = values[i].clone();
            cloned[i].setParent(newParent);

            // migrate resources from old module to new module
            cloned[i].setForeignSource(getDefinition().getSource());
        }
        return cloned;
    }
}
