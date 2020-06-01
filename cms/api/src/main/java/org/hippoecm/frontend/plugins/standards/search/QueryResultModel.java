/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.search;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.session.UserSession;

import static org.hippoecm.frontend.plugins.standards.search.GeneralSearchBuilder.TEXT_QUERY_NAME;

public class QueryResultModel extends LoadableDetachableModel<QueryResult> {

    public static final int LIMIT = 100;

    private final String query;
    private final int queryLimit;

    public QueryResultModel(final String query, final int limit) {
        this.query = query;
        this.queryLimit = limit;
    }

    @Override
    protected QueryResult load() {
        QueryResult result = null;
        try {
            final QueryManager queryManager = UserSession.get().getQueryManager();
            final Query q = queryManager.createQuery(query, Query.XPATH);
            if (queryLimit > 0) {
                q.setLimit(queryLimit);
            } else {
                q.setLimit(LIMIT);
            }

            final long start = System.currentTimeMillis();
            result = q.execute();
            final long executionTime = System.currentTimeMillis() - start;

            if (TextSearchBuilder.log.isDebugEnabled()) {
                TextSearchBuilder.log.debug("Executing search query[{}]: {} took {}ms", TEXT_QUERY_NAME, query,
                        executionTime);
            } else {
                TextSearchBuilder.log.info("Executing search query[{}]: took {}ms", TEXT_QUERY_NAME, executionTime);
            }
        } catch (RepositoryException e) {
            TextSearchBuilder.log.error("Error executing query[{}]: {}", TEXT_QUERY_NAME, query, e);
        }
        return result;
    }
}
