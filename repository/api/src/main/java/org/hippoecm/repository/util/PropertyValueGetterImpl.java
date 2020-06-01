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

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public final class PropertyValueGetterImpl implements ValueGetter<Property, Object> {

    private final ValueGetter<Value, ?> singleValueGetter;
    private final ValueGetter<Value[], ?> multiValueGetter;

    public PropertyValueGetterImpl(ValueGetter<Value, ?> singleValueGetter, ValueGetter<Value[], ?> multiValueGetter) {
        this.singleValueGetter = singleValueGetter;
        this.multiValueGetter = multiValueGetter;
    }

    public PropertyValueGetterImpl(ValueGetter<Value, ?> singleValueGetter) {
        this(singleValueGetter, new MultiValueGetterImpl(singleValueGetter));
    }

    public PropertyValueGetterImpl() {
        this(new SingleValueGetterImpl());
    }

    public Object getValue(Property property) throws RepositoryException {
        if (property.isMultiple()) {
            return multiValueGetter.getValue(property.getValues());
        } else {
            return singleValueGetter.getValue(property.getValue());
        }
    }

}
