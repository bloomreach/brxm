/*
 *  Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.springframework.cache.Cache;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.mock.web.MockServletContext;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms7.services.context.HippoWebappContext.Type.SITE;


/**
 * Do not inherit from AbstractSpringTestCase since that test reuses the SAME HST Spring components per method but
 * for the cache tests, we want fresh HST Spring Caches per method. Although we do restart the Spring component
 * manager on each method, this one does not include the entire HST Spring configs and as a result does not start
 * up a repository for example
 */
public class CompositeHstCacheIT {

    public static final String DEFAULT_PAGE_CACHE_SPRING_BEAN_ID = "pageCache";

    private CompositeHstCache compositeHstCache;
    private Ehcache secondLevelEhCache;
    private EhCacheCache secondLevelPageCache;
    private Ehcache stalePageEhCache;
    private EhCacheCache stalePageCache;

    protected SpringComponentManager componentManager;

    protected static HippoWebappContext webappContext = new HippoWebappContext(SITE, new MockServletContext() {
        public String getContextPath() {
            return "/site";
        }
    });

    @Before
    public void setUp() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");

        final Configuration containerConfiguration = new PropertiesConfiguration();
        containerConfiguration.addProperty("hst.configuration.rootPath", "/hst:hst");
        componentManager = new SpringComponentManager(containerConfiguration);

        final String[] configs = new String[]{CompositeHstCacheIT.class.getName().replace(".", "/") + ".xml"};

        componentManager.setConfigurationResources(configs);

        HippoWebappContextRegistry.get().register(webappContext);

        componentManager.setServletContext(webappContext.getServletContext());
        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(componentManager);

