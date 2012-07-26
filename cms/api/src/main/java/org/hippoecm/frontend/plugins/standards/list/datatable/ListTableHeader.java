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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
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
        add(getBodyContainer());

        if (!triState) {
            //Initial sorting on the "Name" column (if any)
            SortState state = (SortState)stateLocator.getSortState();
            ListColumn[] columns = (ListColumn[])dataTable.getColumns();
            for (ListColumn column : columns) {
                if (column.getRenderer() == null || column.getRenderer() instanceof NameRenderer) {
                    state.setPropertySortOrder(column.getSortProperty(), ISortState.ASCENDING);
                    break;
                }
            }
        }

        add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                SortState state = (SortState)stateLocator.getSortState();
                int dir = state.getPropertySortOrder(property);
                if (dir == ISortState.NONE) {
                    dir = ISortState.ASCENDING;
                } else if (dir == ISortState.ASCENDING) {
                    dir = ISortState.DESCENDING;
                } else if (dir == ISortState.DESCENDING) {
                    dir = ISortState.NONE;
                } else if (triState){
                    dir = ISortState.NONE;
                } else {
                    dir = ISortState.ASCENDING;
                }
                state.setPropertySortOrder(property, dir);
                target.addComponent(dataTable);
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
                    int dir = sortState.getPropertySortOrder(tableHeader.property);

                    if (dir == ISortState.ASCENDING) {
                        return "hippo-list-order-ascending";
                    } else if (dir == ISortState.DESCENDING) {
                        return "hippo-list-order-descending";
                    } else {
                        return "hippo-list-order-none";
                    }
                }
            });
        }
    };

}
