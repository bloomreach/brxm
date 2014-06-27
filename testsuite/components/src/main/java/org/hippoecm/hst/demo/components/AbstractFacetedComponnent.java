/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractFacetedComponnent extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(AbstractFacetedComponnent.class);
  
    public HstQuery getHstQuery(HstRequest request) {

        String query = this.getPublicRequestParameter(request, "query");
        if(query != null ) {
            query = SearchInputParsingUtils.parse(query, false);
        }
        String order = this.getPublicRequestParameter(request, "order");
        HstQuery hstQuery = null;
        if ( (query != null && !"".equals(query)) || (order != null && !"".equals(order))) {
            // there was a free text query. We need to account for this. 
            request.setAttribute("query", query);
            request.setAttribute("order", order);
            // account for the free text string
            
            try {
                HstRequestContext ctx = request.getRequestContext();
                hstQuery = ctx.getQueryManager().createQuery(ctx.getSiteContentBaseBean());
                if(query != null && !"".equals(query)) {
                    Filter f = hstQuery.createFilter();
                    Filter f1 = hstQuery.createFilter();
                    f1.addContains(".", query);
                    Filter f2 = hstQuery.createFilter();
                    f2.addContains("demosite:title", query);
                    f.addOrFilter(f1);
                    f.addOrFilter(f2);
                    hstQuery.setFilter(f);
                }
                if(order != null && !"".equals(order)) {
                    if(order.startsWith("-")) {
                        hstQuery.addOrderByDescending("demosite:"+order.substring(1));
                    } else {
                        hstQuery.addOrderByAscending("demosite:"+order);
                    }
                    
                }
                
            } catch (QueryException e) {
               log.error("QueryException:" , e);
            }
        }
        return hstQuery;
    }
    
}