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

import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.query.reflect.QueryNode;

public interface QueryPersistService {

    /**
     * Persist a query permanently, returning an id that can be used to retrieve it later.
     *
     * @param query the query to be peristed
     * @return the id for the query, that can be used to {@link #retrieve(String)} it later.
     *
     * @throws SearchServiceException
     */
    void persist(String id, QueryNode query) throws SearchServiceException;

    /**
     * Retrieve a query by id.
     *
     * @param queryId the id of the query, as returned by an earlier invocation bo {@link #persist(String, QueryNode)}.
     * @return the {@link Query)} that was persisted earlier
     *
     * @throws SearchServiceException
     */
    QueryNode retrieve(String queryId) throws SearchServiceException;
}
