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
import java.util.concurrent.TimeUnit;

import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CompositeHstCacheSpringWiringIT extends AbstractSpringTestCase {

    // the following spring bean ids *MUST* be wired and the names are not allowed to be changed
    public static final String NOOP_CACHE_SPRING_BEAN_ID = "noopCache";
    public static final String DEFAULT_BINARIES_CACHE_SPRING_BEAN_ID = "defaultBinariesCache";
    public static final String DEFAULT_PAGE_CACHE_SPRING_BEAN_ID = "pageCache";
    public static final String DEFAULT_WEBFILE_CACHE_SPRING_BEAN_ID = "webFileCache";

    @Test
    public void assert_NoopCache_gets_wired() throws Exception {
        HstCache noopCache = getComponent(NOOP_CACHE_SPRING_BEAN_ID);
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
            HstCache cache = getComponent(cacheBeanId);
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
    public void assert_caches_are_blocking() throws Exception {

        for (String cacheBeanId : new String[]{DEFAULT_BINARIES_CACHE_SPRING_BEAN_ID,
                DEFAULT_PAGE_CACHE_SPRING_BEAN_ID, DEFAULT_WEBFILE_CACHE_SPRING_BEAN_ID}) {

            HstCache cache = getComponent(cacheBeanId);

            cache.get("key1");

            // cache should now be blocked on 'key1'

            CountDownLatch doneSignal = new CountDownLatch(1);

            new Thread(new Worker(cache, "key1", doneSignal)).start();

            boolean countReachedZero = doneSignal.await(100, TimeUnit.MILLISECONDS);

            assertFalse("Since a #get on blocking cache is blocking other threads, the" +
                    " count down latch should not be able to reach 0.", countReachedZero);

        }
    }

    class Worker implements Runnable {

        private HstCache cache;
        private String key;
        private CountDownLatch doneSignal;

        Worker(final HstCache cache, final String key, final CountDownLatch doneSignal) {
            this.cache = cache;
            this.key = key;
            this.doneSignal = doneSignal;
        }

        @Override
        public void run() {
            cache.get(key);
            doneSignal.countDown();
        }
    }
}
