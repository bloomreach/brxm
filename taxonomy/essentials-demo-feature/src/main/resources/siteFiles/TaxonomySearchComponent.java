/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package {{componentsPackage}};

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.repository.util.DateTools;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomySearchComponent extends BaseHstComponent {

    private static final Logger log = LoggerFactory.getLogger(TaxonomySearchComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        String query = getPublicRequestParameter(request, "query");


        if (query != null && !"".equals(query)) {
            // get taxonomy filter which is the OR filter
            Filter taxonomyFilter = getTaxonomyFilter(request, query);

            // do the search
            try {
                final HippoBean siteContentBaseBean = request.getRequestContext().getSiteContentBaseBean();
                HstQuery hstQuery = request.getRequestContext().getQueryManager().createQuery(siteContentBaseBean);

                Filter filter = hstQuery.createFilter();
                filter.addContains(".", query);

                filter.addOrFilter(taxonomyFilter);

                hstQuery.setFilter(filter);

                HstQueryResult queryResult = hstQuery.execute();

                HippoBeanIterator beans = queryResult.getHippoBeans();
                List<HippoBean> hits = new ArrayList<>();
                while (beans.hasNext()) {
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
                throw new HstComponentException("Exception happened for query : " + e.getMessage(), e);
            }
        }


    }


    public Filter getTaxonomyFilter(HstRequest request, String query){
        // TODO Also search in possible translations & synonyms
        String xpath = "//element(*, "+ TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY+")[jcr:contains(., '"+query+"')]";
        try {
            Query q = request.getRequestContext().getSession().getWorkspace().getQueryManager().createQuery(xpath, "xpath");
            QueryResult result = q.execute();
            
            List<String> keys = new ArrayList<>();
            
            NodeIterator nodes = result.getNodes();
            while(nodes.hasNext()) {
                Node node = nodes.nextNode();
                if(node == null) {
                    continue;
                }
                keys.add(node.getProperty(TaxonomyNodeTypes.HIPPOTAXONOMY_KEY).getString());
            }
            
            Filter taxonomyFilter = new FilterImpl(request.getRequestContext().getSession(), DateTools.Resolution.DAY);
            for(String uuid : keys) {
                Filter f = new FilterImpl(request.getRequestContext().getSession(), DateTools.Resolution.DAY);
                try {
                    f.addEqualTo(TaxonomyNodeTypes.HIPPOTAXONOMY_KEYS, uuid);
                } catch (FilterException e) {
                    log.error("Invalid filter: ", e);
                }
                taxonomyFilter.addOrFilter(f);
            }
            return taxonomyFilter;
            
        } catch (RepositoryException e) {
            log.error("Error creating taxonomy filter", e);
        }
        
        return null;
    }
}
