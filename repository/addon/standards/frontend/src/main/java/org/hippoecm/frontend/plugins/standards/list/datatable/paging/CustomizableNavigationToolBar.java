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
package org.hippoecm.frontend.plugins.standards.list.datatable.paging;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.model.Model;

public class CustomizableNavigationToolBar extends AbstractToolbar{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public CustomizableNavigationToolBar(DataTable table, int viewSize) {
        this(table, null, null, viewSize);
     }

    public CustomizableNavigationToolBar(DataTable table, String prefix, String postfix, int viewSize) {
        super(table);

        WebMarkupContainer span = new WebMarkupContainer("span");
        add(span);
        span.add(new AttributeModifier("colspan", true, new Model(String
                .valueOf(table.getColumns().length))));

        PagingNavigator pagingNavigator = newPagingNavigator("navigator", table, prefix, postfix);
        // FIXME: viewsize can be set after onBeforeRender, not yet here
        // pagingNavigator.getPagingNavigation().setViewSize(viewSize);
        span.add(pagingNavigator);
        span.add(newNavigatorLabel("navigatorLabel", table));
    }

    /**
     * Factory method used to create the navigator label that will be used by the datatable
     *
     * @param navigatorId
     *            component id navigator label should be created with
     * @param table
     *            dataview used by datatable
     * @return navigator label that will be used to navigate the data table
     *
     */
    protected WebComponent newNavigatorLabel(String navigatorId, final DataTable table)
    {
        WebComponent w = new NavigatorLabel(navigatorId, table);
        return  w;
    }


    /**
     * Factory method used to create the paging navigator that will be used by the datatable.
     *
     * @param navigatorId
     *            component id the navigator should be created with
     * @param table
     *            dataview used by datatable
     * @return paging navigator that will be used to navigate the data table
     */
    protected PagingNavigator newPagingNavigator(String navigatorId, final DataTable table, String prefix, String postfix)
    {
        return new CustomizablePagingNavigator(navigatorId, table,
                        new CustomizablePagingLabelProvider(prefix,postfix))
            {
            private static final long serialVersionUID = 1L;

            /**
             * Implement our own ajax event handling in order to update the datatable itself, as the
             * default implementation doesn't support DataViews.
             *
             * @see AjaxPagingNavigator#onAjaxEvent(AjaxRequestTarget)
             */
            @Override
            protected void onAjaxEvent(AjaxRequestTarget target)
            {
                target.addComponent(table);
            }
        };
    }

    /**
     * Hides this toolbar when there is only one page in the table
     *
     * @see org.apache.wicket.Component#isVisible()
     */
    @Override
    public boolean isVisible()
    {
        return getTable().getPageCount() > 1;
    }
}
