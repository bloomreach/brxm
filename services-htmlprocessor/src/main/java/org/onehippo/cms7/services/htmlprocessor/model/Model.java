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
package org.onehippo.cms7.services.htmlprocessor.model;

import java.io.Serializable;

public interface Model<T> extends Serializable {

    T get();

    void set(final T value);

    static <T> Model<T> of(final T o) {
        return new SimpleModel<>(o);
    }

    class SimpleModel<T> implements Model<T> {

        T value;

        SimpleModel(final T o) {
            value = o;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public void set(final T value) {
            this.value = value;
        }

    }
}



