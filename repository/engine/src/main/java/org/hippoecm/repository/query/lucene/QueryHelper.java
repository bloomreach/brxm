/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;

public class QueryHelper {

    private final static Query noHitsQuery = createNoHitsQuery();
    private final static Query matchAllDocsQuery = new MatchAllDocsQuery();

    private QueryHelper() {}

    public static boolean isNoHitsQuery(final Query query) {
        return noHitsQuery.equals(query);
    }

    public static boolean isMatchAllDocsQuery(final Query query) {
        return matchAllDocsQuery.equals(query);
    }

    public static Query createNoHitsQuery() {
        BooleanQuery noHitsQuery = new BooleanQuery(true);
        noHitsQuery.add(new MatchAllDocsQuery(), Occur.MUST_NOT);
        return noHitsQuery;
    }

    public static Query negateQuery(Query q) {
        BooleanQuery negatedQuery = new BooleanQuery(true);
        negatedQuery.add(new MatchAllDocsQuery(), Occur.MUST);
        negatedQuery.add(q, Occur.MUST_NOT);
        return negatedQuery;
    }
}
