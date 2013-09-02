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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.search.Filter;
import org.hippoecm.repository.FacetedNavigationEngine.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetedEngineCache {
    
    private static final Logger log = LoggerFactory.getLogger(FacetedEngineCache.class);

    private final Map<String, Filter> filterCache;
    private final Map<FECacheKey, Map<String, Count>> facetValueCountMapCache;
    
    public FacetedEngineCache(int filterCacheSize, int facetValueCountMapCacheSize) {
        if (filterCacheSize < 100) {
            log.warn("Minimum filterCache size is 100. Change size to 100");
            filterCacheSize = 100;
        }
        if (facetValueCountMapCacheSize < 100) {
            log.warn("Minimum facetValueCountMapCache size is 100. Change size to 100");
            facetValueCountMapCacheSize = 100;
        }

        // do not make below ConcurrentHashMap as that will not be a LRUMap any more
        filterCache =  Collections.synchronizedMap(new LRUMap<String, Filter>(100, filterCacheSize));
        facetValueCountMapCache = Collections.synchronizedMap(new LRUMap<FECacheKey, Map<String,Count>>(100, facetValueCountMapCacheSize));
    }
    
    public Map<String, Count> getFacetValueCountMap(FECacheKey key) {
        return facetValueCountMapCache.get(key);
    }
    
    public void putFacetValueCountMap(FECacheKey key, Map<String, Count> facetValueCountMap) {
        facetValueCountMapCache.put(key, facetValueCountMap);
    }

    public Filter getFilter(String key) {
        return filterCache.get(key);
    }

    public void putFilter(String key, Filter docIdSet) {
        filterCache.put(key, docIdSet);
    }

    int getDocIdSetCacheSize() {
        return filterCache.size();
    }

    int getFacetValueCountMapCacheSize() {
        return facetValueCountMapCache.size();
    }

    void clear() {
        filterCache.clear();
        facetValueCountMapCache.clear();
    }
    
    /**
     * An FECacheKey is a key that is constructed by any Object array. Two {@link FECacheKey} are equal if all the 
     * Object's in the array are equal. Make sure when using this FECacheKey that all objects have a proper equals and hashCode implementation
     */
    public static class FECacheKey {
        Object[] objects;
       
        public FECacheKey(Object[] objects) {
           if(objects == null || objects.length == 0) {
               throw new IllegalArgumentException("Array is not allowed to be null or length 0");
           }
           this.objects = objects; 
        }

       @Override
       public boolean equals(Object obj) {
           // if all objects are equal, the FECacheKey are considered equal
           if (obj == this) {
               return true;
           }
           if (!(obj instanceof FECacheKey)) {
               return false;
           }
           FECacheKey o = (FECacheKey)obj;
           if(o.objects.length != objects.length) {
               return false;
           }
           for(int i = 0; i < objects.length; i++) {
               if(!objects[i].equals(o.objects[i])) {
                   return false;
               }
           }
           return true;
       }

       @Override
       public int hashCode() {
           int hashCode = 1;
           for(Object obj: objects) {
               hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
           }
           return hashCode;
       }
        
        
    }
   
    class LRUMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private int maxCacheSize;
        private static final int DEFAULT_MAX_CAPACITY = 10000;
        private static final int DEfAULT_INITIAL_CAPACITY = 500;
        private static final float LOAD_FACTOR = 0.75f;
 
        /**
         * Default constructor for an LRU Cache The default capacity is 10000
         */
        LRUMap() {
            this(DEfAULT_INITIAL_CAPACITY, DEFAULT_MAX_CAPACITY);
        }

        /**
         * Constructs an empty <tt>LRUCache</tt> instance with the specified
         * initial capacity, maximumCacheSize,load factor and ordering mode.
         *
         * @param initialCapacity the initial capacity.
         * @param maximumCacheSize
         * @throws IllegalArgumentException if the initial capacity is negative or
         *                 the load factor is non-positive.
         */
        LRUMap(int initialCapacity, int maximumCacheSize) {
            super(initialCapacity, LOAD_FACTOR, true);
            this.maxCacheSize = maximumCacheSize;
        }

        /**
         * @return Returns the maxCacheSize.
         */
        int getMaxCacheSize() {
            return maxCacheSize;
        }

        /**
         * @param maxCacheSize The maxCacheSize to set.
         */
        void setMaxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > maxCacheSize;
        }
    }
}
