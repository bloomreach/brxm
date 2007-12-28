package org.hippoecm.cmsprototype.frontend.plugins.list.datatable.paging;

import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigation;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigationBehavior;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigationLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.IPagingLabelProvider;

public class CustomizablePagingNavigation extends AjaxPagingNavigation{

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param id
     *            See Component
     * @param pageable
     *            The underlying pageable component to navigate
     * @param labelProvider
     *            The label provider for the text that the links should be displaying.
     */
    public CustomizablePagingNavigation(final String id, final IPageable pageable, final IPagingLabelProvider labelProvider)
    {
        super(id, pageable, labelProvider);
    }

    /**
     * Factory method for creating ajaxian page number links.
     * 
     * @param id
     *            link id
     * @param pageable
     *            the pageable
     * @param pageIndex
     *            the index the link points to
     * @return the ajaxified page number link.
     */
    protected Link newPagingNavigationLink(String id, IPageable pageable, int pageIndex)
    {
        return new AjaxPagingNavigationLink(id, pageable, pageIndex);
    }
}
