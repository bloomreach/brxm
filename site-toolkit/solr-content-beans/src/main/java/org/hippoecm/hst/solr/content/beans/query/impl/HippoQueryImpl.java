/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.solr.content.beans.query.impl;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.hippoecm.hst.solr.DocumentObjectBinder;
import org.hippoecm.hst.solr.HippoSolrManager;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryResult;
import org.hippoecm.hst.solr.content.beans.query.HippoQuery;

public class HippoQueryImpl implements HippoQuery {

    private HippoSolrManager manager;

    private SolrQuery solrQuery;

    private int limit = HippoQuery.DEFAULT_LIMIT;

    private int offset;

    public HippoQueryImpl(HippoSolrManager manager, String query) {
        this.manager = manager;
        this.solrQuery = new SolrQuery();
        if (query == null) {
            solrQuery.setQuery("*:*");
        } else {
            solrQuery.setQuery(query);
        }
    }


    @Override
    public void addScope(final String scope) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addScopes(final List<String> scopes) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addExcludedScope(final String scope) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addExcludedScopes(final List<String> scopes) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public SolrQuery getSolrQuery() {
        return solrQuery;
    }

    @Override
    public HippoQueryResult execute() throws SolrServerException {
        return execute(false);
    }

     @Override
    public HippoQueryResult execute(boolean attachProviders) throws SolrServerException {

//        solrQuery.setHighlight(true);
//
//        solrQuery.addHighlightField("*");
//
//        solrQuery.setIncludeScore(true);
//
//        solrQuery.addSortField("name", org.apache.solr.client.solrj.SolrQuery.ORDER.asc);

        solrQuery.set("start", offset);
        solrQuery.set("rows", limit);

        QueryResponse rsp = manager.getSolrServer().query(solrQuery);

        SolrDocumentList docs = rsp.getResults();

        //System.out.println(rsp.getSpellCheckResponse());
        //System.out.println(docs.size());
        if (attachProviders) {
            return new HippoQueryResultImpl(rsp, docs , new DocumentObjectBinder(),  manager.getContentBeanValueProviders() );
        }
        return new HippoQueryResultImpl(rsp, docs , new DocumentObjectBinder(), null );
    }

    @Override
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

}