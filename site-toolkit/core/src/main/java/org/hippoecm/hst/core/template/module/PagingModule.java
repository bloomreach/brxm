package org.hippoecm.hst.core.template.module;

import java.util.List;

import javax.jcr.Node;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;

public interface PagingModule extends Module {
    public static final String PAGESIZE_CMS_PROPERTY = "pageSize";
    public static final String PAGE_PARAMETER_CMS_PROPERTY = "pageParameter";
    
    /**
     * Is called before the other methods of the module in a paging-module tag.
     * @param contex
     */
    public void prePagingRender(PageContext context);
    /**
     * Returns the items from the list that need to be displayed on a page.
     * @param from the first item in the list to be displayed on the page
     * @param to the last item in the list to be displayed on the page
     * @return
     */
    public List getElements(int from, int to);
    
    /**
     * The number of elements in the list.
     * @return
     */
    public int totalItems();
    
    int getPageSize(PageContext context);    
    public int getPageNumber(PageContext context);
    public String getPageParameter() throws TemplateException;
    
}
