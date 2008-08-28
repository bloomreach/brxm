/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.standardworkflow.reorder;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortableDataProvider;

public class ReorderDataProvider extends SortableDataProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private LinkedList<ListItem> listItems;

    public ReorderDataProvider(DocumentsProvider documents) {
        listItems = new LinkedList<ListItem>();
        Iterator<IModel> it = documents.iterator(0, documents.size());
        while (it.hasNext()) {
            IModel entry = it.next();
            if (entry instanceof JcrNodeModel) {
                listItems.add(new ListItem((JcrNodeModel) entry));
            }
        }
    }

    public Iterator<ListItem> iterator(int first, int count) {
        return listItems.subList(first, first + count).iterator();
    }

    public IModel model(Object object) {
        return (IModel) object;
    }

    public int size() {
        return listItems.size();
    }

    public void detach() {
        for (IModel item : listItems) {
            item.detach();
        }
    }

    public void shiftUp(ListItem item) {
        int index = listItems.indexOf(item);
        if (index > 0) {
            listItems.remove(index);
            listItems.add(--index, item);
        }
    }

    public void shiftDown(ListItem item) {
        int index = listItems.indexOf(item);
        if (index < listItems.size()) {
            listItems.remove(index);
            listItems.add(++index, item);
        }
    }

}
