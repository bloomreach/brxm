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
package org.hippoecm.repository.query.lucene;

import java.io.IOException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.FieldSelectors;
import org.apache.jackrabbit.core.query.lucene.QueryHits;
import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;

/**
 * Wraps a lucene query result and adds a close method that allows to release resources after a query has been executed
 * and the results have been read completely.
 */
public class HippoLuceneQueryHits implements QueryHits {

    /**
     * The IndexReader in use by the lucene hits.
     */
    private final IndexReader reader;

    /**
     * The scorer for the query.
     */
    private final Scorer scorer;

    /**
     * The filter for the query.
     */
    private final DocIdSetIterator filter;

    public HippoLuceneQueryHits(IndexReader reader, Filter filter, IndexSearcher searcher, Query query) throws IOException {
        this.reader = reader;
        this.filter = filter.getDocIdSet(reader).iterator();
        // We rely on Scorer#nextDoc() and Scorer#advance(int) so enable
        // scoreDocsInOrder
        this.scorer = query.weight(searcher).scorer(reader, true, false);
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode nextScoreNode() throws IOException {
        if (scorer == null) {
            return null;
        }
        if (filter == null) {
            return null;
        }

        int filterDocId = filter.nextDoc();
        int scorerDocId = scorer.advance(filterDocId);

        while (true) {
            if (filterDocId == scorerDocId) {
                if (scorerDocId == DocIdSetIterator.NO_MORE_DOCS) {
                    break;
                }
                return getScoreNode(scorerDocId);
            } else if (scorerDocId > filterDocId) {
                filterDocId = filter.advance(scorerDocId);
            } else {
                scorerDocId = scorer.advance(filterDocId);
            }
        }

        return null;
    }

    private ScoreNode getScoreNode(int doc) throws IOException {
        NodeId id = new NodeId(reader.document(doc, FieldSelectors.UUID).get(FieldNames.UUID));
        return new ScoreNode(id, scorer.score(), doc);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        // make sure scorer and filter free resources
        if (scorer != null) {
            scorer.advance(Integer.MAX_VALUE);
        }
        if (filter != null) {
            filter.advance(Integer.MAX_VALUE);
        }
    }

    /**
     * @return always -1.
     */
    public int getSize() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public void skip(int n) throws IOException {
        while (n-- > 0) {
            if (nextScoreNode() == null) {
                return;
            }
        }
    }
}