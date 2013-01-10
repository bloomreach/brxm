/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;
import java.util.Date;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.components.solrutil.SolrSearchParams;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.solr.HippoSolrClient;
import org.hippoecm.hst.solr.content.beans.query.HippoQuery;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryResult;

/**
 * Example class for solr faceting. Note that this is just some example. You can set many more different fields and options, als
 */
public class SolrFaceting extends AbstractSearchComponent {

    public static final String SOLR_MODULE_NAME = "org.hippoecm.hst.solr";

    public static final int DEFAULT_PAGE_SIZE = 5;


    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {

        HippoSolrClient solrClient = HstServices.getComponentManager().getComponent(HippoSolrClient.class.getName(), SOLR_MODULE_NAME);
        SolrSearchParams params = new SolrSearchParams(request);

        params.setParamsOnRequestAttr();
        try {
            // set the free text query
            HippoQuery hippoQuery = solrClient.createQuery(params.getQuery());

            SolrQuery solrQuery = hippoQuery.getSolrQuery();

            String facetPath = request.getRequestContext().getResolvedSiteMapItem().getParameter("facetpath");
            if (facetPath != null) {
                // there is a facet path : Account for the constraints
                // the facet path looks like key/value/key/value 
                
                String[] constraints = facetPath.split("/");
                if (constraints.length % 2 != 0) {
                    log.warn("Invalid constraints because not equal number of keys and values");
                } else {
                    int i = 0;
                    while (i < constraints.length) {
                        String facetField = constraints[i];
                        String facetValue = constraints[i+1];
                        solrQuery.addFilterQuery(facetField + ":" + solrClient.getQueryParser().escape(facetValue));
                        i+=2;
                    }
                }
            }
            
            // set rows to 0 : we are not interested to get results, but to get faceted navigation
            solrQuery.setRows(0);
            solrQuery.addFacetField("brand", "categories", "title");
            solrQuery.setFacetLimit(10);

            Calendar startCal = Calendar.getInstance();
            startCal.add(Calendar.YEAR, -5);

            Date startDate = startCal.getTime();
            Date endDate = Calendar.getInstance().getTime();
            solrQuery.addDateRangeFacet("date", startDate , endDate, "+1YEAR");

            // From SOLR 3.6 and higher you can also use :
            // solrQuery.addDateRangeFacet("date", startDate , endDate, "+1YEAR, +1MONTH, +1DAY");
            // This way you can create multiple buckets for 1 date field

            final HippoQueryResult result = hippoQuery.execute();

            // we do not need to bind the beans with their providers for faceting, so no need for
            // result.bindHits()

            request.setAttribute("queryResponse", result.getQueryResponse());
            request.setAttribute("query", params.getQuery());

        } catch (SolrServerException e) {
            throw new HstComponentException(e);
        }

    }


}

