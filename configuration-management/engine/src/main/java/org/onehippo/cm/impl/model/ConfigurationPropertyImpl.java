package org.onehippo.cm.impl.model;

import org.onehippo.cm.api.model.ConfigurationProperty;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueFormatException;
import org.onehippo.cm.api.model.ValueType;

public class ConfigurationPropertyImpl extends ConfigurationItemImpl implements ConfigurationProperty {


    private PropertyType type;
    private ValueType valueType;
    private Value value;
    private Value[] values;

    @Override
    public PropertyType getType() {
        return type;
    }

    public void setType(final PropertyType type) {
        this.type = type;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(final ValueType valueType) {
        this.valueType = valueType;
    }

    @Override
    public Value getValue() throws ValueFormatException {
        return value;
    }

    public void setValue(final Value value) {
        this.value = value;
    }

    @Override
    public Value[] getValues() throws ValueFormatException {
        return values;
    }

    public void setValues(final Value[] values) {
        this.values = values;
    }
}
