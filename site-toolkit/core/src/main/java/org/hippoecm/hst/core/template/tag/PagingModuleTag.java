package org.hippoecm.hst.core.template.tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.module.Module;
import org.hippoecm.hst.core.template.module.PageModule;
import org.hippoecm.hst.core.template.module.PaginatedDataBean;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Tag for paginated display of module data. This tag is almost identical to the module tag.
 *  The difference that a render call does not result in a call to the execute() method. Instead it
 *  gets the items of the paginated view and puts them in a bean that is made available to the
 *  pageScope.
 *   
 *
 */
public class PagingModuleTag extends ModuleTagBase {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ModuleRenderTag.class);
    
    public int doStartTag() throws JspException {       
        return EVAL_BODY_BUFFERED;
    }
    
    public int doEndTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        PageContainerModuleNode pcm = null;
        if (doExecute || doRender) {
            pcm = (PageContainerModuleNode) request.getAttribute(HSTHttpAttributes.CURRENT_PAGE_MODULE_NAME_REQ_ATTRIBUTE);
        }
        if (doExecute) {
            doExecute(request, pcm);
        }
        
        if (doRender) {
           doRender(request, pcm);
        }
        return EVAL_PAGE;
    }
    
    /**
     * Renders the module but paginated.
     */
    protected final void doRender(HttpServletRequest request, PageContainerModuleNode pcm) throws JspException {       
            try {
                PageModule pageModule = getPageModule();              
                pageModule.setVar(var);
                pageModule.setPageModuleNode(getPageModuleNode(request, pcm.getName()));
                pageModule.setModuleParameters(parameters);
                
                int pageSize = pageModule.getPageSize(pageContext);
                int pageNo = pageModule.getPageNumber(pageContext);
                int from = (pageNo == 0 ? 0 : (pageNo * pageSize) - 1);
                int to = pageNo * pageSize;
                
                PaginatedDataBean paginatedData = new PaginatedDataBean();
                paginatedData.setItems(pageModule.getElements(from, to));
                paginatedData.setTotalItems(pageModule.totalItems());
                paginatedData.setPageSize(pageSize);
                paginatedData.setPageNo(pageNo);
                
                pageContext.setAttribute(var, paginatedData);
                } catch (Exception e) {
                    throw new JspException(e);
                }
    }
    
    protected final PageModule getPageModule() throws Exception {
        Object o = null;
        log.info("Create instance of class " + getClassName());
        o = Class.forName(getClassName()).newInstance();
        if (!PageModule.class.isInstance(o)) {
            throw new Exception(getClassName() + " does not implement the interface " + PageModule.class.getName());
        }
        return (PageModule) o;
    }
    
    
    protected int getPageId(HttpServletRequest request, String pageRequestParameter) {      
        int pageId = 0;
        try {
            pageId = Integer.parseInt(request.getParameter(pageRequestParameter));
        } catch (NumberFormatException e) {             
            log.error("Cannot parse requestParameter " + pageRequestParameter + " value=" + request.getParameter(pageRequestParameter));
            pageId = 0;
        }
        return pageId;
    }
   
}
