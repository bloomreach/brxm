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

import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;

import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;

public class ValueImpl implements Value {

    private final Object value;
    private final ValueType valueType;
    private final boolean isResource;

    public ValueImpl(final BigDecimal value) {
        this.value = value;
        this.valueType = ValueType.DECIMAL;
        this.isResource = false;
    }

    public ValueImpl(final Boolean value) {
        this.value = value;
        this.valueType = ValueType.BOOLEAN;
        this.isResource = false;
    }

    public ValueImpl(final byte[] value) {
        this.value = value;
        this.valueType = ValueType.BINARY;
        this.isResource = false;
    }

    public ValueImpl(final Calendar value) {
        this.value = value;
        this.valueType = ValueType.DATE;
        this.isResource = false;
    }

    public ValueImpl(final Double value) {
        this.value = value;
        this.valueType = ValueType.DOUBLE;
        this.isResource = false;
    }

    public ValueImpl(final Long value) {
        this.value = value;
        this.valueType = ValueType.LONG;
        this.isResource = false;
    }

    public ValueImpl(final String value) {
        this.value = value;
        this.valueType = ValueType.STRING;
        this.isResource = false;
    }

    public ValueImpl(final ValueType type, final String path) {
        this.value = path;
        this.valueType = type;
        this.isResource = true;
    }

    public ValueImpl(final URI value) {
        this.value = value;
        this.valueType = ValueType.URI;
        this.isResource = false;
    }

    @Override
    public Object getObject() {
        return value;
    }

    @Override
    public String getString() {
        if (isResource) {
            return value.toString();
        }
        if (valueType == ValueType.BINARY) {
            return new String((byte[]) value);
        }
        return value.toString();
    }

    @Override
    public ValueType getType() {
        return valueType;
    }

    @Override
    public boolean isResource() {
        return isResource;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

}
