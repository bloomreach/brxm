/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.api.resource;

/**
 * Abstract {@link ValueMap} base class.
 */
public abstract class AbstractValueMap implements ValueMap {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T get(String name, Class<T> type) {
        final Object value = get(name, (T) null);

        if (value != null && type != null) {
            final T converted = AbstractResource.convertValueOfBasicType(value, type);

            if (converted != null) {
                return converted;
            }

            if (!type.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException("The type doesn't match with the value type: " + value.getClass());
            }
        }

        return (T) value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T get(String name, T defaultValue) {
        if (name == null) {
            throw new IllegalArgumentException("The name must not be a null.");
        }

        final Object value = get(name);
        return (value != null) ? (T) value : defaultValue;
    }

}
