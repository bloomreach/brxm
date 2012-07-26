/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.standards.tabs;

import java.util.Iterator;
import java.util.List;
import javax.jcr.Node;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModifiedDocumentsProvider implements ISortableDataProvider {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ModifiedDocumentsProvider.class);

    private SortState state = new SortState();
    private List<JcrNodeModel> changedTabs;

    public ModifiedDocumentsProvider(List<JcrNodeModel> changedTabs) {
        this.changedTabs = changedTabs;
    }


    public Iterator iterator(int first, int count) {
        return changedTabs.subList(first, first + count).iterator();
    }


    public int size() {
        return changedTabs.size();
    }

    public IModel model(Object object) {
        return (IModel) object;
    }


    public ISortState getSortState() {
        return state;
    }

    public void setSortState(ISortState state) {
        this.state = (SortState) state;
    }

    public void detach() {
    }
}
