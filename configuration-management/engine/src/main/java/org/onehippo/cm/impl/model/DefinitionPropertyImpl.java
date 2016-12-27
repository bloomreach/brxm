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
package org.onehippo.cm.impl.model;

import java.text.MessageFormat;

import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueFormatException;
import org.onehippo.cm.api.model.ValueType;

public class DefinitionPropertyImpl extends DefinitionItemImpl implements DefinitionProperty {

    private PropertyType propertyType;
    private ValueType valueType;
    private Value value;
    private Value[] values;

    public DefinitionPropertyImpl(final String name, final Value value, final DefinitionNodeImpl parent) {
        super(name, parent);
        this.propertyType = PropertyType.SINGLE;
        this.valueType = value.getType();
        this.value = value;
        this.values = null;
    }

    public DefinitionPropertyImpl(final String name, final Value[] values, final DefinitionNodeImpl parent) {
        super(name, parent);
        this.propertyType = PropertyType.LIST;

        if (values.length > 0) {
            final ValueType valueType = values[0].getType();
            for (int i = 1; i < values.length; i++) {
                // todo create unit test and parser test
                if (!valueType.equals(values[i].getType())) {
                    throw new IllegalArgumentException(MessageFormat.format(
                            "Argument 'values' must contain values of the same type, found value type '{}' as well as '{}'",
                            valueType,
                            values[i].getType()));
                }
            }
            this.valueType = valueType;
        } else {
            this.valueType = ValueType.STRING;
        }

        this.value = null;
        this.values = values;
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
    public Value getValue() throws ValueFormatException {
        if (value == null) {
            throw new ValueFormatException("Property contains multiple values");
        }
        return value;
    }

    @Override
    public Value[] getValues() throws ValueFormatException {
        if (values == null) {
            throw new ValueFormatException("Property contains single value");
        }
        return values;
    }

}
