/**
 * Copyright 2016-2023 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.jmx;

public interface CacheStatsMXBean {

    /**
     * Methods below return stats on combined first level, second level and stale caches.
     * It cannot be calculated from the separate stats: The stats below are very straightforward: A
     * hit is counted when a cached response is returned, regardless from which cache and whether
     * the hit is stale or not
     */
    long getCacheHits();
    long getCacheMisses();
    double getCacheHitRate();
    double getCacheMissRate();
    void reset();

    /**
     * end methods for first, second and stale combined
     */
    long getFirstLevelCachePuts();
    long getFirstLevelCacheHits();
    long getFirstLevelCacheMisses();
    double getFirstLevelCacheHitRate();
    double getFirstLevelCacheMissRate();
    // we can only know the cache size and max size of the first level cache and not of the second/stale
    int getFirstLevelCacheSize();
    int getFirstLevelCacheMaxSize();
    long getFirstLevelCacheTimeToLiveSeconds();
    long getFirstLevelCacheTimeToIdleSeconds();
    void resetFirstLevelCache();

    long getSecondLevelCachePuts();
    long getSecondLevelCacheHits();
    long getSecondLevelCacheMisses();
    double getSecondLevelCacheHitRate();
    double getSecondLevelCacheMissRate();
    void resetSecondLevelCache();

    long getStaleCachePuts();
    long getStaleCacheHits();
    long getStaleCacheMisses();
    double getStaleCacheHitRate();
    double getStaleCacheMissRate();
    void resetStaleCache();

    void resetAll();
}
