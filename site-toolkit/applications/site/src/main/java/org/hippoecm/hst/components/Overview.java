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

import java.util.List;

import org.hippoecm.hst.beans.GeneralPage;
import org.hippoecm.hst.beans.NewsPage;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

public class Overview extends BaseHstComponent {
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
       // System.out.println(this.getParameters(request));
        
        HippoBean hippoBean = this.getContentBean(request);

        
        // how to get a list of documents of some specific bean
        List<NewsPage> news = ((HippoFolder)hippoBean).getDocuments(NewsPage.class);
        
        // how to get a list of documents of some superclass bean
        List<GeneralPage> generalpages = ((HippoFolder)hippoBean).getDocuments(GeneralPage.class);
        
        
        if(hippoBean == null) {
            hippoBean = this.getSiteContentBaseBean(request);
        }
      
        if(hippoBean == null) {
            return;
        }
        
        request.setAttribute("parent", hippoBean.getParentBean());
        request.setAttribute("current",hippoBean);
        
        if(hippoBean instanceof HippoFolder) {
            request.setAttribute("collections", ((HippoFolder)hippoBean).getFolders());
            request.setAttribute("documents", ((HippoFolder)hippoBean).getDocuments());
        }
        
    }

 
}


  
