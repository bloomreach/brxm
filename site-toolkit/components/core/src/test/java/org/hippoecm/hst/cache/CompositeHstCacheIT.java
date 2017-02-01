/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cache;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.ehcache.EhCacheCache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CompositeHstCacheIT extends AbstractSpringTestCase {

    public static final String DEFAULT_PAGE_CACHE_SPRING_BEAN_ID = "pageCache";

    private CompositeHstCache compositeHstCache;
    private Ehcache secondLevelEhCache;
    private EhCacheCache secondLevelPageCache;
    private Ehcache stalePageEhCache;
    private EhCacheCache stalePageCache;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        compositeHstCache = getComponent(DEFAULT_PAGE_CACHE_SPRING_BEAN_ID);
        secondLevelEhCache = HstServices.getComponentManager().getComponent("secondLevelPageCache");
        secondLevelPageCache = new EhCacheCache(secondLevelEhCache);
        stalePageEhCache = HstServices.getComponentManager().getComponent("stalePageCache");
        stalePageCache = new EhCacheCache(stalePageEhCache);
    }

    @After
    public void tearDown() throws Exception {
        compositeHstCache.cacheStats.resetAll();
        compositeHstCache.ehcache.removeAll();
        secondLevelEhCache.removeAll();
        super.tearDown();
    }


    @Test
    public void only_primary_cache_tests() throws Exception {
        CacheElement element = compositeHstCache.createElement("key1", "value1");
        assertNotNull(element);
        compositeHstCache.put(element);
        assertTrue(compositeHstCache.isKeyInCache("key1"));
    }

    @Test
    public void put_in_primary_cache_also_puts_in_second_level_cache_however_with_different_time_to_live() {
        compositeHstCache.setSecondLevelCache(secondLevelPageCache);
        String key = "key";
        CacheElement cacheElememt = compositeHstCache.createElement(key, "content");
        compositeHstCache.put(cacheElememt);

        // assert item is in first level cache with time to live of 1 day, see SpringComponentManager-cache.xml


        CacheElement element = compositeHstCache.get(key);
        assertEquals("First level entries are cached by default for 1 day", 3600, element.getTimeToLiveSeconds());

        // assert item is in first level cache with time to live of 1 day, see SpringComponentManager-cache.xml

        CacheElement firstLevelCacheElement = compositeHstCache.get(key);
        assertEquals("First level entries are cached by default for 1 day", 3600, firstLevelCacheElement.getTimeToLiveSeconds());

        Element secondLevelEhCacheElement = secondLevelEhCache.get(key);
        assertTrue(secondLevelEhCacheElement != null);

        // second level cache does have TimeToLive of 3 minutes instead of 1 day (see CompositeHstCacheIT.xml)
        assertEquals("Second level entries are cached by default for 1 day", 180, secondLevelEhCacheElement.getTimeToLive());

    }

    @Test
    public void multiple_gets_for_present_key_result_only_in_primary_cache_hit() {
        compositeHstCache.setSecondLevelCache(secondLevelPageCache);
        String key = "key";
        compositeHstCache.put(compositeHstCache.createElement(key, "content"));

        assertEquals(compositeHstCache.cacheStats.getCacheHits(), 0);
        assertEquals(compositeHstCache.cacheStats.getFirstLevelCachePuts(), 1);
        assertEquals(compositeHstCache.cacheStats.getFirstLevelCacheHits(), 0);
        assertEquals(compositeHstCache.cacheStats.getSecondLevelCacheHits(), 0);

        compositeHstCache.get(key);

        assertEquals(compositeHstCache.cacheStats.getCacheHits(), 1);
        assertEquals(compositeHstCache.cacheStats.getFirstLevelCachePuts(), 1);
        assertEquals(compositeHstCache.cacheStats.getFirstLevelCacheHits(), 1);
        assertEquals(compositeHstCache.cacheStats.getSecondLevelCacheHits(), 0);

        compositeHstCache.get(key);

        assertEquals(compositeHstCache.cacheStats.getCacheHits(), 2);
        assertEquals(compositeHstCache.cacheStats.getFirstLevelCachePuts(), 1);
        assertEquals(compositeHstCache.cacheStats.getFirstLevelCacheHits(), 2);
        assertEquals(compositeHstCache.cacheStats.getSecondLevelCacheHits(), 0);

        compositeHstCache.ehcache.remove(key);

        // get results in hit in second level cache and restores first level cache (thus extra put)
        compositeHstCache.get(key);

        assertEquals(compositeHstCache.cacheStats.getCacheHits(), 3);
        assertEquals(compositeHstCache.cacheStats.getFirstLevelCachePuts(), 2);
        assertEquals(compositeHstCache.cacheStats.getFirstLevelCacheHits(), 2);
        assertEquals(compositeHstCache.cacheStats.getSecondLevelCacheHits(), 1);
    }

    @Test
    public void is_key_in_cache_on_composite_cache_only_check_primary_cache() {

        compositeHstCache.setSecondLevelCache(secondLevelPageCache);
        String key = "key";
        compositeHstCache.put(compositeHstCache.createElement(key, "content"));
        compositeHstCache.remove(key);
        assertFalse("Primary cache does not have the 'key' entry any more", compositeHstCache.isKeyInCache(key));
        // though a #get does result in a cached result since the second level cache returns it.
        assertTrue(compositeHstCache.get(key) != null);
        // the get(key) above has restored the entry in primary cache
        assertTrue("Primary cache should again have the 'key' entry ", compositeHstCache.isKeyInCache(key));
    }

    @Test
    public void remove_key_or_clear_on_composite_cache_only_removes_first_level_cache_entries() {
        compositeHstCache.setSecondLevelCache(secondLevelPageCache);
        String key = "key";
        compositeHstCache.put(compositeHstCache.createElement(key, "content"));
        compositeHstCache.remove(key);

        //
        assertFalse(compositeHstCache.isKeyInCache(key));

        // assert secondLevelPageCache still contains 'key'
        assertTrue(secondLevelPageCache.get(key) != null);
        // remove from second level page cache
        secondLevelEhCache.remove(key);

        compositeHstCache.put(compositeHstCache.createElement(key, "content"));
        compositeHstCache.clear();

        assertTrue(secondLevelPageCache.get(key) != null);
    }


    @Test
    public void second_level_cache_get_results_in_restore_to_primary_cache_with_custom_time_to_live() throws Exception {

        compositeHstCache.setSecondLevelCache(secondLevelPageCache);

        // populate the second level page compositeHstCache
        final String key = "key";
        final String content = "content";
        final int timeToLiveSeconds = 3;
        secondLevelPageCache.put(key, new Element(key, content, timeToLiveSeconds, timeToLiveSeconds));

        assertFalse("Primary cache should not have the key", compositeHstCache.isKeyInCache(key));
        assertFalse("Primary cache should not have the key", compositeHstCache.ehcache.isKeyInCache(key));

        // sleep 1 second to make sure when the item is move to primary cache, it gets a smaller timeToLiveIdle
        Thread.sleep(1000);
        // get should result in fetch from second level cache
        CacheElement element = compositeHstCache.get(key);

        assertEquals(element.getContent(), content);
        // since the element has *originated* from second level cache, and that entry had a timeToLiveSeconds of 3 seconds,
        // the primary cache should get a timeToLiveSeconds of also at most 3 seconds MINUS the 1 second delay we built in
        // BETWEEN putting the entry in second level cache and retrieving it (which restores it in first level cache
        // with a RE-COMPUTATION how long it still is allowed to live
        assertTrue(element.getTimeToLiveSeconds() <= 2);
        assertTrue(element.getTimeToIdleSeconds() <= 2);

        // check the timeToLive value of second level entry: That should still be 3 seconds (it does not countdown)
        Element secondLevelElement = secondLevelPageCache.get(key, Element.class);
        assertEquals("TimeToLive of the second level entry should stay 3", secondLevelElement.getTimeToLive(), 3);


        assertTrue("After compositeHstCache.get(key), primary cache now should have the element for key",
                compositeHstCache.ehcache.isKeyInCache(key));
        assertTrue("After compositeHstCache.get(key), primary cache now should have the element for key",
                compositeHstCache.isKeyInCache(key));

        Element primaryCacheElement = compositeHstCache.ehcache.get(key);

        // assert that primaryCacheElement is created at least 1 second later than secondLevelElement because of  Thread.sleep(1000);
        assertTrue(primaryCacheElement.getCreationTime() >= secondLevelElement.getCreationTime() + 1000 );

    }

    @Test
    public void stale_cache_time_to_live_logic() throws Exception {
        compositeHstCache.setStaleCache(stalePageCache);
        String key = "key";
        // trigger a new entry in stale cache
        compositeHstCache.put(compositeHstCache.createElement(key, "content"));

        /*
          Below is confusing BUT

          stalePageCache.get(key, Element.class);

          returns the value for 'key' which is the Primary Cache Element

          whereas

          Element staleElement = stalePageEhCache.get(key);

          returns the Element as it *is* in the statePage cache (with the TTL of the stale page cache configuration!)
         */
        Element staleCachedPrimaryElement = stalePageCache.get(key, Element.class);
        assertTrue("Put in composite cache should result in put in stale page cache as well", staleCachedPrimaryElement != null);
        assertEquals("The TTL of stale cached Element should be the TTL of primary page cache config because it is the Element itself from " +
                "primary cache that is returned.", compositeHstCache.ehcache.getCacheConfiguration().getTimeToLiveSeconds(), staleCachedPrimaryElement.getTimeToLive());


        Element staleElement = stalePageEhCache.get(key);
        assertEquals("The TTL of stale elements *in* the stale cache should be the TTL of stale page cache config and " +
                        "not the TTL of primary cached element.",
                stalePageEhCache.getCacheConfiguration().getTimeToLiveSeconds(), staleElement.getTimeToLive());

    }


    @Test
    public void stale_cache_restores_to_primary_cache_tests() throws Exception {
        compositeHstCache.setStaleCache(stalePageCache);
        String key = "key";
        compositeHstCache.put(compositeHstCache.createElement(key, "content"));

        compositeHstCache.ehcache.remove(key);

        // a get on the composite cache should result in the following behavior:
        // The first #get returns null (in real app, then that thread is going to recreate the result)
        // The first #get triggers the stale cached entry to be restored in primary cache *releasing* the lock on #get(key)
        // Consequent #gets should return the value again from primary cache

        assertEquals(compositeHstCache.cacheStats.getStaleCachePuts(), 1);
        assertEquals(compositeHstCache.cacheStats.getFirstLevelCachePuts(), 1);
        assertEquals(compositeHstCache.cacheStats.getStaleCachePuts(), 1);

        CacheElement result = compositeHstCache.get(key);
        assertNull("the first #get should restore the stale cached entry in primary cache and release the lock on get(key) " +
                "but should never return the result! The result will in general be recreated by the calling thread and " +
                "later replace the restored stale result.", result);

        CacheElement result2 = compositeHstCache.get(key);
        assertNotNull("The stale cached entry should be now returned", result2);
        assertNotNull("The state entry should be restored in primary cache",compositeHstCache.ehcache.get(key));

    }

    /**
     * This test below validates an *extremely* important artifact of the stale cache logic: The stale cache logic works
     * as follows: If the #get(key) is done on the composite cache, first the primary cache is checked: This #get(key)
     * locks *all* other requests on the same #get(key). One thread can proceed. This thread will restore the stale cached
     * entry in primary cache AND return null for get(key) : This way, that thread thus has a cache miss hit and can recreate
     * the cached response (by a real application) that in the ends gets stored again in cache replacing the stale value
     * with the newly generated value
     */
    @Test
    public void concurrent_gets_when_primary_entry_missing_but_stale_entry_present_results_in_one_null_result_and_others_get_stale() throws Exception {
        compositeHstCache.setStaleCache(stalePageCache);
        final String key = "key";
        compositeHstCache.put(compositeHstCache.createElement(key, "content"));
        compositeHstCache.ehcache.remove(key);

        assertEquals(1, compositeHstCache.cacheStats.getFirstLevelCachePuts());
        assertEquals(1, compositeHstCache.cacheStats.getStaleCachePuts());
        assertEquals(0, compositeHstCache.cacheStats.getStaleCacheHits());

        final int workerCount = 1000;
        final int workerThreads = 50;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(workerCount);

        ExecutorService executorService = Executors.newFixedThreadPool(workerThreads);
        for (int i = 0 ; i < workerCount; i++) {
            executorService.submit((Runnable)() -> {
                try {
                    startSignal.await();
                } catch (InterruptedException e) {

                }
                compositeHstCache.get(key);
                doneSignal.countDown();
            });
        }

        startSignal.countDown();
        doneSignal.await();

        assertEquals("One new put from stale expected.",2, compositeHstCache.cacheStats.getFirstLevelCachePuts());
        assertEquals(1, compositeHstCache.cacheStats.getStaleCachePuts());
        assertEquals(1, compositeHstCache.cacheStats.getStaleCacheHits());

        assertEquals("Expected that of the 1000 worker count, only 1 had a cache miss, the rest should had " +
                "get a stale cache hit from primary cache.", 999, compositeHstCache.cacheStats.getFirstLevelCacheHits());
    }


    // TODO MODIFY THE ELEMENT.CREATION TIME TO FORCE STALE CACHE POPULATION

}
