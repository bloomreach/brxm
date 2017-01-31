/**
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.concurrent.atomic.AtomicLong;

import net.sf.ehcache.Ehcache;

public class CacheStats implements CacheStatsMXBean {


    private AtomicLong cacheHits = new AtomicLong();
    private AtomicLong cacheMisses = new AtomicLong();

    private AtomicLong firstLevelCachePuts = new AtomicLong();
    private AtomicLong firstLevelCacheHits = new AtomicLong();
    private AtomicLong firstLevelCacheMisses = new AtomicLong();

    private AtomicLong secondLevelCachePuts = new AtomicLong();
    private AtomicLong secondLevelCacheHits = new AtomicLong();
    private AtomicLong secondLevelCacheMisses = new AtomicLong();

    private AtomicLong staleCachePuts = new AtomicLong();
    private AtomicLong staleCacheHits = new AtomicLong();
    private AtomicLong staleCacheMisses = new AtomicLong();

    private Ehcache firstLevelCache;

    public void setFirstLevelCache(final Ehcache firstLevelCache) {
        this.firstLevelCache = firstLevelCache;
    }

    @Override
    public long getCacheHits() {
        return cacheHits.get();
    }

    @Override
    public long getCacheMisses() {
        return cacheMisses.get();
    }

    @Override
    public double getCacheHitRate() {
        return hitRate(cacheHits, cacheMisses);
    }

    @Override
    public double getCacheMissRate() {
        return missRate(cacheHits, cacheMisses);
    }

    @Override
    public long getFirstLevelCachePuts() {
        return firstLevelCachePuts.get();
    }

    @Override
    public long getFirstLevelCacheHits() {
        return firstLevelCacheHits.get();
    }

    @Override
    public long getFirstLevelCacheMisses() {
        return firstLevelCacheMisses.get();
    }

    @Override
    public double getFirstLevelCacheHitRate() {
        return hitRate(firstLevelCacheHits, firstLevelCacheMisses);
    }

    @Override
    public double getFirstLevelCacheMissRate() {
        return missRate(firstLevelCacheHits, firstLevelCacheMisses);
    }

    public int getFirstLevelCacheSize() {
        return firstLevelCache.getSize();
    }

    public int getFirstLevelCacheMaxSize() {
        return new Long(firstLevelCache.getCacheConfiguration().getMaxEntriesLocalHeap()).intValue()
                + (int)firstLevelCache.getCacheConfiguration().getMaxEntriesLocalDisk();
    }

    @Override
    public long getFirstLevelCacheTimeToLiveSeconds() {
        return firstLevelCache.getCacheConfiguration().getTimeToLiveSeconds();
    }

    @Override
    public long getFirstLevelCacheTimeToIdleSeconds() {
        return firstLevelCache.getCacheConfiguration().getTimeToIdleSeconds();
    }

    @Override
    public void resetFirstLevelCache() {
        firstLevelCachePuts.set(0);
        firstLevelCacheHits.set(0);
        firstLevelCacheMisses.set(0);
    }

    @Override
    public long getSecondLevelCachePuts() {
        return secondLevelCachePuts.get();
    }

    @Override
    public long getSecondLevelCacheHits() {
        return secondLevelCacheHits.get();
    }

    @Override
    public long getSecondLevelCacheMisses() {
        return secondLevelCacheMisses.get();
    }

    @Override
    public double getSecondLevelCacheHitRate() {
        return hitRate(secondLevelCacheHits, secondLevelCacheMisses);
    }

    @Override
    public double getSecondLevelCacheMissRate() {
        return missRate(secondLevelCacheHits, secondLevelCacheMisses);
    }

    @Override
    public void resetSecondLevelCache() {
        secondLevelCachePuts.set(0);
        secondLevelCacheHits.set(0);
        secondLevelCacheMisses.set(0);
    }

    @Override
    public long getStaleCachePuts() {
        return staleCachePuts.get();
    }

    @Override
    public long getStaleCacheHits() {
        return staleCacheHits.get();
    }

    @Override
    public long getStaleCacheMisses() {
        return staleCacheMisses.get();
    }

    @Override
    public double getStaleCacheHitRate() {
        return hitRate(staleCacheHits, staleCacheMisses);
    }

    @Override
    public double getStaleCacheMissRate() {
        return missRate(staleCacheHits, staleCacheMisses);
    }

    @Override
    public void resetStaleCache() {
        staleCachePuts.set(0);
        staleCacheHits.set(0);
        staleCacheMisses.set(0);
    }

    public void incrementCacheHits() {
        cacheHits.incrementAndGet();
    }

    public void incrementCacheMisses() {
        cacheMisses.incrementAndGet();
    }

    public void incrementFirstLevelCachePuts() {
        firstLevelCachePuts.incrementAndGet();
    }

    public void incrementFirstLevelCacheHits() {
        firstLevelCacheHits.incrementAndGet();
    }

    public void incrementFirstLevelCacheMisses() {
        firstLevelCacheMisses.incrementAndGet();
    }

    public void incrementSecondLevelCachePuts() {
        secondLevelCachePuts.incrementAndGet();
    }

    public void incrementSecondLevelCacheHits() {
        secondLevelCacheHits.incrementAndGet();
    }

    public void incrementSecondLevelCacheMisses() {
        secondLevelCacheMisses.incrementAndGet();
    }

    public void incrementStaleCachePuts() {
        staleCachePuts.incrementAndGet();
    }

    public void incrementStaleCacheHits() {
        staleCacheHits.incrementAndGet();
    }

    public void incrementStaleCacheMisses() {
        staleCacheMisses.incrementAndGet();
    }

    private double hitRate(final AtomicLong hits, final AtomicLong misses) {
        long hitNr = hits.get();
        long missNr = misses.get();
        if (hitNr == 0 && missNr == 0) {
            return 0;
        }
        return ( (hitNr * 1.0D) / (hitNr + missNr));
    }

    private double missRate(final AtomicLong hits, final AtomicLong misses) {
        long hitNr = hits.get();
        long missNr = misses.get();
        if (hitNr == 0 && missNr == 0) {
            return 0;
        }
        return 1 - hitRate(hits, misses);
    }

}
