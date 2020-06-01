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

import java.util.List;

import org.onehippo.cm.model.tree.ConfigurationProperty;
import org.onehippo.cm.model.tree.PropertyKind;
import org.onehippo.cm.model.tree.ValueFormatException;
import org.onehippo.cm.model.tree.ValueType;

public class ConfigurationPropertyImpl extends ConfigurationItemImpl<DefinitionPropertyImpl>
        implements ConfigurationProperty {

    private PropertyKind kind;
    private ValueType valueType;
    private ValueImpl value;
    private List<ValueImpl> values;

    @Override
    public PropertyKind getKind() {
        return kind;
    }

    public void setKind(final PropertyKind kind) {
        this.kind = kind;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(final ValueType valueType) {
        this.valueType = valueType;
    }

    @Override
    public boolean isMultiple() {
        return getKind().isMultiple();
    }

    @Override
    public ValueImpl getValue() throws ValueFormatException {
        return value;
    }

    public void setValue(final ValueImpl value) {
        this.value = value;
    }

    @Override
    public List<ValueImpl> getValues() throws ValueFormatException {
        return values;
    }

    public void setValues(final List<ValueImpl> values) {
        this.values = values;
    }

}
