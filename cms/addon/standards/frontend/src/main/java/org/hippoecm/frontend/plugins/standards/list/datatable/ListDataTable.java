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
package org.hippoecm.frontend.plugins.standards.list.datatable;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;

public class ListDataTable extends DataTable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public ListDataTable(String id, TableDefinition tableDefinition, ISortableDataProvider dataProvider,
            int rowsPerPage, final boolean triState) {
        super(id, tableDefinition.getColumns(), dataProvider, rowsPerPage);
        setOutputMarkupId(true);
        setVersioned(false);

        if (tableDefinition.showColumnHeaders()) {
            addTopToolbar(new AjaxFallbackHeadersToolbar(this, dataProvider) {
                private static final long serialVersionUID = 1L;

                @Override
                protected WebMarkupContainer newSortableHeader(String borderId, String property,
                        ISortStateLocator locator) {
                    return new ListTableHeader(borderId, property, locator, ListDataTable.this, triState);
                }
            });
        }
        addBottomToolbar(new ListNavigationToolBar(this));
    }

    @Override
    protected Item newRowItem(String id, int index, final IModel model) {
        OddEvenItem item = new OddEvenItem(id, index, model);

        if (getModel().equals(model)) {
            item.add(new AttributeAppender("class", new Model("selected"), " "));
        }

        item.add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                AbstractListingPlugin plugin = (AbstractListingPlugin) findParent(AbstractListingPlugin.class);
                plugin.selectionChanged(model);
            }
        });

        return item;
    }

}
