package org.hippoecm.cmsprototype.frontend.plugins.list.datatable.paging;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;

public class CustomizableNavigationToolBar extends AjaxNavigationToolbar{

    private static final long serialVersionUID = 1L;
    
    public CustomizableNavigationToolBar(DataTable table) {
        super(table);
    }
    
    public CustomizableNavigationToolBar(DataTable table, String prefix, String postfix) {
        super(table);
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
    protected PagingNavigator newPagingNavigator(String navigatorId, final DataTable table)
    {
        
        return new CustomizablePagingNavigator(navigatorId, table, 
                        new CustomizablePagingLabelProvider("page",null))
            {
            private static final long serialVersionUID = 1L;

            /**
             * Implement our own ajax event handling in order to update the datatable itself, as the
             * default implementation doesn't support DataViews.
             * 
             * @see AjaxPagingNavigator#onAjaxEvent(AjaxRequestTarget)
             */
            protected void onAjaxEvent(AjaxRequestTarget target)
            {
                target.addComponent(table);
            }
        };
    }
}
