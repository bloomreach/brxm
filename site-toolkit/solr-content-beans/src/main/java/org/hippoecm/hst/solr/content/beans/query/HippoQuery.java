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

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;

public interface HippoQuery {

    /**
     * The default limit that is used for a HstQuery. Use {@link #setLimit(int)} if you need to override this value.
     */
    final static int DEFAULT_LIMIT = 10;

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
     * * sets the scopes to search from. Because it is a varargs, you can set multiple <code>scopes</code>. The <code>scopes</code> is an absolute path like /foo/bar/lux or
     * http://www.example.com/news or http: or https: etc
     * This method overrides the <code>scopes</code>s that have been set before.
     * @param scopes the varargs <code>scopes</code> to search below
     * @throws IllegalArgumentException when <code>scopes</code> is <code>null</code>
     */
    void setScopes(String... scopes) throws IllegalArgumentException;

    /**
     * sets the scopes to <b>exclude</b> from searches. Because it is a varargs, you can set multiple <code>scopes</code>s. The <code>scopes</code> is
     * an absolute path like /foo/bar/lux or http://www.example.com/news or http: or https: etc
     * This method overrides the <code>scopes</code> that have been set before.
     * @param scopes   the varargs <code>scopes</code> to NOT return search results from
     * @throws IllegalArgumentException when <code>scopes</code> is <code>null</code>
     */
    void setExcludedScopes(String... scopes) throws IllegalArgumentException;

    /**
     * <p>
     * Method to filter the search result to only <b>include</b> results where the backing {@link org.hippoecm.hst.content.beans.standard.ContentBean}s
     * are of one of the types of <code>classes</code> , or subType if <code>subTypes</code> equals <code>true</code>.
     * </p>
     * <p>
     * When this method is not called at all, the query does not filter on included classes, but returns possibly all classes (unless {@link #setExcludedClasses(boolean, Class[])})
     * is called)
     * </p>
     * @param subTypes whether the result is allowed to contain subtypes of the filter <code>classes</code>
     * @param classes the varargs <code>classes</code> that is used as filter for the search : The type (or subType) of the {@link org.hippoecm.hst.content.beans.standard.ContentBean}s 
     *                of the search result must match one the <code>classes</code>
     * @throws IllegalArgumentException when <code>classes</code> is <code>null</code>
     * @see #setExcludedClasses(boolean, Class[])
     */
    void setIncludedClasses(boolean subTypes, Class<?>... classes) throws IllegalArgumentException;

    /**
     * <p>
     * Method to filter the search result to <b>exclude</b> results where the backing {@link org.hippoecm.hst.content.beans.standard.ContentBean}s
     * are of one of the types of <code>classes</code> , or subType if <code>subTypes</code> equals <code>true</code>.
     * </p>
     * @param subTypes whether subtypes of <code>classes</code> should also be excluded
     * @param classes the varargs <code>classes</code> that is used as filter for the search : The type (or subType) of the {@link org.hippoecm.hst.content.beans.standard.ContentBean}s
     *                of the search result <b>will not</b> match one the <code>classes</code>
     * @throws IllegalArgumentException when <code>classes</code> is <code>null</code>
     * @see #setIncludedClasses(boolean, Class[])
     */
    void setExcludedClasses(boolean subTypes, Class<?>... classes) throws IllegalArgumentException;

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
