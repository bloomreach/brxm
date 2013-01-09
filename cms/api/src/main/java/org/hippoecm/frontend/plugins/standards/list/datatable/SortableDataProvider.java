/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.datatable;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;

public abstract class SortableDataProvider<T> implements ISortableDataProvider<T> {
    private static final long serialVersionUID = 1L;

    private SortState state = new SortState();

    public void setSortState(ISortState state) {
        if (!(state instanceof SortState)) {
            throw new IllegalArgumentException("argument [state] must be an instance of SortState");
        }
        this.state = (SortState) state;
    }

    public SortState getSortState() {
        return state;
    }
}