        compositeHstCache = componentManager.getComponent(DEFAULT_PAGE_CACHE_SPRING_BEAN_ID);
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
        if (componentManager != null) {
            componentManager.stop();
            componentManager.close();
            HstServices.setComponentManager(null);
        }
        HippoWebappContextRegistry.get().unregister(webappContext);
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
        try {
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
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
        }
    }

    @Test
    public void multiple_gets_for_present_key_result_only_in_primary_cache_hit() {
        try {
            compositeHstCache.setSecondLevelCache(secondLevelPageCache);
            String key = "key";
            compositeHstCache.put(compositeHstCache.createElement(key, "content"));

            assertEquals(0, compositeHstCache.cacheStats.getCacheHits());
            assertEquals(1, compositeHstCache.cacheStats.getFirstLevelCachePuts());
            assertEquals(0, compositeHstCache.cacheStats.getFirstLevelCacheHits());
            assertEquals(0, compositeHstCache.cacheStats.getSecondLevelCacheHits());

            compositeHstCache.get(key);

            assertEquals(1, compositeHstCache.cacheStats.getCacheHits());
            assertEquals(1, compositeHstCache.cacheStats.getFirstLevelCachePuts());
            assertEquals(1, compositeHstCache.cacheStats.getFirstLevelCacheHits());
            assertEquals(0, compositeHstCache.cacheStats.getSecondLevelCacheHits());

            compositeHstCache.get(key);

            assertEquals(2, compositeHstCache.cacheStats.getCacheHits());
            assertEquals(1, compositeHstCache.cacheStats.getFirstLevelCachePuts());
            assertEquals(2, compositeHstCache.cacheStats.getFirstLevelCacheHits());
            assertEquals(0, compositeHstCache.cacheStats.getSecondLevelCacheHits());

            compositeHstCache.ehcache.remove(key);

            // get results in hit in second level cache and restores first level cache (thus extra put)
            CacheElement cacheElement = compositeHstCache.get(key);
            assertNotNull("Hit from second level cache should be returned. ", cacheElement);

            assertEquals(3, compositeHstCache.cacheStats.getCacheHits());
            assertEquals(2, compositeHstCache.cacheStats.getFirstLevelCachePuts());
            assertEquals(2, compositeHstCache.cacheStats.getFirstLevelCacheHits());
            assertEquals(1, compositeHstCache.cacheStats.getSecondLevelCacheHits());
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
        }
    }

    @Test
    public void is_key_in_cache_on_composite_cache_only_check_primary_cache() {
        try {
            compositeHstCache.setSecondLevelCache(secondLevelPageCache);
            String key = "key";
            compositeHstCache.put(compositeHstCache.createElement(key, "content"));
            compositeHstCache.remove(key);
            assertFalse("Primary cache does not have the 'key' entry any more", compositeHstCache.isKeyInCache(key));
            // though a #get does result in a cached result since the second level cache returns it.
            assertTrue(compositeHstCache.get(key) != null);
            // the get(key) above has restored the entry in primary cache
            assertTrue("Primary cache should again have the 'key' entry ", compositeHstCache.isKeyInCache(key));
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
        }
    }

    @Test
    public void remove_key_or_clear_on_composite_cache_only_removes_first_level_cache_entries() {
        try {
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
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
        }
    }


    @Test
    public void second_level_cache_get_results_in_restore_to_primary_cache_with_custom_time_to_live() throws Exception {

        try {
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

            assertEquals(content, element.getContent());
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
            assertTrue(primaryCacheElement.getCreationTime() >= secondLevelElement.getCreationTime() + 1000);
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
        }
    }

    @Test
    public void stale_cache_time_to_live_logic() throws Exception {
        try {
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
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
        }
    }


    @Test
    public void stale_cache_restores_to_primary_cache_tests() throws Exception {
        try {
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

            assertNotNull("The state entry should be restored in primary cache", compositeHstCache.ehcache.get(key));

            CacheElement result2 = compositeHstCache.get(key);
            assertNotNull("The stale cached entry should be now returned", result2);
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
        }
    }

    /**
     * This test below validates an *extremely* important artifact of the stale cache logic: The stale cache logic works
     * as follows: If the #get(key) is done on the composite cache, first the primary cache is checked: This #get(key)
     * locks *all* other requests on the same #get(key). One thread can proceed. This thread will restore the stale
     * cached entry in primary cache AND return null for get(key) : This way, that thread thus has a cache miss hit and
     * can recreate the cached response (by a real application) that in the ends gets stored again in cache replacing
     * the stale value with the newly generated value
     */
    @Test
    public void concurrent_gets_when_primary_entry_missing_but_stale_entry_present_results_in_one_null_result_and_others_get_stale() throws Exception {
        try {
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
            for (int i = 0; i < workerCount; i++) {
                executorService.submit((Runnable) () -> {
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

            assertEquals("One new put from stale expected.", 2, compositeHstCache.cacheStats.getFirstLevelCachePuts());
            assertEquals(1, compositeHstCache.cacheStats.getStaleCachePuts());
            assertEquals(1, compositeHstCache.cacheStats.getStaleCacheHits());

            assertEquals("Expected that of the 1000 worker count, only 1 had a cache miss, the rest should had " +
                    "get a stale cache hit from primary cache.", 999, compositeHstCache.cacheStats.getFirstLevelCacheHits());
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
        }
    }


    @Test
    public void stale_and_second_level_cache_tests() throws Exception {
        try {
            compositeHstCache.setSecondLevelCache(secondLevelPageCache);
            compositeHstCache.setStaleCache(stalePageCache);

            final String key = "key";
            compositeHstCache.put(compositeHstCache.createElement(key, "content"));

            assertEquals(1, compositeHstCache.cacheStats.getFirstLevelCachePuts());
            assertEquals(1, compositeHstCache.cacheStats.getSecondLevelCachePuts());
            assertEquals(1, compositeHstCache.cacheStats.getStaleCachePuts());

            assertTrue(compositeHstCache.isKeyInCache(key));
            assertTrue(compositeHstCache.ehcache.isKeyInCache(key));
            assertTrue(secondLevelEhCache.isKeyInCache(key));
            assertTrue(stalePageEhCache.isKeyInCache(key));
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
            compositeHstCache.setStaleCache((Cache) null);
        }
    }

    @Test
    public void second_level_cached_entry_has_precedence_over_stale() throws Exception {
        try {
            compositeHstCache.setSecondLevelCache(secondLevelPageCache);
            compositeHstCache.setStaleCache(stalePageCache);

            final String key = "key";
            compositeHstCache.put(compositeHstCache.createElement(key, "content"));

            // remove from primary cache
            compositeHstCache.remove(key);

            // override the second level cached value (this does not impact compositeHstCache.cacheStats
            final String SECOND_LEVEL_CONTENT = "second_level_content";
            // valid for 180 seconds
            secondLevelPageCache.put(key, new Element(key, SECOND_LEVEL_CONTENT, 180, 180));

            CacheElement cached = compositeHstCache.get(key);
            assertEquals("second level cache entry should have been restored.", SECOND_LEVEL_CONTENT, cached.getContent());

            assertEquals(2, compositeHstCache.cacheStats.getFirstLevelCachePuts());
            assertEquals(1, compositeHstCache.cacheStats.getSecondLevelCachePuts());
            assertEquals(1, compositeHstCache.cacheStats.getSecondLevelCacheHits());
            assertEquals(1, compositeHstCache.cacheStats.getStaleCachePuts());
            assertEquals(0, compositeHstCache.cacheStats.getStaleCacheHits());
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
            compositeHstCache.setStaleCache((Cache) null);
        }
    }


    @Test
    public void stale_value_is_restored_if_second_level_entry_is_expired_from_second_level_cache_global_TTL() throws Exception {
        try {
            compositeHstCache.setSecondLevelCache(secondLevelPageCache);
            compositeHstCache.setStaleCache(stalePageCache);

            final String key = "key";
            String CONTENT = "content";
            compositeHstCache.put(compositeHstCache.createElement(key, CONTENT));

            // remove from primary cache
            compositeHstCache.remove(key);

            final String SECOND_LEVEL_CONTENT = "second_level_content";
            int TIME_TO_LIVE_IDLE_SECONDS = 1;

            secondLevelEhCache.put(new Element(key, SECOND_LEVEL_CONTENT, TIME_TO_LIVE_IDLE_SECONDS, TIME_TO_LIVE_IDLE_SECONDS));

            // Note that above results in something else than
            // secondLevelPageCache.put(key, new Element(key, SECOND_LEVEL_CONTENT, TIME_TO_LIVE_IDLE_SECONDS, TIME_TO_LIVE_IDLE_SECONDS));
            // secondLevelEhCache.put adds a new cache entry that expires *after* 1 second, while the secondLevelPageCache.put
            // adds a new cache entry that expires after 'second level cache TTL' and has an Element as cached value that is
            // new Element(key, SECOND_LEVEL_CONTENT, TIME_TO_LIVE_IDLE_SECONDS, TIME_TO_LIVE_IDLE_SECONDS)
            // Also see the next unit test method, which uses secondLevelPageCache.put instead of secondLevelEhCache.put

            Thread.sleep(1010);

            assertNull("should be expired from cache.", secondLevelEhCache.get(key));

            CacheElement cached = compositeHstCache.get(key);
            // since second level entry should be expired from the cache by the global TTL, stale entry is restored and after the first
            // compositeHstCache.get("key"); which will return null, it should return the stale response again
            assertNull("First get hitting stale cache should inject stale value in primary and return null.", cached);

            CacheElement secondTime = compositeHstCache.get(key);
            assertNotNull(secondTime);
            // stale content
            assertEquals(CONTENT, secondTime.getContent());
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
            compositeHstCache.setStaleCache((Cache) null);
        }
    }

    @Test
    public void if_second_level_entry_is_present_but_has_expired_element_as_value_the_entry_is_treated_as_stale() throws Exception {
        try {
            compositeHstCache.setSecondLevelCache(secondLevelPageCache);
            compositeHstCache.setStaleCache(stalePageCache);

            final String key = "key";
            String CONTENT = "content";
            compositeHstCache.put(compositeHstCache.createElement(key, CONTENT));

            // remove from primary cache
            compositeHstCache.remove(key);

            final String SECOND_LEVEL_CONTENT = "second_level_content";
            int TIME_TO_LIVE_IDLE_SECONDS = 1;

            secondLevelPageCache.put(key, new Element(key, SECOND_LEVEL_CONTENT, TIME_TO_LIVE_IDLE_SECONDS, TIME_TO_LIVE_IDLE_SECONDS));

            Thread.sleep(1010);

            Element secondLevelElement = secondLevelEhCache.get(key);
            assertNotNull("Second level element should have TTL of global secondLevelEhCache hence still present, *however*, the " +
                    "cached value that is returned does have a TTL of 1 second.", secondLevelElement);
            assertEquals(180L, secondLevelElement.getTimeToLive());
            assertEquals(1L, ((Element) secondLevelElement.getObjectValue()).getTimeToLive());

            CacheElement cached = compositeHstCache.get(key);

            assertNull("Although the second level cache still has the entry, the entry has an element indicating it is " +
                    "expired, hence should not be directly returned! Instead it should treat the element as stale, hence " +
                    "returning null on first request and returning the second level (stale) value on second get.", cached);

            CacheElement secondTime = compositeHstCache.get(key);
            // stale content
            assertEquals(SECOND_LEVEL_CONTENT, secondTime.getContent());
        } finally {
            compositeHstCache.setSecondLevelCache((Cache) null);
            compositeHstCache.setStaleCache((Cache) null);
        }
    }

}
