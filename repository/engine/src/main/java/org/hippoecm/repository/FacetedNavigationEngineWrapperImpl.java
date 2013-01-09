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
package org.hippoecm.repository;

import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.spi.Name;

public class FacetedNavigationEngineWrapperImpl<Q extends FacetedNavigationEngine.Query, C extends FacetedNavigationEngine.Context>
        implements FacetedNavigationEngine<Q, C> {

    private FacetedNavigationEngine<Q, C> upstream;

    public FacetedNavigationEngineWrapperImpl(FacetedNavigationEngine<Q, C> upstream) {
        this.upstream = upstream;
    }

    public C prepare(String principal, Subject subject, List<Q> initialQueries, Session session)
            throws RepositoryException {
        Context context = upstream.prepare(principal, subject, initialQueries, session);
        return (C) context;
    }

    public void unprepare(C authorization) {
        upstream.unprepare(authorization);
    }

    public void reload(Map<Name, String[]> facetValues) {
        upstream.reload(facetValues);
    }

    public boolean requiresReload() {
        boolean rtvalue = upstream.requiresReload();
        return rtvalue;
    }

    public boolean requiresNotify() {
        boolean rtvalue = upstream.requiresNotify();
        return rtvalue;
    }

    public void notify(String docId, Map<Name, String[]> oldFacets, Map<Name, String[]> newFacets) {
        upstream.notify(docId, oldFacets, newFacets);
    }

    public void purge() {
        upstream.purge();
    }

    public Result view(String queryName, Q initialQuery, C authorization, List<KeyValue<String, String>> facetsQuery,
            List<FacetRange> rangeQuery, Q openQuery, Map<String, Map<String, Count>> resultset,
            Map<String, String> inheritedFilter, HitsRequested hitsRequested) throws UnsupportedOperationException {
        Result result;
        result = upstream.view(queryName, initialQuery, authorization, facetsQuery, rangeQuery, openQuery, resultset,
                inheritedFilter, hitsRequested);
        return result;
    }

    public Result view(String queryName, Q initialQuery, C authorization, List<KeyValue<String, String>> facetsQuery,
            Q openQuery, Map<String, Map<String, Count>> resultset, Map<String, String> inheritedFilter,
            HitsRequested hitsRequested) throws UnsupportedOperationException {
        Result result;
        result = upstream.view(queryName, initialQuery, authorization, facetsQuery, openQuery, resultset,
                inheritedFilter, hitsRequested);
        return result;
    }

    public Result view(String queryName, Q initialQuery, C authorization, List<KeyValue<String, String>> facetsQuery,
            Q openQuery, Map<String, String> inheritedFilter, HitsRequested hitsRequested) {
        Result result;
        result = upstream.view(queryName, initialQuery, authorization, facetsQuery, openQuery, inheritedFilter,
                hitsRequested);
        return result;
    }

    public Q parse(String query) {
        return upstream.parse(query);
    }

    public Result query(String statement, C context) throws InvalidQueryException, RepositoryException {
        return upstream.query(statement, context);
    }

}
