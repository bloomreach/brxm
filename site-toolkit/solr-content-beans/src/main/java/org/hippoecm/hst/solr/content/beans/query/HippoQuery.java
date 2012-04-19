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
package org.hippoecm.hst.solr.content.beans.query;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;

public interface HippoQuery {

    /**
     * The default limit that is used for a HstQuery. Use {@link #setLimit(int)} if you need to override this value.
     */
    final static int DEFAULT_LIMIT = 100;

    /**
     * Sets the limit of search results.
     * <b>Note</b> that setting this value very high might influence performance negatively
     * If not set, a default limit of 100 is used
     * @param limit
     */
    void setLimit(int limit);

    /**
     * Sets the offset to start searching from. Default offset is <code>-1</code> which means it is ignored. A negative offset will be ignored
     * @param offset
     */
    void setOffset(int offset);

    /**
     * add scope to search below : The scope is an absolute path like /foo/bar/lux or http://www.example.com/news
     * @param scope
     */
    void addScope(String scope);
    
    /**
     * add scopes to search below : The scope is an absolute path like /foo/bar/lux or http://www.example.com/news
     * @param scopes
     */
    void addScopes(List<String> scopes);

    /**
     * add scope to exclude from search: The scope is an absolute path like /foo/bar/lux or http://www.example.com/news
     * @param scope
     */

    void addExcludedScope(String scope);

    /**
     * add scopes to exclude from search: The scope is an absolute path like /foo/bar/lux or http://www.example.com/news
     * @param scopes
     */
    void addExcludedScopes(List<String> scopes);

    /**
     * Returns the {@link SolrQuery}. The already set constraints, like scopes and alike
     * are already present on this query
     * @return the {@link SolrQuery} to be executed
     */
    SolrQuery getSolrQuery();
    
    /**
     * The execution of the query without having providers attached
     * @return <code>{@link HippoQueryResult}</code>
     * @throws SolrServerException
     */
    HippoQueryResult execute() throws SolrServerException;

    /**
     * The actual execution of the HstQuery. The HippoQueryResult will never contain one and the same result twice. So, if a result matches
     * the search criteria twice, it is still returned as one hit.
     *
     * @param attachProviders whether the results should get there providers from {@link org.hippoecm.hst.solr.HippoSolrManager#getContentBeanValueProviders()}
     *                        attached
     * @return  <code>{@link HippoQueryResult}</code>
     * @throws SolrServerException
     */
    HippoQueryResult execute(boolean attachProviders) throws SolrServerException;

}
