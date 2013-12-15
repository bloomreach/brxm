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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.AbstractReadOnlyModel;

public class ListTableHeader<T> extends Border {
    private static final long serialVersionUID = 1L;

    private T property;
    private ISortStateLocator<T> stateLocator;

    @Deprecated
    public ListTableHeader(String id, final T property, final ISortStateLocator<T> stateLocator,
                           final DataTable dataTable, final boolean triState) {
        this(id, property, stateLocator);
    }
    public ListTableHeader(String id, final T property, final ISortStateLocator<T> stateLocator) {
        super(id);

        this.property = property;
        this.stateLocator = stateLocator;

        add(new AttributeModifier("class", new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                switch (getSortOrder()) {
                    case ASCENDING:
                        return "hippo-list-order-ascending";
                    case DESCENDING:
                        return "hippo-list-order-descending";
                    default:
                        return "hippo-list-order-none";
                }
            }
        }));

        add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                switch (getSortOrder()) {
                    case NONE:
                        setSortOrder(SortOrder.ASCENDING);
                        break;
                    case ASCENDING:
                        setSortOrder(SortOrder.DESCENDING);
                        break;
                    case DESCENDING:
                        setSortOrder(SortOrder.NONE);
                        break;
                }
                onClick(target);
            }
        });
    }

    public void onClick(final AjaxRequestTarget target) {
    }

    private void setSortOrder(final SortOrder order) {
        stateLocator.getSortState().setPropertySortOrder(property, order);

    }

    private SortOrder getSortOrder() {
        return stateLocator.getSortState().getPropertySortOrder(property);
    }

}
