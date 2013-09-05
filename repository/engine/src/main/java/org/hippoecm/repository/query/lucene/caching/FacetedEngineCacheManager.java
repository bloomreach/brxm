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

import org.apache.jackrabbit.core.query.lucene.JackrabbitIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetedEngineCacheManager {

    private static final Logger log = LoggerFactory.getLogger(FacetedEngineCacheManager.class);

    private int facetValueCountMapSize = 250;
    private int docIdSetCacheSize = 250;

    private Object searcherCreatedWithObject;

    private CacheAndSearcher cacheAndSearcher;
    private IndexSearcher searcher;
    private FacetedEngineCache cache;

    public synchronized CacheAndSearcher getCacheAndSearcherInstance(IndexReader currentReader) {
        if (currentReader instanceof JackrabbitIndexReader) {
            // for every search a new JackrabbitIndexReader is created that is a wrapper around potentially
            // the same index reader as for the previous request. Since JackrabbitIndexReader extends FilterIndexReader
            // the getCoreCacheKey returns instance of wrapped index.
            if (searcherCreatedWithObject == currentReader.getCoreCacheKey()) {
                log.debug("Return valid cache and searcher");
                return cacheAndSearcher;
            }
            log.debug("Create new cache and searcher");
            searcherCreatedWithObject = currentReader.getCoreCacheKey();
        } else {
            if (searcherCreatedWithObject == currentReader) {
                log.debug("Return valid cache and searcher");
                return cacheAndSearcher;
            }
            log.debug("Create new cache and searcher");
            searcherCreatedWithObject = currentReader;
        }

        searcher = new IndexSearcher(currentReader);
        cacheAndSearcher = new CacheAndSearcher(new FacetedEngineCache(docIdSetCacheSize, facetValueCountMapSize), searcher);
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
