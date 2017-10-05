/*
 * Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.taxonomy.demo.components;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

public class Search extends TaxonomySearchComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
        String query = getPublicRequestParameter(request, "query");
        
        
        
        if(query != null && !"".equals(query)) {
            // get taxonomy filter which is the OR filter
            Filter taxonomyFilter = getTaxonomyFilter(request, query);
            
            // do the search
            try {
                final HippoBean siteContentBaseBean = request.getRequestContext().getSiteContentBaseBean();
                HstQuery hstQuery = request.getRequestContext().getQueryManager().createQuery(siteContentBaseBean);
               
                Filter filter = hstQuery.createFilter();
                filter.addContains(".",  query);
                
                filter.addOrFilter(taxonomyFilter);
                
                hstQuery.setFilter(filter);
                
                HstQueryResult queryResult = hstQuery.execute();
                
                HippoBeanIterator beans = queryResult.getHippoBeans();
                List<HippoBean> hits = new ArrayList<>();
                while(beans.hasNext()) {
                    HippoBean bean = beans.nextHippoBean();
                    if (bean != null) {
                        hits.add(bean);
                    } else {
                       // disregard empty bean
                    }
                    
                }
                
                request.setAttribute("query", query);
                request.setAttribute("hits", hits);
                
            } catch (QueryException e) {
               throw new HstComponentException("Execption happened for query : " + e.getMessage() , e);
            }
        }
        
        
    }

    
}
