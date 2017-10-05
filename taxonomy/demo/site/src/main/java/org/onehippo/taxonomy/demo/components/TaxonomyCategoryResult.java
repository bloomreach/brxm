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

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyManager;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyCategoryResult extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(TaxonomyCategoryResult.class);

 
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        TaxonomyManager taxonomyManager = HstServices.getComponentManager().getComponent(TaxonomyManager.class.getName());

        String taxonomy = getComponentParameter("root");
        String relPath = getComponentParameter("path");
        
        if(taxonomy == null || taxonomyManager.getTaxonomies().getTaxonomy(taxonomy) == null) {
            request.setAttribute("error", "Cannot find taxonomy " + taxonomy);
        } else {
            Taxonomy tax = taxonomyManager.getTaxonomies().getTaxonomy(taxonomy);
            if(relPath == null || tax.getCategory(relPath) == null){
                request.setAttribute("error", "Cannot find taxonomy term for relPath " + relPath);
            } else {
                Category category = tax.getCategory(relPath);
                /*
                 * Search for documents having the key of the selected category,
                 * using the hippotaxonomy:keys property.
                 */ 
                String key = category.getKey();
                HippoBean root = request.getRequestContext().getSiteContentBaseBean();
                try {
                    
                    HstQuery query = request.getRequestContext().getQueryManager().createQuery(root);
                    Filter filter = query.createFilter();
                    // only documents having at least one "hippotaxonomy:keys" = key
                    filter.addEqualTo(TaxonomyNodeTypes.HIPPOTAXONOMY_KEYS, key);
                    query.setFilter(filter);
                    HstQueryResult result = query.execute();
                    HippoBeanIterator beans = result.getHippoBeans();
                    List<HippoBean> documents = new ArrayList<>();
                    while(beans.hasNext()) {
                        HippoBean bean =  beans.nextHippoBean();
                        if(bean == null) {
                            continue;
                        }
                        documents.add(bean);
                    }
                    request.setAttribute("category", category);
                    request.setAttribute("documents", documents);
                } catch (QueryException e) {
                    log.error("Invalid query", e);
                }
                
            }
        }
    }
}