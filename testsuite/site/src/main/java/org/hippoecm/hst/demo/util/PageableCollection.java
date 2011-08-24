/*
 * Copyright 2009 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.demo.util;


import java.util.ArrayList;
import java.util.List;

/**
 * @param <T> can hold different data types
 * @see Pageable
 */
public class PageableCollection<T> extends Pageable {

    private List<T> items;

    public PageableCollection(int total) {
        super(total);
        items = new ArrayList<T>();

    }

    public PageableCollection(int total, List<T> items) {
        super(total);
        this.items = items;
    }

    public void addItem(T item) {
        items.add(item);
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }
}
