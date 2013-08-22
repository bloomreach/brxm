/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.repository.query.lucene.caching;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

public class FacetedEngineCacheManager {

    private int facetValueCountMapSize = 250;
    private int docIdSetCacheSize = 250;

    private IndexReader searcherCreatedWithIndexReader;

    private CacheAndSearcher cacheAndSearcher;
    private IndexSearcher searcher;
    private FacetedEngineCache cache;

    public synchronized CacheAndSearcher getCacheAndSearcherInstance(IndexReader currentReader) {
        if (searcherCreatedWithIndexReader == currentReader) {
            return cacheAndSearcher;
        }

        // the currentReader is not the same as the reader with which the cacheAndSearcher was created. Recreate it now 
        searcherCreatedWithIndexReader = currentReader;
        searcher = new IndexSearcher(currentReader);
        cacheAndSearcher = new CacheAndSearcher(cache, searcher);
        return cacheAndSearcher;
    }

    public void setDocIdSetCacheSize(final int docIdSetCacheSize) {
        this.docIdSetCacheSize = docIdSetCacheSize;
    }

    public int getDocIdSetCacheSize() {
        return docIdSetCacheSize;
    }

    public void setFacetValueCountMapSize(final int facetValueCountMapSize) {
        this.facetValueCountMapSize = facetValueCountMapSize;
    }

    public int getFacetValueCountMapSize() {
        return facetValueCountMapSize;
    }

    public void init() {
        cache = new FacetedEngineCache(docIdSetCacheSize, facetValueCountMapSize);
    }

    public class CacheAndSearcher {
        
        private FacetedEngineCache cache; 
        private  IndexSearcher searcher;
        
        public CacheAndSearcher(FacetedEngineCache cache, IndexSearcher searcher) {
            this.cache = cache;
            this.searcher = searcher;
        }
        public FacetedEngineCache getCache() {
            return cache;
        }
        public IndexSearcher getSearcher() {
            return searcher;
        }
        
    }
  
}
