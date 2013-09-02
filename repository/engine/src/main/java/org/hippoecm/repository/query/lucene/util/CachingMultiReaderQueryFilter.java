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
package org.hippoecm.repository.query.lucene.util;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jackrabbit.core.query.lucene.MultiIndexReader;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.OpenBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachingMultiReaderQueryFilter extends Filter {

    private static final Logger log = LoggerFactory.getLogger(CachingMultiReaderQueryFilter.class);

    private final Map<IndexReader, OpenBitSet> cache = new ConcurrentHashMap<IndexReader, OpenBitSet>(
            new WeakHashMap<IndexReader, OpenBitSet>());

    private final Query query;

    // for some queries, typically the jackrabbit parentAxis / childAxis queries do not support
    // multi index dissection as they require to be able to jump through multiple indexes for a query.
    // these queries need to be done with dissectMultiIndex = false.
    private boolean dissectMultiIndex = true;

    public CachingMultiReaderQueryFilter(final Query query) {
        this.query = query;
    }


    public CachingMultiReaderQueryFilter(final Query query, boolean dissectMultiIndex) {
        this.query = query;
        this.dissectMultiIndex = dissectMultiIndex;
    }

    public Query getQuery() {
        return query;
    }

    @Override
    public DocIdSet getDocIdSet(final IndexReader reader) throws IOException {
        if (dissectMultiIndex && reader instanceof MultiIndexReader) {
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
        OpenBitSet docIdSet = cache.get(reader);
        if (docIdSet != null) {
            return docIdSet;
        }
        // no synchronization needed: worst case scenario two concurrent thread do it both
        docIdSet = createDocIdSet(reader);
        cache.put(reader, docIdSet);
        return docIdSet;
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

        long docIdSetCreationTime = System.currentTimeMillis() - start;
        log.info("Creating CachingMultiReaderQueryFilter doc id set took {} ms.", String.valueOf(docIdSetCreationTime));

        return bits;
    }

}
