/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components.paging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * DefaultPagination
 *
 * @version $Id$
 */
public class DefaultPagination<T extends HippoBean> extends Pageable<T> {


    private List<T> items;

    @SuppressWarnings({"RawUseOfParameterizedType", "rawtypes", "StaticVariableOfConcreteClass"})
    private static final Pageable EMPTY_IMMUTABLE = new DefaultPagination(0, true);

    /**
     * Returns empty immutable collection
     *
     * @param <E> return type
     * @return empty, immutable list
     */
    @SuppressWarnings({"unchecked"})
    public static <T extends HippoBean> DefaultPagination<T> emptyCollection() {
        return (DefaultPagination<T>) EMPTY_IMMUTABLE;
    }

    private DefaultPagination(final int total, final boolean empty) {
        super(total);
        if (empty) {
            items = Collections.emptyList();
        }
    }

    public DefaultPagination(final int total) {
        super(total);
        items = new ArrayList<>();
    }

    public DefaultPagination(List<T> items) {
        super(items.size());
        this.items = items;
    }

    public DefaultPagination(List<T> items, int total) {
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


