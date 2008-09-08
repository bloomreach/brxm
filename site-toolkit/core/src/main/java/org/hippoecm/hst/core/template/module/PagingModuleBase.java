package org.hippoecm.hst.core.template.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PagingModuleBase extends ModuleBase implements PagingModule {
    private static final Logger log = LoggerFactory.getLogger(PagingModuleBase.class);
    private static int DEFAULT_PAGESIZE = 10;
  
    public int getPageSize(PageContext context) {            
        String propertyValue = null;
        try {           
            try {
                propertyValue = pageContainerModule.getPropertyValue(PagingModule.PAGESIZE_CMS_PROPERTY);
            } catch (Exception e) {
                log.error("Cannot get value", e);
                return DEFAULT_PAGESIZE;
            }
            return Integer.valueOf(propertyValue);          
        } catch (NumberFormatException e) {
            log.error("Cannot parse attribute " + PagingModule.PAGESIZE_CMS_PROPERTY + " with value " + propertyValue);
        } 
        return DEFAULT_PAGESIZE;
    }

    public int getPageNumber(PageContext context) {
        String pageParameter = null;
        try {
            pageParameter = pageContainerModule.getPropertyValue(PagingModule.PAGE_PARAMETER_CMS_PROPERTY);
            HttpServletRequest request = (HttpServletRequest) context.getRequest();
            String pageParameterValue = request.getParameter(pageParameter);
            try {
                return Integer.parseInt(pageParameterValue);
            } catch (NumberFormatException e) {
              log.error ("Cannot parse parameter " + pageParameter + " with value " + pageParameterValue);
              return 0;
            }
        } catch (TemplateException e) {
            log.error("Cannot get pageParameter " + PagingModule.PAGE_PARAMETER_CMS_PROPERTY + " from module " + pageContainerModule);
            return 0;
        }  
    }

    public void prePagingRender(PageContext context) {
         //empty 
    }
    
    
}
