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

}
