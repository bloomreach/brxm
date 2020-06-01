/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.IPluginContext;

public class TableDefinition<T> implements IClusterable {

    private List<ListColumn<T>> columns;
    private Map<String, Comparator<T>> comparators;
    private boolean showColumnHeaders;
    private boolean sortable;

    public TableDefinition(List<ListColumn<T>> columnList) {
        this(columnList, true);
    }

    public TableDefinition(List<ListColumn<T>> columnList, boolean showColumnHeaders) {
        this(columnList, showColumnHeaders, true);
    }

    public TableDefinition(List<ListColumn<T>> columnList, boolean showColumnHeaders, boolean sortable) {
        columns = new ArrayList<>();
        comparators = new HashMap<>();
        for (ListColumn<T> column : columnList) {
            columns.add(column);
            comparators.put(column.getSortProperty(), column.getComparator());
        }
        this.showColumnHeaders = showColumnHeaders;
        this.sortable = sortable;
    }

    public void init(IPluginContext context) {
        for (ListColumn<T> column : columns) {
            column.init(context);
        }
    }

    public void destroy() {
        for (ListColumn<T> column : columns) {
            column.destroy();
        }
    }

    public Map<String, Comparator<T>> getComparators() {
        return comparators;
    }

    @SuppressWarnings("unchecked")
    public List<ListColumn<T>> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    public boolean showColumnHeaders() {
        return showColumnHeaders;
    }

    public boolean isSortable() {
        return sortable;
    }
}
