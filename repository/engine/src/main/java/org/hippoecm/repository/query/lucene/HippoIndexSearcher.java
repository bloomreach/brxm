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
package org.hippoecm.repository.query.lucene;

import java.io.IOException;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.JackrabbitIndexSearcher;
import org.apache.jackrabbit.core.query.lucene.JackrabbitQuery;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryHits;
import org.apache.jackrabbit.core.query.lucene.QueryHits;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

public class HippoIndexSearcher extends JackrabbitIndexSearcher {

    private final IndexReader reader;
    private final AuthorizationFilter authorizationFilter;

    public HippoIndexSearcher(SessionImpl s, IndexReader r, ItemStateManager ism, final AuthorizationFilter abs) {
        super(s, r, ism);
        reader = r;
        authorizationFilter = abs;
    }

    @Override
    public QueryHits evaluate(Query query, final Sort sort, final long resultFetchHint) throws IOException {
        query = query.rewrite(reader);
        QueryHits hits = null;
        if (query instanceof JackrabbitQuery) {
            hits = ((JackrabbitQuery) query).execute(this, getSession(), sort);
        }
        if (hits == null) {
            boolean sortedSearch = sort.getSort().length == 0;
            if (sortedSearch && authorizationFilter == null) {
                hits = new LuceneQueryHits(reader, this, query);
            } else if (sortedSearch) {
                hits = new HippoLuceneQueryHits(reader, authorizationFilter, this, query);
            } else {
                hits = new HippoSortedLuceneQueryHits(reader, authorizationFilter, this, query, sort, resultFetchHint);
            }
        }
        return hits;
    }
}
