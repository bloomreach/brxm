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

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.components.solrutil.SolrSearchParams;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.solr.HippoSolrClient;
import org.hippoecm.hst.solr.content.beans.query.HippoQuery;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryResult;

public class SolrRight extends AbstractSearchComponent {

    public static final String SOLR_MODULE_NAME = "org.hippoecm.hst.solr";

    @Override
    public void doBeforeServeResource(final HstRequest request, final HstResponse response) throws HstComponentException {
        if (request.getParameter("suggestquery") == null) {
            // no query
            return;
        }

        HippoSolrClient solrClient = HstServices.getComponentManager().getComponent(HippoSolrClient.class.getName(), SOLR_MODULE_NAME);
        String suggest = request.getParameter("suggestquery");
        try {

            HippoQuery hippoQuery = solrClient.createQuery(suggest);

            // we want to get suggestions/autocompletion/didyoumean only!
            hippoQuery.getSolrQuery().setRequestHandler("suggest");
            HippoQueryResult result = hippoQuery.execute();

            // we do not need to bind the beans with their providers for faceting, so no need for
            // result.bindHits()

            // because suggestions reuse the spell check component, we can use the spell check response
            final SpellCheckResponse spellCheckResponse = result.getQueryResponse().getSpellCheckResponse();
            if (spellCheckResponse != null) {
                request.setAttribute("collated", spellCheckResponse.getCollatedResult());
                request.setAttribute("suggestions", spellCheckResponse.getSuggestions());
            }

        } catch (SolrServerException e) {
            throw new HstComponentException(e);
        }

    }

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {

        SolrSearchParams params = new SolrSearchParams(request);
        params.setParamsOnRequestAttr();

    }


}

