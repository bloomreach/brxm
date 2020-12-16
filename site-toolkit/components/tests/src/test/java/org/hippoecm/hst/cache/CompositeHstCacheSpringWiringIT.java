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
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CompositeHstCacheSpringWiringIT extends CompositeHstCacheIT {

    // the following spring bean ids *MUST* be wired and the names are not allowed to be changed
    public static final String NOOP_CACHE_SPRING_BEAN_ID = "noopCache";
    public static final String DEFAULT_BINARIES_CACHE_SPRING_BEAN_ID = "defaultBinariesCache";
    public static final String DEFAULT_PAGE_CACHE_SPRING_BEAN_ID = "pageCache";
    public static final String DEFAULT_WEBFILE_CACHE_SPRING_BEAN_ID = "webFileCache";

    @Test
    public void assert_NoopCache_gets_wired() throws Exception {
        HstCache noopCache = componentManager.getComponent(NOOP_CACHE_SPRING_BEAN_ID);
        assertNotNull(noopCache);
        CacheElement element = noopCache.createElement("key1", "content1");
        assertNotNull(element);
        noopCache.put(element);
        assertFalse(noopCache.isKeyInCache("key1"));
        assertNull(noopCache.get("key1"));
    }

    @Test
    public void assert_caches_get_wired() throws Exception {
        for (String cacheBeanId : new String[]{DEFAULT_BINARIES_CACHE_SPRING_BEAN_ID,
                DEFAULT_PAGE_CACHE_SPRING_BEAN_ID, DEFAULT_WEBFILE_CACHE_SPRING_BEAN_ID}) {
            HstCache cache = componentManager.getComponent(cacheBeanId);
            assertNotNull(cache);
            CacheElement element = cache.createElement("key1", "content1");
            assertNotNull(element);
            cache.put(element);
            assertTrue(cache.isKeyInCache("key1"));
            element = cache.get("key1");
            assertNotNull(element);
            assertEquals("content1", element.getContent());
            cache.remove("key1");
            assertNull(cache.get("key1"));
        }

    }

    @Test
    public void assert_get_on_missing_key_on_caches_is_blocking() throws Exception {

        for (String cacheBeanId : new String[]{DEFAULT_BINARIES_CACHE_SPRING_BEAN_ID,
                DEFAULT_PAGE_CACHE_SPRING_BEAN_ID, DEFAULT_WEBFILE_CACHE_SPRING_BEAN_ID}) {

            HstCache cache = componentManager.getComponent(cacheBeanId);

            String key = "key";
            cache.get(key);
            // cache should now be blocked for other thread on the same #get for 'key' because it is not yet present

            CountDownLatch doneSignal = new CountDownLatch(1);

            new Thread(() -> {
                cache.get(key);
                doneSignal.countDown();
            }).start();

            boolean countReachedZero = doneSignal.await(100, TimeUnit.MILLISECONDS);

            assertFalse("Since a #get on blocking cache is blocking other threads when the key is not present, the" +
                    " count down latch should not be able to reach 0.", countReachedZero);


        }
    }

    @Test
    public void assert_get_on_present_key_on_caches_is_not_blocking() throws Exception {

        for (String cacheBeanId : new String[]{DEFAULT_BINARIES_CACHE_SPRING_BEAN_ID,
                DEFAULT_PAGE_CACHE_SPRING_BEAN_ID, DEFAULT_WEBFILE_CACHE_SPRING_BEAN_ID}) {

            HstCache cache = componentManager.getComponent(cacheBeanId);

            String key = "key";
            cache.put(cache.createElement(key, "value"));

            cache.get(key);
            // cache should now not be blocked on 'key' because it is present

            CountDownLatch doneSignal = new CountDownLatch(1);

            new Thread(() -> {
                cache.get(key);
                doneSignal.countDown();
            }).start();

            boolean countReachedZero = doneSignal.await(100, TimeUnit.MILLISECONDS);

            assertTrue("Since a #get on blocking cache is not blocking other threads when the key is present, the" +
                    " count down latch should not be able to reach 0.", countReachedZero);

            cache.remove("key");
        }
    }

    @Test
    public void assert_is_key_in_cache_on_cache_is_not_blocking() throws Exception {
        HstCache cache = componentManager.getComponent(DEFAULT_PAGE_CACHE_SPRING_BEAN_ID);

        String key = "key";
        cache.get(key);

        // cache should now be blocked on 'key'

        CountDownLatch doneSignal = new CountDownLatch(1);

        new Thread(() -> {
            cache.isKeyInCache(key);
            doneSignal.countDown();
        }).start();

        boolean countReachedZero = doneSignal.await(100, TimeUnit.MILLISECONDS);
        assertTrue("#isKeyInCache should not be blocking ", countReachedZero);
    }


}
