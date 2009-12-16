/*
 *  Copyright 2008 Hippo.
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
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ComplexExplanation;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.ToStringUtils;

/** A Query that matches documents containing a term.
This may be combined with other terms with a {@link BooleanQuery}.
 */
public class FixedScoreTermQuery extends Query {
    
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private Term term;

    private class FixedScoreTermWeight implements Weight {
       
        private static final long serialVersionUID = 1L;
        private Similarity similarity;
        private float value;

        public FixedScoreTermWeight(Searcher searcher) throws IOException {
            this.similarity = searcher.getSimilarity();
        }

        public String toString() {
            return "weight(" + FixedScoreTermQuery.this + ")";
        }

        public Query getQuery() {
            return FixedScoreTermQuery.this;
        }

        public float getValue() {
            return value;
        }

        public float sumOfSquaredWeights() {
            return 1.0f; // square it
        }

        public void normalize(float queryNorm) {
            value = 1.0f;
        }

        public Scorer scorer(IndexReader reader) throws IOException {
            TermDocs termDocs = reader.termDocs(term);

            if (termDocs == null) {
                return null;
            }
            return new FixedScoreTermScorer(this, termDocs, similarity);
        }

        public Explanation explain(IndexReader reader, int doc) throws IOException {

            ComplexExplanation result = new ComplexExplanation();

            return result;
        }
    }

    protected class FixedScoreTermScorer extends Scorer {
        private int pointer;
        private int doc;
        private int pointerMax;
        private TermDocs termDocs;
        private final int[] docs = new int[32]; // buffered doc numbers (do not set higher)
        private final int[] freqs = new int[32]; // buffered term freqs

        protected FixedScoreTermScorer(Similarity similarity) {
            super(similarity);
        }

        protected FixedScoreTermScorer(Weight weight, TermDocs td, Similarity similarity) {
            super(similarity);
            this.termDocs = td;
        }

        public void score(HitCollector hc) throws IOException {
            next();
            score(hc, Integer.MAX_VALUE);
        }

        @Override
        public int doc() {
            return doc;
        }

        @Override
        public Explanation explain(int value) throws IOException {
            return new Explanation(1.0f, "No Score Term Query has constant score of 1.0");
        }

        @Override
        public boolean next() throws IOException {
            pointer++;
            if (pointer >= pointerMax) {
                pointerMax = termDocs.read(docs, freqs); // refill buffer
                if (pointerMax != 0) {
                    pointer = 0;
                } else {
                    termDocs.close(); // close stream
                    doc = Integer.MAX_VALUE; // set to sentinel value
                    return false;
                }
            }
            doc = docs[pointer];
            return true;
        }

        @Override
        public float score() throws IOException {
            return 1.0f;
        }

        @Override
        public boolean skipTo(int target) throws IOException {
            // first scan in cache
            for (pointer++; pointer < pointerMax; pointer++) {
                if (docs[pointer] >= target) {
                    doc = docs[pointer];
                    return true;
                }
            }

            // not found in cache, seek underlying stream
            boolean result = termDocs.skipTo(target);
            if (result) {
                pointerMax = 1;
                pointer = 0;
                docs[pointer] = doc = termDocs.doc();
                freqs[pointer] = termDocs.freq();
            } else {
                doc = Integer.MAX_VALUE;
            }
            return result;
        }
    }

    /** Constructs a query for the term <code>t</code>. */
    public FixedScoreTermQuery(Term t) {
        term = t;
    }

    /** Returns the term of this query. */
    public Term getTerm() {
        return term;
    }

    protected Weight createWeight(Searcher searcher) throws IOException {
        return new FixedScoreTermWeight(searcher);
    }

    public void extractTerms(Set terms) {
        terms.add(getTerm());
    }

    /** Prints a user-readable version of this query. */
    public String toString(String field) {
        StringBuffer buffer = new StringBuffer();
        if (!term.field().equals(field)) {
            buffer.append(term.field());
            buffer.append(":");
        }
        buffer.append(term.text());
        buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.toString();
    }

    /** Returns true iff <code>o</code> is equal to this. */
    public boolean equals(Object o) {
        if (!(o instanceof FixedScoreTermQuery)) {
            return false;
        }
        FixedScoreTermQuery other = (FixedScoreTermQuery) o;
        return this.term.equals(other.term);
    }

    /** Returns a hash code value for this object.*/
    public int hashCode() {
        return Float.floatToIntBits(getBoost()) ^ term.hashCode();
    }
}
