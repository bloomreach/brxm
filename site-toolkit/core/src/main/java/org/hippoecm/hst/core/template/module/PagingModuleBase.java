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
package org.hippoecm.hst.core.template.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Non specific PagingModule functionality like getting the pagesize, pagenumber etc.
 * Things that should be the same for standard modules that want to enable paginated
 * browsing.
 *
 */
public abstract class PagingModuleBase extends ModuleBase implements PagingModule {
    private static final Logger log = LoggerFactory.getLogger(PagingModuleBase.class);
    private static int DEFAULT_PAGESIZE = 3;
  
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
            pageParameter = getPageParameter();
            HttpServletRequest request = (HttpServletRequest) context.getRequest();
            String pageParameterValue = request.getParameter(pageParameter);
            if (pageParameterValue == null) {
                // value is not set
                return 0;
            }
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
    
    public String getPageParameter() throws TemplateException {
        return pageContainerModule.getPropertyValue(PagingModule.PAGE_PARAMETER_CMS_PROPERTY);
    }
    
    
}
