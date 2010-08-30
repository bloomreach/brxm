/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoFacetChildNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.utils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetedRight extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(FacetedRight.class);
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
       
        HippoFacetNavigationBean facNavBean = null;
       
        String query = this.getPublicRequestParameter(request, "query");
        
        if (query != null && !"".equals(query)) {
            // there was a free text query. We need to account for this. 
            request.setAttribute("query", query);
            request.setAttribute("queryString", "?query=" + query);
            // account for the free text string
        }
        
        facNavBean = BeanUtils.getFacetNavigationBean(request, query, getObjectConverter());

        request.setAttribute("facetnav", facNavBean);
        
        if (facNavBean instanceof HippoFacetChildNavigationBean) {
            request.setAttribute("childNav", "true");
        }
    }
   
}