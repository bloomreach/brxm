/*
 * Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.util;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static javax.jcr.PropertyType.nameFromValue;

final class SingleValueGetterImpl implements ValueGetter<Value, Object> {

    @Override
    public Object getValue(Value value) throws RepositoryException {
        if (value != null) {
            int type = value.getType();
            if (SINGLE_VALUE_GETTER_MAP.containsKey(type)) {
                final ValueGetter<Value, ?> getter = SINGLE_VALUE_GETTER_MAP.get(type);
                return getter.getValue(value);
            } else {
                throw new RepositoryException(format("Type %s is not supported", nameFromValue(type)));
            }
        } else {
            return null;
        }
    }

    private static final Map<Integer, ValueGetter<Value, Object>> SINGLE_VALUE_GETTER_MAP = new HashMap<Integer, ValueGetter<Value, Object>>() {{
        put(PropertyType.BINARY, new ValueGetter<Value, Object>() {
            @Override
            public Object getValue(Value value) throws RepositoryException {
                return value.getBinary();
            }
        });
        put(PropertyType.BOOLEAN, new ValueGetter<Value, Object>() {
            @Override
            public Object getValue(Value value) throws RepositoryException {
                return value.getBoolean();
            }
        });
        put(PropertyType.DATE, new ValueGetter<Value, Object>() {
            @Override
            public Object getValue(Value value) throws RepositoryException {
                return value.getDate();
            }
        });
        put(PropertyType.DECIMAL, new ValueGetter<Value, Object>() {
            @Override
            public Object getValue(Value value) throws RepositoryException {
                return value.getDecimal();
            }
        });
        put(PropertyType.DOUBLE, new ValueGetter<Value, Object>() {
            @Override
            public Object getValue(Value value) throws RepositoryException {
                return value.getDouble();
            }
        });
        put(PropertyType.LONG, new ValueGetter<Value, Object>() {
            @Override
            public Object getValue(Value value) throws RepositoryException {
                return value.getLong();
            }
        });

        final ValueGetter<Value, Object> stringValueGetter = new ValueGetter<Value, Object>() {
            @Override
            public Object getValue(Value value) throws RepositoryException {
                return value.getString();
            }
        };
        put(PropertyType.STRING, stringValueGetter);
        put(PropertyType.NAME, stringValueGetter);
        put(PropertyType.PATH, stringValueGetter);
        put(PropertyType.REFERENCE, stringValueGetter);
        put(PropertyType.WEAKREFERENCE, stringValueGetter);
    }};

}
