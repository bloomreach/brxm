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

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.hippoecm.hst.solr.DocumentObjectBinder;
import org.hippoecm.hst.solr.HippoSolrManager;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryResult;
import org.hippoecm.hst.solr.content.beans.query.HippoQuery;
import org.slf4j.LoggerFactory;

public class HippoQueryImpl implements HippoQuery {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HippoQueryImpl.class);

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
    public void setScopes(final String... scopes) {
        setScopes(scopes, true);
    }

    @Override
    public void setExcludedScopes(final String... scopes) {
        setScopes(scopes, false);
    }

    private void setScopes(final String[] scopes, boolean include) {
        if (scopes == null) {
            throw new IllegalArgumentException("varargs scopes is not allowed to be null");
        }
        String fqField ;
        if (include) {
            fqField = DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY;
        } else {
            fqField = "-"+DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY;
        }


        
        // if there is already a scopes FQ set, first remove that one
        List<String> toRemove = new ArrayList<String>();
        if (solrQuery.getFilterQueries() != null) {
            for (String fq : solrQuery.getFilterQueries()) {
                if (fq.startsWith(fqField)) {
                    toRemove.add(fq);
                }
            }
            if (!toRemove.isEmpty()) {
                log.info("The filter query for scopes or excludescopes was already set before. Removing old value");
                for (String fq : toRemove) {
                    log.info("removing scope or exclude scope '{}'", fq);
                    solrQuery.removeFilterQuery(fq);
                }
            }
        }

        StringBuilder scopeFilterQuery = new StringBuilder();
        for (String scope : scopes) {
            log.debug("Add scope to search below '{}'", scope);
            // escape chars like ':'
            String escapedScope = manager.getQueryParser().escape(scope);
            if (scopeFilterQuery.length() == 0) {
                scopeFilterQuery.append(fqField).append(":").append(escapedScope);
            } else {
                if (include) {
                    scopeFilterQuery.append(" OR ");
                } else {
                    scopeFilterQuery.append(" AND ");
                }
                scopeFilterQuery.append(fqField).append(":").append(escapedScope);
            }
        }
        if (scopeFilterQuery.length() > 0) {
           solrQuery.addFilterQuery(scopeFilterQuery.toString());
        }
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

        solrQuery.set("start", offset);
        solrQuery.set("rows", limit);

        QueryResponse rsp = manager.getSolrServer().query(solrQuery);

        SolrDocumentList docs = rsp.getResults();

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