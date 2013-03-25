/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.jackrabbit.core.query.lucene.MultiIndexReader;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.OpenBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationFilter extends Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationFilter.class);

    private static class MultiDocIdSet extends DocIdSet {

        private final int[] maxDocs;
        private final DocIdSet[] docIdSets;

        public MultiDocIdSet(final DocIdSet[] docIdSets, final int[] maxDocs) {
            this.docIdSets = docIdSets;
            this.maxDocs = maxDocs;
        }

        @Override
        public DocIdSetIterator iterator() throws IOException {
            return new DocIdSetIterator() {

                int docID = -1;
                int docOffset = 0;
                int docIdSetIndex = 0;
                DocIdSetIterator currentDocIdSetIterator = (docIdSets.length == 0) ? null : docIdSets[0].iterator();

                @Override
                public int docID() {
                    return docID;
                }

                @Override
                public int nextDoc() throws IOException {
                    while (currentDocIdSetIterator != null) {
                        int currentDocIdSetDocId = currentDocIdSetIterator.nextDoc();
                        if (currentDocIdSetDocId != NO_MORE_DOCS) {
                            docID = docOffset + currentDocIdSetDocId;
                            return docID;
                        }
                        pointCurrentToNextIterator();
                    }
                    docID = NO_MORE_DOCS;
                    return docID;
                }

                /**
                 * if there is no next iterator, currentDocIdSetIterator becomes null
                 */
                private void pointCurrentToNextIterator() throws IOException {
                    currentDocIdSetIterator = null;
                    while (currentDocIdSetIterator == null && docIdSetIndex+1 < docIdSets.length) {
                        docOffset += maxDocs[docIdSetIndex];
                        docIdSetIndex++;
                        currentDocIdSetIterator = docIdSets[docIdSetIndex].iterator();
                    }
                }

                @Override
                public int advance(final int target) throws IOException {
                    while (docID < target && nextDoc() != NO_MORE_DOCS) {}
                    return docID;
                }
            };
        }
    }

    private final Map<IndexReader, DocIdSet> cache = Collections.synchronizedMap(
            new WeakHashMap<IndexReader, DocIdSet>());
    private final Query query;


    public AuthorizationFilter(final Query query) {
        this.query = query;
    }

    public Query getQuery() {
        return query;
    }

    @Override
    public DocIdSet getDocIdSet(final IndexReader reader) throws IOException {
        if (reader instanceof MultiIndexReader) {
            MultiIndexReader multiIndexReader = (MultiIndexReader) reader;

            IndexReader[] indexReaders = multiIndexReader.getIndexReaders();
            DocIdSet[] docIdSets = new DocIdSet[indexReaders.length];
            int[] maxDocs = new int[indexReaders.length];
            for (int i = 0; i < indexReaders.length; i++) {
                IndexReader subReader = indexReaders[i];
                docIdSets[i] = getIndexReaderDocIdSet(subReader);
                maxDocs[i] = subReader.maxDoc();
            }

            return new MultiDocIdSet(docIdSets, maxDocs);
        }
        return getIndexReaderDocIdSet(reader);
    }

    private DocIdSet getIndexReaderDocIdSet(final IndexReader reader) throws IOException {
        DocIdSet docIdSet = cache.get(reader);
        if (docIdSet == null) {
            docIdSet = createAndCacheDocIdSet(reader);
        }
        return docIdSet;
    }

    private synchronized DocIdSet createAndCacheDocIdSet(IndexReader reader) throws IOException {
        DocIdSet docIdSet;
        docIdSet = cache.get(reader);
        if (docIdSet != null) {
           return docIdSet;
        }
        docIdSet = createDocIdSet(reader);
        cache.put(reader, docIdSet);
        return docIdSet;
    }

    private DocIdSet createDocIdSet(IndexReader reader) throws IOException {
        final OpenBitSet bits = new OpenBitSet(reader.maxDoc());

        long start = System.currentTimeMillis();

        new IndexSearcher(reader).search(query, new AbstractHitCollector() {

            @Override
            public final void collect(int doc, float score) {
                bits.set(doc);  // set bit for hit
            }
        });

        long docIdSetCreationTime = System.currentTimeMillis() - start;
        log.info("Creating authorization doc id set took {} ms.", String.valueOf(docIdSetCreationTime));

        return bits;
    }

}
