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

import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hippoecm.repository.FacetedNavigationEngine.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetedEngineCache {
    
    private static final Logger log = LoggerFactory.getLogger(FacetedEngineCache.class);
   
    Map<String, BitSet> bitSetCache;
    Map<FECacheKey, Map<String, Count>> facetValueCountMapCache;
    
    long creationTime;
    volatile long fvcMisses = 0L;
    
    volatile long fvcHits = 0L;
    volatile long bitSetMisses = 0L;
    volatile long bitSetHits = 0L;
    
    public FacetedEngineCache(int bitSetCacheSize, int facetValueCountMapCacheSize) {
        creationTime = System.currentTimeMillis();
        if (bitSetCacheSize < 100) {
            log.warn("Minimum bitSetCache size is 100. Change size to 100");
            bitSetCacheSize = 100;
        }
        if (facetValueCountMapCacheSize < 100) {
            log.warn("Minimum facetValueCountMapCache size is 100. Change size to 100");
            facetValueCountMapCacheSize = 100;
        }
       
        bitSetCache =  Collections.synchronizedMap(new LRUMap<String, BitSet>(100, bitSetCacheSize));
        facetValueCountMapCache = Collections.synchronizedMap(new LRUMap<FECacheKey, Map<String,Count>>(100, facetValueCountMapCacheSize));
    }
    
    public Map<String, Count> getFacetValueCountMap(FECacheKey key) {
        Map<String, Count> val = facetValueCountMapCache.get(key);
        if (val == null) {
            fvcMisses++;
        } else {
            fvcHits++;
        }
        return val;
    }
    
    public void putFacetValueCountMap(FECacheKey key, Map<String, Count> facetValueCountMap) {
        facetValueCountMapCache.put(key, facetValueCountMap);
    }
    
    
    public BitSet getBitSet(String key){
        BitSet val = bitSetCache.get(key);
        if (val == null) {
            bitSetMisses++;
        } else {
            bitSetHits++;
        }
        return val;
    }
    
    public void putBitSet(String key, BitSet bitSet){
       bitSetCache.put(key, bitSet);
    }
    
    void clear() {
        bitSetCache.clear();
        facetValueCountMapCache.clear();
    }
    
    private long estimateMemoryInBytes(Map<FECacheKey, Map<String, Count>> cache) {
        long start = System.currentTimeMillis();
        long estimatedBytes = 0;
        
        for(Entry<FECacheKey, Map<String, Count>> entry : cache.entrySet()) {
            // we ignore the objects in the FECacheKey objects array, as they are most likely also used for other keys. Only the object
            // reference overhead and the array overheaad (12)
            estimatedBytes += entry.getKey().objects.length * 8 + 12;
            
            for(Entry<String, Count> subentry : entry.getValue().entrySet()) {
                // add the String overhead
               int stringMemLenght = 8 * (int) ((((subentry.getKey().length()) * 2) + 45) / 8);
               estimatedBytes += stringMemLenght;
               // add the Count overhead: Object overhead 8 and int 4 bytes
               estimatedBytes += 8 + 4;
            }
        }
        log.info("Estimating the facetValueCountMap memory size took: " + (System.currentTimeMillis() - start) + " ms.");
        return estimatedBytes;
    }

    /*
     * A very rough estimate of the memory consumed by Map<String, BitSet>. The bitSetLength is the length of all the BitSet as they
     * are all of equal length.
     * 
     * Note that most of object overhead is ignored. We just calculate a rough order of magnitude about size
     */
    private long estimateMemoryInBytes(Map<String, BitSet> cache, int bitSetLength) {
        long start = System.currentTimeMillis();
        // we estimate the BitSet size just equal to the lenght for the bitset: this might be bigger than bitSetLength because new BitSet(bitSetLength) 
        // can create a bitset with more capacity. Therefore, we create a new BitSet(bitSetLength)
        long estimatedBytes = (cache.size() * new BitSet(bitSetLength).size() / 8) ;
        // add the BitSet object overhead
        estimatedBytes +=   8 * cache.size();
        // add the Entry overhead:
        estimatedBytes +=   8 * cache.size();
        
        for(Entry<String, BitSet> entry : cache.entrySet()) {
            // add the String overhead
           int stringMemLenght = 8 * (int) ((((entry.getKey().length()) * 2) + 45) / 8);
           estimatedBytes += stringMemLenght;
        }
        log.info("Estimating the BitSet memory size took: " + (System.currentTimeMillis() - start)  + " ms.");
        return estimatedBytes;
    }

    @Override
    protected void finalize() throws Throwable {
        
        if(log.isDebugEnabled()) {
            log.debug("Disposing old FacetedEngineCache");
            if(bitSetCache.size() != 0) {
                int maxDoc = bitSetCache.values().iterator().next().size();
                long bitSetCacheSizeInBytes = estimateMemoryInBytes(bitSetCache, maxDoc); 
                log.debug("Cache lived for {} seconds.", String.valueOf((System.currentTimeMillis() - creationTime)/1000) );
                log.debug("Cache bitSetCache size = '{}'. Each BitSet has length '{}'",bitSetCache.size() ,maxDoc);
                log.debug("Cache bitSetCache estimated memory consumption is '{} Kb', or '{} Mb'", (bitSetCacheSizeInBytes + 512)/1024, (bitSetCacheSizeInBytes + (1024 * 512) ) / (1024*1024));
                log.debug("Cache bitSet stats: hits = '{}' , misses = '{}'. Hit percentage = "+ (bitSetHits * 100) / (bitSetHits + bitSetMisses) +"%", bitSetHits, bitSetMisses);
            }
            if(facetValueCountMapCache.size() != 0) {
                long fvcSizeInBytes = estimateMemoryInBytes(facetValueCountMapCache); 
                log.debug("Cache facetValueCountMapCache size = '{}'", facetValueCountMapCache.size());
                log.debug("Cache facetValueCountMapCache estimated memory consumption is '{} Kb', or '{} Mb'", (fvcSizeInBytes + 512)/1024, (fvcSizeInBytes + (1024 * 512) )/(1024*1024));
                log.debug("Cache facetValueCountMapCache stats: hits = '{}' , misses = '{}'. Hit percentage = "+ ( fvcHits * 100 ) / (fvcHits + fvcMisses) +"%", fvcHits, fvcMisses);
            }
        }    
        
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
