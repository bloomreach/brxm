/*
 *  Copyright 2008 Hippo.
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

    void setFilter(BaseFilter filter);

    BaseFilter getFilter();

    /**
     * 
     * @return a new empty Filter
     */
    Filter createFilter();
    
    /**
     * Sets the limit of search results
     * @param offset
     */
    void setLimit(int limit);

    /**
     * Sets the offset to start searching from
     * @param offset
     */
    void setOffset(int offset);

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
     * @param scopes
     */
    void addScopes(List<HippoBean> scopes);
    
    /**
     * add scopes to search in. 
     * @param scopes
     */
    void addScopes(Node[] scopes);
    
    /**
     * Whether invalid scopes should be skipped, or if an invalid scope is found (jcr node is null, HippoBean is empty, etc), throw 
     * a QueryException. Default HstQuery implementation throw a QueryException when an invalid scope is encountered. If skipInvalid is set
     * to <code>true</code>, then still, when all scopes happen to be invalid, a QueryException is thrown
     * @param skipInvalidScopes is <code>true</code>, invalid scopes are ignored
     */
    void setSkipInvalidScopes(boolean skipInvalidScopes);
    
    /**
     * The actual execution of the HstQuery. The HstQueryResult will never contain one and the same result twice. So, if a result matches
     * the search criteria twice, it is still returned as one hit. 
     * @return <code>{@link HstQueryResult}</code>
     */
    HstQueryResult execute() throws QueryException;
}
