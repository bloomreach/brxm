/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.model;

import org.apache.wicket.model.AbstractReadOnlyModel;

/**
 * Read-only Wicket model that accepts a lambda function to return the object.
 */
public final class ReadOnlyModel<T> extends AbstractReadOnlyModel<T> {

    private final SerializableSupplier<T> supplier;

    private ReadOnlyModel(final SerializableSupplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T getObject() {
        return supplier.get();
    }

    public static <T> ReadOnlyModel<T> of(final SerializableSupplier<T> supplier) {
        return new ReadOnlyModel<>(supplier);
    }
}
