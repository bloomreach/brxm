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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.model.Model;

public class ListNavigationToolBar extends AbstractToolbar {

    private static final long serialVersionUID = 1L;

    public ListNavigationToolBar(DataTable table, IPagingDefinition pagingDefinition) {
        super(table);

        WebMarkupContainer span = new WebMarkupContainer("span");
        add(span);
        span.add(new AttributeModifier("colspan", true, new Model(String.valueOf(table.getColumns().size()))));

        PagingNavigator pagingNavigator = newPagingNavigator("navigator", table, pagingDefinition);
        span.add(pagingNavigator);
        span.add(new NavigatorLabel("navigatorLabel", table));
    }

    @Override
    public boolean isVisible() {
        return getTable().getPageCount() > 1;
    }

    private PagingNavigator newPagingNavigator(String navigatorId, final DataTable table,
            IPagingDefinition pagingDefinition) {
        return new ListPagingNavigator(navigatorId, table, new ListPagingLabelProvider(), pagingDefinition) {
            private static final long serialVersionUID = 1L;

            /**
             * Implement our own ajax event handling in order to update the datatable itself, as the
             * default implementation doesn't support DataViews.
             *
             * @see AjaxPagingNavigator#onAjaxEvent(AjaxRequestTarget)
             */
            @Override
            protected void onAjaxEvent(AjaxRequestTarget target) {
                target.add(table);
            }
        };
    }

}
