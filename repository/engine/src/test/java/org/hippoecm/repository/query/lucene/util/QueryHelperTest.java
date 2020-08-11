/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.query.lucene.util;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hippoecm.repository.query.lucene.QueryHelper;
import org.junit.Test;

import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;
import static org.hippoecm.repository.query.lucene.QueryHelper.isMatchAllDocsQuery;
import static org.hippoecm.repository.query.lucene.QueryHelper.isNoHitsQuery;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QueryHelperTest {

    @Test
    public void validateMatchAllDocsQuery_returns_true_if_matchAllDocsQuery() {
        Query matchAllDocsQuery = new MatchAllDocsQuery();
        assertTrue(isMatchAllDocsQuery(matchAllDocsQuery));
    }

    @Test
    public void validateNoHitsQueryChecker_returns_true_if_not_nohitsquery() {
        BooleanQuery noHitsQuery = new BooleanQuery(true);
        noHitsQuery.add(new MatchAllDocsQuery(), MUST_NOT);
        assertTrue(isNoHitsQuery(noHitsQuery));
        assertTrue(isNoHitsQuery(QueryHelper.createNoHitsQuery()));
    }

    @Test
    public void validateNoHitsQueryChecker_returns_false_if_not_nohitsquery() {
        BooleanQuery notANoHitsQuery = new BooleanQuery(true);
        notANoHitsQuery.add(new MatchAllDocsQuery(), SHOULD);
        assertFalse(isNoHitsQuery(notANoHitsQuery));
    }

}
