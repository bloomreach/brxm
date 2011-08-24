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

import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.beans.ProductBean;
import org.hippoecm.hst.utils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetedDetail extends AbstractFacetedComponnent {

    public static final Logger log = LoggerFactory.getLogger(FacetedDetail.class);
  
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        HstQuery hstQuery = getHstQuery(request);
        
        ProductBean product = BeanUtils.getFacetedNavigationResultDocument(request, hstQuery, getObjectConverter(), ProductBean.class);
        
        request.setAttribute("document", product);
        
    }

       
}