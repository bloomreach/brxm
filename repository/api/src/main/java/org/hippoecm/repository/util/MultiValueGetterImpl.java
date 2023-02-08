/*
 * Copyright 2011-2023 Bloomreach
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

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.List;

final class MultiValueGetterImpl implements ValueGetter<Value[], List<?>> {

    private final ValueGetter<Value, ?> singleValueGetter;

    public MultiValueGetterImpl(ValueGetter<Value, ?> singleValueGetter) {
        this.singleValueGetter = singleValueGetter;
    }

    @Override
    public List<?> getValue(Value[] values) throws RepositoryException {
        if (values != null && values.length > 0) {
            final List<Object> result = new ArrayList<>(values.length);
            for (Value each : values) {
                result.add(singleValueGetter.getValue(each));
            }
            return result;
        } else {
            return null;
        }
    }
}
