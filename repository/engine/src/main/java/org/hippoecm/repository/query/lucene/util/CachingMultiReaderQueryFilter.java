/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;

import org.apache.jackrabbit.core.query.lucene.MultiIndexReader;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.WeakIdentityMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachingMultiReaderQueryFilter extends Filter {

    private static final Logger log = LoggerFactory.getLogger(CachingMultiReaderQueryFilter.class);

    private final WeakIdentityMap<IndexReader, OpenBitSet> cache = WeakIdentityMap.newConcurrentHashMap();

    private final Query query;
    // userId of the jcr session triggering this CachingMultiReaderQueryFilter : Required only for logging purposes
    private final String userId;

    /**
     * @param query only plain Lucene queries are allowed here, as Jackrabbit Query implementations are very specific,
     *              keep references to index readers, need multi index readers, etc etc
     */
    public CachingMultiReaderQueryFilter(final Query query, final String userId) {
        this.query = query;
        this.userId = userId;
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
        log.warn("IndexReader of type MultiIndexReader expected for userId '{}' but reader of type '{}' was found. " +
                "Do not dissect the reader but use it as is.", userId, reader.getClass().getName());

        return getIndexReaderDocIdSet(reader);
    }

    private DocIdSet getIndexReaderDocIdSet(final IndexReader reader) throws IOException {

        OpenBitSet docIdSet = cache.get(reader);
        if (docIdSet != null) {
            log.debug("For userId '{}' return cached bitSet for reader with max doc '{}' and num docs '{}'",
                    userId, reader.maxDoc(), reader.numDocs());
            return docIdSet;
        }
        synchronized (this) {
            // try again after obtaining the lock
            docIdSet = cache.get(reader);
            if (docIdSet != null) {
                log.debug("For userId '{}' return cached bitSet for reader with max doc '{}' and num docs '{}'",
                        userId, reader.maxDoc(), reader.numDocs());
                return docIdSet;
            }
            log.debug("For userId '{}' could not find a cached bitSet for reader  with max doc '{}' and num docs '{}'",
                    userId, reader.maxDoc(), reader.numDocs());
            docIdSet = createDocIdSet(reader);
            cache.put(reader, docIdSet);
            return docIdSet;
        }
    }

    private OpenBitSet createDocIdSet(IndexReader reader) throws IOException {
        final OpenBitSet bits = new OpenBitSet(reader.maxDoc());

        long start = System.currentTimeMillis();
        new IndexSearcher(reader).search(query, new AbstractHitCollector() {
            @Override
            public final void collect(int doc, float score) {
                bits.set(doc);  // set bit for hit
            }
        });
        log.info("For userId '{}', creating CachingMultiReaderQueryFilter doc id set took {} ms.", userId,
                String.valueOf(System.currentTimeMillis() - start));
        return bits;
    }

}
