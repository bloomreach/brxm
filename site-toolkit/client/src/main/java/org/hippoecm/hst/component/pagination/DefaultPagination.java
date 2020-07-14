/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.component.pagination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultPagination<T> extends Pagination<T> {
     
    private List<T> items;

    /**
     * Returns empty immutable collection
     *
     * @param <T> return type
     * @return empty, immutable list
     */
    public static <T> DefaultPagination<T> emptyCollection() {
        return new DefaultPagination<>(Collections.emptyList());
    }

    public DefaultPagination() {
        this(0);
    }

    public DefaultPagination(final int total) {
        this(total, new ArrayList<>());
    }

    public DefaultPagination(List<T> items) {
        this(items.size(), items);
    }

    public DefaultPagination(int total, List<T> items) {
        super(total);
        this.items = items;
    }

    public void addItem(T item) {
        items.add(item);
    }

    @Override
    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

}
