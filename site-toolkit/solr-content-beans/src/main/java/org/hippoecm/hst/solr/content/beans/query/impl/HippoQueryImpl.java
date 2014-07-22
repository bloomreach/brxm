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
package org.hippoecm.hst.solr.content.beans.query.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.hippoecm.hst.solr.DocumentObjectBinder;
import org.hippoecm.hst.solr.HippoSolrClient;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryResult;
import org.hippoecm.hst.solr.content.beans.query.HippoQuery;
import org.slf4j.LoggerFactory;

public class HippoQueryImpl implements HippoQuery {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HippoQueryImpl.class);

    private HippoSolrClient client;

    private SolrQuery solrQuery;


    public HippoQueryImpl(HippoSolrClient client, String query) {
        this.client = client;
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

    @Override
    public void setIncludedClasses(final boolean subTypes, final Class<?>... classes) {
        if (classes == null) {
            throw new IllegalArgumentException("varargs classes is not allowed to be null");
        }
        setClasses(classes, subTypes, true);
    }

    @Override
    public void setExcludedClasses(final boolean subTypes, final Class<?>... classes) {
        if (classes == null) {
            throw new IllegalArgumentException("varargs classes is not allowed to be null");
        }
        setClasses(classes, subTypes, false);
    }

    @Override
    public SolrQuery getSolrQuery() {
        return solrQuery;
    }

    @Override
    public HippoQueryResult execute() throws SolrServerException {

        if (solrQuery.getRows() == null) {
            // limit was not set. Set default limit
            solrQuery.setRows(HippoQuery.DEFAULT_LIMIT);
        }

        // We ALWAYS need to fetch the following STORED properties (perhaps they are missing, however we need to try to fetch them)
        // if the getFields does not start with * we need to include the fields below
        // 1. id
        // 2. HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME
        // 3. canonicalUUID
        // 4. name
        // 5. comparePath
        // hence add these fields here to be fetched
        // If all fields are already retrieved nothing changes by adding this here

        if (solrQuery.getFields() != null && !solrQuery.getFields().startsWith("*")) {
            solrQuery.addField("id");
            solrQuery.addField(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME);
            solrQuery.addField(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH);
            solrQuery.addField("name");
            solrQuery.addField("comparePath");
        }

        long start = System.currentTimeMillis();
        QueryResponse rsp = client.getSolrServer().query(solrQuery);

        try {
            log.info("Execution took: '{}' ms. SOLR query = '{}'", (System.currentTimeMillis() - start) , URLDecoder.decode(solrQuery.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            new SolrServerException(e);
        }

        return new HippoQueryResultImpl(rsp, new DocumentObjectBinder(), client);
    }


    @Override
    public void setLimit(int limit) {
        solrQuery.setRows(limit);
    }

    @Override
    public void setOffset(int offset) {
        solrQuery.setStart(offset);
    }
    
    private void setScopes(final String[] scopes, boolean include) {
        String fqField ;
        if (include) {
            fqField = DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY;
        } else {
            fqField = "-"+DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY;
        }

        // if there is already a scopes FQ for fqField set, first remove that one
        List<String> toRemove = new ArrayList<String>();
        if (solrQuery.getFilterQueries() != null) {
            for (String fq : solrQuery.getFilterQueries()) {
                if (fq.startsWith(fqField)) {
                    toRemove.add(fq);
                }
            }
            if (!toRemove.isEmpty()) {
                log.warn("The filter query for fqField '{}' for scopes or excludescopes was already set before. Removing old value", fqField);
                for (String fq : toRemove) {
                    log.warn("removing scope or exclude scope '{}'", fq);
                    solrQuery.removeFilterQuery(fq);
                }
            }
        }

        StringBuilder scopeFilterQuery = new StringBuilder();
        for (String scope : scopes) {
            log.debug("Add scope to search below '{}'", scope);
            // escape chars like ':'
            String escapedScope = client.getQueryParser().escape(scope);
            if (scopeFilterQuery.length() == 0) {
                scopeFilterQuery.append(fqField).append(":").append(escapedScope);
            } else {
                if (include) {
                    // inclusions must be OR-ed
                    scopeFilterQuery.append(" OR ");
                } else {
                    // exclusion must be AND-ed
                    scopeFilterQuery.append(" AND ");
                }
                scopeFilterQuery.append(fqField).append(":").append(escapedScope);
            }
        }
        if (scopeFilterQuery.length() > 0) {
            solrQuery.addFilterQuery(scopeFilterQuery.toString());
        }
    }


    private void setClasses(final Class<?>[] classes, boolean subTypes, boolean include) {
        String fqField ;
        if (subTypes) {
            fqField = DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_HIERARCHY;
        } else {
            fqField = DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME;
        }
        if (!include) {
            fqField = "-"+fqField;
        } 
        // if there is already a classes FQ for fqField set, first remove that one
        List<String> toRemove = new ArrayList<String>();
        if (solrQuery.getFilterQueries() != null) {
            for (String fq : solrQuery.getFilterQueries()) {
                if (fq.startsWith(fqField)) {
                    toRemove.add(fq);
                }
            }
            if (!toRemove.isEmpty()) {
                log.warn("The filter query for fqField '{}' for includedClasses or excludedClasses was already set before. Removing old value", fqField);
                for (String fq : toRemove) {
                    log.warn("removing includedClasses or excludedClasses scope '{}'", fq);
                    solrQuery.removeFilterQuery(fq);
                }
            }
        }
        
        StringBuilder classesFilterQuery = new StringBuilder();
        for (Class<?> clazz : classes) {
            log.debug("Add clazz to search query '{}'", clazz.getName());
            // escape chars like ':'
            if (classesFilterQuery.length() == 0) {
                classesFilterQuery.append(fqField).append(":").append(clazz.getName());
            } else {
                if (include) {
                    // inclusions must be OR-ed
                    classesFilterQuery.append(" OR ");
                } else {
                    // exclusion must be AND-ed
                    classesFilterQuery.append(" AND ");
                }
                classesFilterQuery.append(fqField).append(":").append(clazz.getName());
            }
        }
        if (classesFilterQuery.length() > 0) {
            solrQuery.addFilterQuery(classesFilterQuery.toString());
        }
    }


}