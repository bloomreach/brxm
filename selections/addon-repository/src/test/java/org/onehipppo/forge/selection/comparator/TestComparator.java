/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehipppo.forge.selection.comparator;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.plugin.sorting.IListItemComparator;
import org.onehippo.forge.selection.frontend.plugin.sorting.SortBy;
import org.onehippo.forge.selection.frontend.plugin.sorting.SortOrder;

public class TestComparator implements IListItemComparator {
    
    private SortBy sortBy = SortBy.label;
    private SortOrder sortOrder = SortOrder.ascending;
    
    @Override
    public void setConfig(final IPluginConfig config) {
        throw new UnsupportedOperationException("not implementing deprecated method");
    }

    @Override
    public void setSortOptions(final SortBy sortBy, final SortOrder sortOrder) {
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
    }

    @Override
    public int compare(final ListItem o1, final ListItem o2) {
        if (sortBy.equals(SortBy.label)) {
            if (sortOrder.equals(SortOrder.ascending)) {
                return (o1.getLabel().compareTo(o2.getLabel()));
            } else {
                return (o2.getLabel().compareTo(o1.getLabel()));
            }
        } else {
            if (sortOrder.equals(SortOrder.ascending)) {
                return (o1.getKey().compareTo(o2.getKey()));
            } else {
                return (o2.getKey().compareTo(o1.getKey()));
            }
        }
    }
}
