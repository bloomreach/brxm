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

import java.util.Calendar;

import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;

public class ValueImpl implements Value {

    private Object value;
    private ValueType valueType;

    public ValueImpl(final byte[] value) {
        this.value = value;
        this.valueType = ValueType.BINARY;
    }

    public ValueImpl(final Boolean value) {
        this.value = value;
        this.valueType = ValueType.BOOLEAN;
    }

    public ValueImpl(final Double value) {
        this.value = value;
        this.valueType = ValueType.DOUBLE;
    }

    public ValueImpl(final Integer value) {
        this.value = value;
        this.valueType = ValueType.LONG;
    }

    public ValueImpl(final String value) {
        this.value = value;
        this.valueType = ValueType.STRING;
    }

    public ValueImpl(final Calendar value) {
        this.value = value;
        this.valueType = ValueType.DATE;
    }

    @Override
    public Object getObject() {
        return value;
    }

    @Override
    public String getString() {
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
        return false;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

}
