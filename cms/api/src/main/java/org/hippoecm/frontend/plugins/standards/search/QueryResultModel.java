/*
 *  Copyright 2010 Hippo.
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
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoQuery;

public class QueryResultModel extends LoadableDetachableModel<QueryResult> {

    private static final long serialVersionUID = 1L;

    public static final int LIMIT = 100;

    private final String query;
    private int limit;

    public QueryResultModel(String query, int limit) {
        this.query = query;
        this.limit = limit;
    }

    @Override
    protected QueryResult load() {
        QueryResult result = null;
        javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
        try {
            QueryManager queryManager = ((UserSession) Session.get()).getQueryManager();
            HippoQuery hippoQuery = (HippoQuery) queryManager.createQuery(query, "xpath");
            session.refresh(true);
            if (limit > 0 && limit < LIMIT) {
                hippoQuery.setLimit(limit);
            } else {
                hippoQuery.setLimit(LIMIT);
            }

            long start = System.currentTimeMillis();
            result = hippoQuery.execute();
            long end = System.currentTimeMillis();
            TextSearchBuilder.log.info("Executing search query: " + TextSearchBuilder.TEXT_QUERY_NAME + " took " + (end - start) + "ms");
        } catch (RepositoryException e) {
            TextSearchBuilder.log.error("Error executing query[" + TextSearchBuilder.TEXT_QUERY_NAME + "]: " + query, e);
        }
        return result;
    }
}
