/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.query;

import java.util.List;

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.BaseFilter;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;

public interface HstQuery {

    /**
     * The default limit that is used for a HstQuery. Use {@link #setLimit(int)} if you need to override this value. 
     */
    final static int DEFAULT_LIMIT = 1000;
    
    
    void setFilter(BaseFilter filter);

    BaseFilter getFilter();

    /**
     * 
     * @return a new empty Filter
     */
    Filter createFilter();
    
    /**
     * Sets the limit of search results.
     * <b>Note</b> that setting this value very high might influence performance negatively
     * @param limit
     */
    void setLimit(int limit);
    
    /**
     * Returns the query as string. When <code>skipDefaultOrder</code> is <codee>TRUE</code>, the string representation will only include an 'order by' clause
     * when there is set one explicitly.
     * @param skipDefaultOrderBy
     * @return the query as string
     * @throws QueryException if we cannot build the query string
     */
    String getQueryAsString(boolean skipDefaultOrderBy) throws QueryException;
    
    /**
     * Returns the limit of the HstQuery. If no limit is set, it returns the default HstQuery limit {@link #DEFAULT_LIMIT}
     * @return the limit 
     */
    int getLimit();
    
    /**
     * Sets the offset to start searching from. Default offset is <code>-1</code> which means it is ignored. A negative offset will be ignored
     * @param offset
     */
    void setOffset(int offset);

    /**
     * Returns the offset of the HstQuery. If no offset is set through {@link #setOffset(int)}, the offset will be <code>-1</code> and will be ignored 
     * @return the offset 
     */
    int getOffset();
    
    /**
     * Order the object found (ascending)
     * @param fieldNameAttribute the name of the field used to sort the search result
     */
    void addOrderByAscending(String fieldNameAttribute);

    /**
     * Order the object found (descending)
     * @param fieldNameAttribute the name of the field used to sort the search result
     */

    void addOrderByDescending(String fieldNameAttribute);

    /**
     * add scopes to search in. 
     * If the exact scope is already added to exclude from the search, it is removed from the excluded list.
     * @param scopes
     */
    void addScopes(List<HippoBean> scopes);
    
    /**
     * add scopes to search in. 
     * If the exact scope is already added to exclude from the search, it is removed from the excluded list.
     * @param scopes
     */
    void addScopes(Node[] scopes);
    
    /**
     * add scopes to exclude from search. 
     * If the exact scope is already added as a scope to search in, it is removed from there
     * @param scopes
     */
    void excludeScopes(List<HippoBean> scopes);
    
    /**
     * add scopes to exclude from search. 
     * If the exact scope is already added as a scope to search in, it is removed from there
     * @param scopes
     */
    void excludeScopes(Node[] scopes);
    
    /**
     * Whether invalid scopes should be skipped, or if an invalid scope is found (jcr node is null, HippoBean is empty, etc), throw 
     * a QueryException. Default HstQuery implementation throw a QueryException when an invalid scope is encountered. If skipInvalid is set
     * to <code>true</code>, then still, when all scopes happen to be invalid, a QueryException is thrown
     * @param skipInvalidScopes is <code>true</code>, invalid scopes are ignored
     * @deprecated  since 2.25.02 : skipInvalidScopes is not used any more. You can remove invoking this method
     */
    @Deprecated
    void setSkipInvalidScopes(boolean skipInvalidScopes);
    
    /**
     * The actual execution of the HstQuery. The HstQueryResult will never contain one and the same result twice. So, if a result matches
     * the search criteria twice, it is still returned as one hit. 
     * @return <code>{@link HstQueryResult}</code>
     */
    HstQueryResult execute() throws QueryException;
}
