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
import org.apache.lucene.search.HitCollector;
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
        this.scorer = query.weight(searcher).scorer(reader);
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode nextScoreNode() throws IOException {
        /**
         * find next match, code verbally copied from
            {@link IndexSearcher#search(Query, Filter, HitCollector)}
         */
        boolean more = filter.next() && scorer.skipTo(filter.doc());
        while (more) {
            int filterDocId = filter.doc();
            if (filterDocId > scorer.doc() && !scorer.skipTo(filterDocId)) {
                more = false;
            } else {
                int scorerDocId = scorer.doc();
                if (scorerDocId == filterDocId) { // permitted by filter
                    return getScoreNode(scorerDocId);
                } else {
                    more = filter.skipTo(scorerDocId);
                }
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
        // make sure scorer frees resources
        scorer.skipTo(Integer.MAX_VALUE);
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