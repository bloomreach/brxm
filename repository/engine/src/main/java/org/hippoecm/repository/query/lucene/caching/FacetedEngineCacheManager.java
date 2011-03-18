/*
 *  Copyright 2011 Hippo.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.index.IndexReader;

public class FacetedEngineCacheManager {
    
    private final Map<IndexReader, FacetedEngineCache> feCaches = new ConcurrentHashMap<IndexReader, FacetedEngineCache>();
     
    public synchronized FacetedEngineCache getCache(IndexReader currentReader, int bitSetCacheSize, int facetValueCountMapCacheSize) {
        FacetedEngineCache cache = feCaches.get(currentReader);
        if(cache == null) {
            // mark all old instances from the map as disposable. We'll try to close the FacetedEngineCaches here. If the refCount is not 0, it will
            // be closed later on
            for(FacetedEngineCache staleCache : feCaches.values()) {
                synchronized (staleCache) {
                    staleCache.markStale();
                    if(staleCache.getRefCount() <= 0 ) {
                        staleCache.dispose();
                    }
                }
            }
            // remove all stale caches from the map.
            feCaches.clear();
            cache = new FacetedEngineCache(currentReader, bitSetCacheSize, facetValueCountMapCacheSize);
            feCaches.put(currentReader, cache);
        }
        
        cache.incrementRefCount();
        
        return cache;
    }

    public synchronized void decreaseRefCount(FacetedEngineCache cache) {
        cache.decreaseRefCount();
    }

    
  
}
