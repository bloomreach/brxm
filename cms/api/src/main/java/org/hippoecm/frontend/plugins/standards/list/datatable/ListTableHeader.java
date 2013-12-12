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

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.resolvers.NameRenderer;

public class ListTableHeader extends Border {
    private static final long serialVersionUID = 1L;

    private String property;
    private ISortStateLocator stateLocator;

    public ListTableHeader(String id, final String property, final ISortStateLocator stateLocator,
            final DataTable dataTable, final boolean triState) {
        super(id);
        this.property = property;
        this.stateLocator = stateLocator;

        add(new CssModifier(this));

        if (!triState) {
            //Initial sorting on the "Name" column (if any)
            SortState state = (SortState)stateLocator.getSortState();
            List<? extends IColumn> columns = dataTable.getColumns();
            for (IColumn column : columns) {
                ListColumn<?> listColumn = (ListColumn) column;
                if (listColumn.getRenderer() == null || listColumn.getRenderer() instanceof NameRenderer) {
                    state.setPropertySortOrder(listColumn.getSortProperty(), SortOrder.ASCENDING);
                    break;
                }
            }
        }

        add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                SortState state = (SortState) stateLocator.getSortState();
                SortOrder dir = state.getPropertySortOrder(property);
                if (dir == SortOrder.NONE.NONE) {
                    dir = SortOrder.ASCENDING.ASCENDING;
                } else if (dir == SortOrder.ASCENDING.ASCENDING) {
                    dir = SortOrder.DESCENDING.DESCENDING;
                } else if (dir == SortOrder.DESCENDING) {
                    dir = SortOrder.NONE.NONE;
                } else if (triState) {
                    dir = SortOrder.NONE.NONE;
                } else {
                    dir = SortOrder.ASCENDING;
                }
                state.setPropertySortOrder(property, dir);
                target.add(dataTable);
            }
        });
    }

    private static class CssModifier extends AttributeModifier {
        private static final long serialVersionUID = 1L;

        public CssModifier(final ListTableHeader tableHeader) {
            super("class", true, new AbstractReadOnlyModel() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    ISortState sortState = tableHeader.stateLocator.getSortState();
                    SortOrder dir = sortState.getPropertySortOrder(tableHeader.property);

                    if (dir == SortOrder.ASCENDING) {
                        return "hippo-list-order-ascending";
                    } else if (dir == SortOrder.DESCENDING) {
                        return "hippo-list-order-descending";
                    } else {
                        return "hippo-list-order-none";
                    }
                }
            });
        }
    };

}
