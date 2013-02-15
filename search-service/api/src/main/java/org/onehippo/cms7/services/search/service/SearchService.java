/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.service;

import org.onehippo.cms7.services.search.query.InitialQuery;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.query.reflect.QueryNode;
import org.onehippo.cms7.services.search.result.QueryResult;

/**
 * Search Service which allows to retrieve content items
 * from underlying search sources such as JCR Repository, Solr Server, etc.
 */
public interface SearchService {

    /**
     * Checking whether the underlying search service is alive
     * @return
     * @throws SearchServiceException
     */
    public boolean isAlive();

    /**
     * Creates search query
     * @return
     * @throws SearchServiceException
     */
    public InitialQuery createQuery() throws SearchServiceException;

    /**
     * Performs search query
     * @param searchQuery
     * @return
     * @throws SearchServiceException
     */
    public QueryResult search(Query searchQuery) throws SearchServiceException;

    /**
     * Returns a reflected view on a query.
     *
     * @param query
     * @return
     * @throws SearchServiceException
     */
    public QueryNode asQueryNode(Query query) throws SearchServiceException;

}
