/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.configuration.cache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertTrue;



public class TestEventCache {

    @Test
    public void testReferenceQueue() throws Exception {
        Map<String, WeakReference<FinalizableTinyObject>> configurationObjectTagRegistry = new HashMap<>();
        final ReferenceQueue<FinalizableTinyObject> integerReferenceQueue = new ReferenceQueue<>();
        Counter finalizedObjectsCounter = new Counter();
        final int numberOfObjects = 1000;
        for (int i = 0 ; i < numberOfObjects; i++) {
            configurationObjectTagRegistry.put("test" + i, new WeakReference<>(new FinalizableTinyObject(finalizedObjectsCounter), integerReferenceQueue));
        }

        // make sure all WeakReference referents are GC-ed
        while (finalizedObjectsCounter.finalized.get() != numberOfObjects) {
            System.gc();
            Thread.sleep(100);
            System.gc();
        }
        int queueLength = 0;
        Reference<? extends FinalizableTinyObject> poll = integerReferenceQueue.poll();
        while (poll != null) {
            queueLength++;
            poll = integerReferenceQueue.poll();
        }
        assertEquals(queueLength, numberOfObjects);
    }

    @Test
    public void testWeakTaggedCache() throws InterruptedException {
        final WeakTaggedCache<FinalizableTinyKey, FinalizableTinyObject, String> weakTaggedCache = new WeakTaggedCache<>();
        final Counter finalizedObjectsCounter = new Counter();
        final Counter finalizedKeyCounter = new Counter();
        final int numberOfObjects = 1000;
        for (int i = 0 ; i < numberOfObjects; i++) {
            weakTaggedCache.put(new FinalizableTinyKey("key"+i, finalizedKeyCounter), new FinalizableTinyObject(finalizedObjectsCounter), "tag"+i);
        }

        // this one should not be able to be GC-ed
        FinalizableTinyObject tinyObject =  new FinalizableTinyObject(finalizedObjectsCounter);
        weakTaggedCache.put(new FinalizableTinyKey("unGarbagableKey", finalizedKeyCounter), tinyObject, "unGarbagableTag");
        while (finalizedObjectsCounter.finalized.get() != numberOfObjects) {
            System.gc();
            Thread.sleep(100);
            System.gc();
        }

        // the cache is only cleaned on next access
        assertEquals("size of key value map other than expected.", numberOfObjects + 1, weakTaggedCache.keyValueMap.size());
        assertEquals("size of value key map other than expected.", numberOfObjects + 1, weakTaggedCache.valueKeyMap.size());

        // since the cachekey are strongly referenced in the WeakTaggedCache and the WeakTaggedCache did not yet had a cleanup()
        // there still can't be finalized a single cachekey
        assertEquals(0, finalizedKeyCounter.finalized.get());

        // access the cache for force cleanup
        weakTaggedCache.get(new FinalizableTinyKey("foo", null));

        assertEquals(1, weakTaggedCache.keyValueMap.size());
        assertEquals(1, weakTaggedCache.valueKeyMap.size());

        // now, all items should have been evicted *EXCEPT* the one we still reference
        for (int i = 0 ; i < numberOfObjects; i++) {
            assertNull("since object is gc-ed, it should not be present any more", weakTaggedCache.get(new FinalizableTinyKey("key" + i, null)));
        }

        // the registry is only cleaned on next access when all keys have been GC-ed
        assertEquals(numberOfObjects + 1, weakTaggedCache.weakKeyTagRegistry.keyTagsMap.size());
        assertEquals(numberOfObjects + 1, weakTaggedCache.weakKeyTagRegistry.tagKeysMap.size());

        while (finalizedKeyCounter.finalized.get() != numberOfObjects) {
            System.gc();
            Thread.sleep(100);
            System.gc();
        }

        // only AFTER weakTaggedCache.get("foo") the STRONG KEYS have been removed from weakTaggedCache and now
        // have become available for the garbage collector. After they have been GC-ed, the weakKeyTagRegistry
        // should be cleaned up

        // access the cache for force cleanup
        weakTaggedCache.weakKeyTagRegistry.get("foo");
        assertEquals(1, weakTaggedCache.weakKeyTagRegistry.keyTagsMap.size());
        assertEquals(1, weakTaggedCache.weakKeyTagRegistry.tagKeysMap.size());


        assertNotNull(weakTaggedCache.get(new FinalizableTinyKey("unGarbagableKey", null)));
        assertTrue(tinyObject == weakTaggedCache.get(new FinalizableTinyKey("unGarbagableKey", null)));

        weakTaggedCache.evictKeysByTag("unGarbagableTag");
        assertNull(weakTaggedCache.get(new FinalizableTinyKey("unGarbagableKey", null)));

    }

    /**
     * keep the cache running for a while and store big keys and objects all time : This should
     * not lead to memory issues when the keys/objects are ready for gc
     */
    @Test
    public void testWeakTaggedCacheMemoryUsage() {
        final WeakTaggedCache<FinalizableBigKey, FinalizableBigObject, String> weakTaggedCache = new WeakTaggedCache<>();
        // per key and per object about 10 Mbyte, so 1000 * 20 Mbyte = 20 Gbyte should expose memory issues
        final int numberOfObjects = 1000;
        for (int i = 0 ; i < numberOfObjects; i++) {
            weakTaggedCache.put(new FinalizableBigKey("key"+i), new FinalizableBigObject(), "tag"+i);
        }
        assertTrue("No OOM", true);
    }

    @Test
    public void testDummy() {
        final WeakTaggedCache<FinalizableBigKey, FinalizableBigObject, String> weakTaggedCache = new WeakTaggedCache<>();
        // per key and per object about 10 Mbyte, so 1000 * 20 Mbyte = 20 Gbyte should expose memory issues
        final int numberOfObjects = 5;
        for (int i = 0 ; i < numberOfObjects; i++) {
            weakTaggedCache.put(new FinalizableBigKey("key"+i), new FinalizableBigObject(), "tag"+i);
        }
        assertTrue("No OOM", true);
    }


    public class FinalizableTinyKey {

        final Counter c;
        final String key;

        public FinalizableTinyKey(final String key, final Counter c) {
            this.c = c;
            this.key = key;
        }

        @Override
        protected void finalize() throws Throwable {
            c.finalized.incrementAndGet();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FinalizableTinyKey that = (FinalizableTinyKey) o;

            if (!key.equals(that.key)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    public class FinalizableBigKey {

        final String key;
        // about 10 Mbyte
        final byte[] bigMemoryTaker = new byte[10 * 1024 * 1024];

        public FinalizableBigKey(final String key) {
            this.key = key;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FinalizableBigKey that = (FinalizableBigKey) o;

            if (!key.equals(that.key)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }


    class FinalizableTinyObject{

        final Counter c;
        FinalizableTinyObject(final Counter c) {
            this.c = c;
        }

        @Override
        protected void finalize() throws Throwable {
            c.finalized.incrementAndGet();
        }
    }

    class FinalizableBigObject{

        // about 10 Mbyte
        final byte[] bigMemoryTaker = new byte[10 * 1024 * 1024];

    }

    class Counter {
        volatile AtomicInteger finalized = new AtomicInteger();
    }
}
