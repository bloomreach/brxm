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
package org.hippoecm.hst.components;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

public class PageableOverview extends GenericResourceServingHstComponent {
    
    private static final int DEFAULT_PAGE_SIZE = 1;
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
        String pageSizeStr = this.getParameter("size", request);
        int pageSize = DEFAULT_PAGE_SIZE;
        if(pageSizeStr != null) {
            try {
            pageSize = Integer.parseInt(pageSizeStr);
            } catch (NumberFormatException e) {  
            }
        } 
        
        String currentPageStr = request.getParameter("page");
        int currentPage = 1;
        if (currentPageStr != null) {
            try {
                currentPage = Integer.parseInt(currentPageStr);
            } catch (NumberFormatException e) {
            }
        }
        
        HippoBean  n = this.getContentBean(request);
        
        if(n == null) {
            return;
        }
        
        int from = (currentPage - 1) * pageSize;
        int to  = ((currentPage) * pageSize);
        
        
        request.setAttribute("parent", n.getParentBean());
        request.setAttribute("current",(n));
        
        if(n instanceof HippoFolder) {
            request.setAttribute("collections",((HippoFolder)n).getFolders());
            Object o = ((HippoFolder)n).getDocuments();
            request.setAttribute("documents",((HippoFolder)n).getDocuments(from, to));
        }
       
        int totalItems = ((HippoFolder)n).getDocumentSize();
        List<Page> pages = new ArrayList<Page>();
        if(totalItems > pageSize) {
            int nrOfPages = totalItems/pageSize;
            int i = 0;
            while(++i <= nrOfPages) {
                pages.add(new Page(i));
            }
        }
        
        request.setAttribute("pages",pages);
    }

    
    public class Page{
        
        int number;
        public Page(int number) {
            this.number = number;
        }
        
        public int getNumber(){
            return this.number;
        }
        
    }

}


  
