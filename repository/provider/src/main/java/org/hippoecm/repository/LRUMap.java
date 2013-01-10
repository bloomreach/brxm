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
package org.hippoecm.repository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU Cache for caching parts in the modules that are expensive to create, for example a ParsedFacet
 * 
 */
class LRUMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;
    private int maxCacheSize;
    private static final int DEFAULT_MAX_CAPACITY = 1000;
    private static final int DEfAULT_INITIAL_CAPACITY = 100;
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
