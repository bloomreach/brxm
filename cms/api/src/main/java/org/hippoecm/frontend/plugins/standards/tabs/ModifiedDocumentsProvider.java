/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;

class ModifiedDocumentsProvider implements ISortableDataProvider {

    private ISortState state = new SortState();
    private List<JcrNodeModel> modifiedDocuments;

    public ModifiedDocumentsProvider(List<JcrNodeModel> modifiedDocuments) {
        this.modifiedDocuments = modifiedDocuments;
    }

    public Iterator iterator(long first, long count) {
        return modifiedDocuments.subList((int) first, (int) (first + count)).iterator();
    }

    public long size() {
        return modifiedDocuments.size();
    }

    public IModel model(Object object) {
        return (IModel) object;
    }

    public void setModifiedDocuments(final List<JcrNodeModel> modifiedDocuments) {
        this.modifiedDocuments = modifiedDocuments;
    }

    public ISortState getSortState() {
        return state;
    }

    public void setSortState(ISortState state) {
        this.state = state;
    }

    public void detach() {
        if (modifiedDocuments != null) {
            for (JcrNodeModel model : modifiedDocuments) {
                if (model != null) {
                    model.detach();
                }
            }
        }
    }
}
