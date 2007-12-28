package org.hippoecm.cmsprototype.frontend.plugins.list.datatable.paging;

import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigation;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.IPagingLabelProvider;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigation;

public class CustomizablePagingNavigator extends AjaxPagingNavigator{

    private static final long serialVersionUID = 1L;

    public CustomizablePagingNavigator(String id, IPageable pageable, IPagingLabelProvider labelProvider) {
        super(id, pageable, labelProvider);
    }
    public CustomizablePagingNavigator(String id, IPageable pageable) {
        super(id, pageable);
    }
    
    /**
     * Create a new PagingNavigation. May be subclassed to make us of specialized PagingNavigation.
     * 
     * @param pageable
     *            the pageable component
     * @param labelProvider
     *            The label provider for the link text.
     * @return the navigation object
     */
    protected PagingNavigation newNavigation(final IPageable pageable,
            final IPagingLabelProvider labelProvider)
    {
        PagingNavigation pagingNavigation = new CustomizablePagingNavigation("navigation", pageable, labelProvider);
        pagingNavigation.setMargin(1);
        pagingNavigation.setViewSize(5);
        return pagingNavigation;
    }
    
}
