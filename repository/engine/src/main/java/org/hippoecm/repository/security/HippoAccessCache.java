/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.jackrabbit.core.id.ItemId;

/**
 * Cache access permissions for the HippoAccessManager
 * User <code>HippoAccessCache.getInstance(userId)<code> to
 * get a cache for the given userId. The cache is thread safe.
 */
public class HippoAccessCache {

    /**
     * The read access cache map
     */
    private LRUCache<ItemId, Boolean> readAccessCache = null;

    /**
     * The max size of the current cache
     */
    private volatile int currentMaxSize;

    /**
     * Counters
     */
    private long accesses = 0L;
    private long hits = 0L;
    private long misses = 0L;


    /**
     * The caches per repository service instance
     */
    private static final Map<String, HippoAccessCache> caches = new WeakHashMap<String, HippoAccessCache>();


    /**
     *
     * @param userId  the userId. If <code>null</code> this method will return a
     *                new cache instance for each such call.
     * @param cacheSize
     * @return the <code>HippoAccessCache</code> instance for the given
     *                <code>userId</code>.
     */
    public static HippoAccessCache getInstance(String userId, final int cacheSize) {
        // if no userId is provided return a new volatile cache
        if (userId == null) {
            return new HippoAccessCache(cacheSize);
        }
        synchronized (caches) {
            HippoAccessCache cache = caches.get(userId);
            if (cache == null) {
                cache = new HippoAccessCache(cacheSize);
                caches.put(userId, cache);
            }
            return cache;
        }
    }

    /**
     * Clear all caches known to the HippoAccessCache
     */
    public static void clearAll() {
        synchronized (caches) {
            for (HippoAccessCache cache : caches.values()) {
                cache.clear();
            }
        }
    }

    /**
     * private constructor
     * @param cacheSize
     */
    HippoAccessCache(final int cacheSize) {
        // set the current size;
        currentMaxSize = cacheSize;
        if (currentMaxSize < 1) {
            return;
        }
        // set initial cache size
        int init = currentMaxSize/20;
        readAccessCache = new LRUCache<ItemId, Boolean>(init, currentMaxSize);
    }

    /**
     * Fetch cache value
     * @param id ItemId
     * @return cached value or null when not in cache
     */
    synchronized public Boolean get(ItemId id) {
        if (currentMaxSize < 1) {
            return null;
        }
        synchronized (readAccessCache) {
            accesses++;
            Boolean obj = (Boolean) readAccessCache.get(id);
            if (obj == null) {
                misses++;
            } else {
                hits++;
            }
            return obj;
        }
    }

    /**
     * Store key-value in cache
     * @param id ItemId the key
     * @param isGranted the value
     */
    synchronized public void put(ItemId id, boolean isGranted) {
        if (currentMaxSize < 1) {
            return;
        }
        synchronized (readAccessCache) {
            readAccessCache.put(id, Boolean.valueOf(isGranted));
        }
    }

    /**
     * Remove key-value from cache
     * @param id ItemId the key
     */
    synchronized public void remove(ItemId id) {
        if (currentMaxSize < 1) {
            return;
        }
        synchronized (readAccessCache) {
            readAccessCache.remove(id);
        }
    }

    /**
     * Clear the cache
     */
    synchronized public void clear() {
        if (currentMaxSize < 1) {
            return;
        }
        synchronized (readAccessCache) {
            readAccessCache.clear();
        }
    }

    /**
     * The current number of items in the cache
     * @return int
     */
    public int getSize() {
        int size;
        synchronized (readAccessCache) {
            size = readAccessCache.size();
        }
        return size;
    }

    /**
     * Total number of times this cache is accessed
     * @return long
     */
    public long getAccesses() {
        return accesses;
    }

    /**
     * Total number of cache hits
     * @return long
     */
    public long getHits() {
        return hits;
    }

    /**
     * Total number of cache misses
     * @return long
     */
    public long getMisses() {
        return misses;
    }

    /**
     * The max size of the cache
     * @return int
     */
    public int getMaxSize() {
        return currentMaxSize;
    }

    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private int maxCacheSize;
        private static final int DEFAULT_MAX_CAPACITY = 10000;
        private static final int DEfAULT_INITIAL_CAPACITY = 500;
        private static final float LOAD_FACTOR = 0.75f;

        /**
         * Default constructor for an LRU Cache The default capacity is 10000
         */
        public LRUCache() {
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
        public LRUCache(int initialCapacity, int maximumCacheSize) {
            super(initialCapacity, LOAD_FACTOR, true);
            this.maxCacheSize = maximumCacheSize;
        }

        /**
         * @return Returns the maxCacheSize.
         */
        public int getMaxCacheSize() {
            return maxCacheSize;
        }

        /**
         * @param maxCacheSize The maxCacheSize to set.
         */
        public void setMaxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > maxCacheSize;
        }
    }

}
