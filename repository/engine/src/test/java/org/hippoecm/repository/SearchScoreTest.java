/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;

import org.hippoecm.repository.util.RowIterable;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertTrue;

public class SearchScoreTest extends RepositoryTestCase {

    /**
     * Below two documents are defined: The first document contains 'The quick brown fox jumps over the lazy dog'
     * The second document contains 'The quick brown fox jumps over the lazy dog' but contains a second field as well
     * We thus expect that due to TF & IDF (see Lucene) when searching for 'quick' should result in a higher
     * score for Document1. This should be the case both for plain text searches as well as for wildcard searches
     *
     * NOTE that because in o.h.r.query.lucene.ServicingNodeIndexer#createFulltextField(String, boolean, boolean, boolean)
     * we use Field.Index.ANALYZED instead of Field.Index.ANALYZED_NO_NORMS that it actually *works* for non-wildcard
     * queries: This is because when using NO_NORMS, no field length normalization is used during indexing!
     */
    private String[] defaultContent = new String[] {
            "/test", "nt:unstructured",
               "/test/Document1", "hippo:handle",
                  "jcr:mixinTypes", "mix:referenceable",
                  "/test/Document1/Document1", "hippo:testsearchdocument",
                     "jcr:mixinTypes", "mix:referenceable",
                     "title", "The quick brown fox jumps over the lazy dog",
                     "summary", "The earliest known appearance of the phrase is from The Boston Journal. " +
                                "In an article titled Current Notes in the February 10, 1885 morning edition, " +
                                "the phrase is mentioned as a good practice sentence for writing students: ",
               "/test/Document2", "hippo:handle",
                  "/test/Document2/Document2", "hippo:testsearchdocument",
                     "jcr:mixinTypes", "mix:referenceable",
                     "title", "The quick brown fox jumps over the lazy dog"
    };



    private void createContent(String[]... contents) throws Exception {
        if (contents == null) {
            throw new IllegalArgumentException("no bootstrap content");
        }

        for (String[] content : contents) {
            build(content, session);
        }
        session.save();
    }

    @Test
    public void test_scoring_descending_no_wildcard_involved() throws Exception {
        assert_scoring("quick");
    }

    @Test
    public void test_scoring_ascending_no_wildcard_involved() throws Exception {
        assert_scoring("quick");
    }

    @Test
    public void test_scoring_descending_with_wildcard_involved() throws Exception {
        assert_scoring("qui*");
    }

    @Test
    public void test_scoring_ascending_with_wildcard_involved() throws Exception {
        assert_scoring("qui*");
    }

    private void assert_scoring(final String contains) throws Exception {

        createContent(defaultContent);
        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+contains+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();

        Map<String, Double> scores = new HashMap<>();
        for (Row row : new RowIterable(queryResult.getRows())) {
            scores.put(row.getNode().getName(), row.getScore());
        }

        assertTrue(scores.get("Document1") < scores.get("Document2") );
    }
}
