/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet.util;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.cache.ehcache.HstCacheEhCacheImpl;
import org.hippoecm.hst.servlet.utils.BinariesCache;
import org.hippoecm.hst.servlet.utils.BinaryPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BinariesCacheTest {
    
    HstCache hstBinariesCache;
    CacheManager cacheManager;
    BinaryPage page;
    BinariesCache bc;
    
    @Before
    public void setup() throws UnsupportedEncodingException {
        page = new BinaryPage("/content/binaries/my:pdffile");
        cacheManager = CacheManager.create();
        cacheManager.addCache("binariesCache");
        Ehcache ehBinariesCache = cacheManager.getCache("binariesCache");
        BlockingCache cache = new BlockingCache(ehBinariesCache);
        // after 10 ms a LockTimeoutException is thrown when a lock cannot be acquired
       // cache.setTimeoutMillis(10);
        hstBinariesCache = new HstCacheEhCacheImpl(cache);
        bc = new BinariesCache(hstBinariesCache);
    }
    
    @After
    public void tearDown() {
        cacheManager.shutdown();
    }

    @Test
    public void testPutGet() {
        bc.putPage(page);
        assertNotNull(bc.getPageFromBlockingCache(page.getResourcePath()));
    }

    @Test
    public void testIsCacheable() {
        page.setLength(BinariesCache.DEFAULT_MAX_OBJECT_SIZE_BYTES);
        assertTrue(bc.isBinaryDataCacheable(page));
    }
    
    @Test
    public void testIsNotCacheableNoSize() {
        page.setLength(-1L);
        assertFalse(bc.isBinaryDataCacheable(page));
    }

    @Test
    public void testIsNotCacheableTooLarge() {
        page.setLength(BinariesCache.DEFAULT_MAX_OBJECT_SIZE_BYTES + 1L);
        assertFalse(bc.isBinaryDataCacheable(page));
    }
    
    @Test
    public void testExpired() {
        page.setNextValidityCheckTime(System.currentTimeMillis() - 5000L);
        assertTrue(bc.mustCheckValidity(page));
    }

    @Test
    public void testNotExpired() {
        page.setNextValidityCheckTime(System.currentTimeMillis() + 5000L);
        assertFalse(bc.mustCheckValidity(page));
    }
    
    @Test
    public void testUpdateExpired() {
        page.setNextValidityCheckTime(System.currentTimeMillis() - 5000L);
        assertTrue(bc.mustCheckValidity(page));
        bc.updateNextValidityCheckTime(page);
        assertFalse(bc.mustCheckValidity(page));
    }

    @Test
    public void isStaleNoLastModified() {
        page.setLastModified(-1L);
        assertTrue(bc.isPageStale(page, 1234L));
    }

    @Test
    public void isStaleHasBeenModified() {
        page.setLastModified(4321L);
        assertTrue(bc.isPageStale(page, 1234L));
    }

    @Test
    public void isNotStale() {
        page.setLastModified(1234L);
        assertFalse(bc.isPageStale(page, 1234L));
    }


    @Test
    public void testGetNonNullIsNotLocking() {
        // PUT in cache so the item is available
        bc.putPage(page);

        Thread [] getters = new Thread[2];
        for (int i = 0; i < getters.length; i++) {
            getters[i] = new Thread(new Runnable() {
                public void run() {
                    BinaryPage elem = bc.getPageFromBlockingCache(page.getResourcePath());
                    assertNotNull(elem);
                }
            });
        }

        for (int i = 0; i < getters.length; i++) {
            getters[i].start();
        }

        try {
            for (int i = 0; i < getters.length; i++) {
                getters[i].join();
            }
        } catch (InterruptedException e) {
        }

    }


    @Test
    public void testGetNullIsLockingForEverWhenNoTimeoutSet() throws InterruptedException {
        // NO put so page.getResourcePath() is not available in cache
        final long WAIT_BEFORE_PUT = 100;
        final List<Throwable> throwables = Collections.synchronizedList(new LinkedList<Throwable>());
        final Thread [] getters = new Thread[2];
        final Semaphore firstOrSecond = new Semaphore(1);
        final Semaphore secondCacheAccess = new Semaphore(0);
        for (int i = 0; i < getters.length; i++) {
            getters[i] = new Thread(new Runnable() {
                public void run() {
                    if (firstOrSecond.tryAcquire()) {
                        BinaryPage elem = bc.getPageFromBlockingCache(page.getResourcePath());
                        secondCacheAccess.release();
                        try {
                            assertNull(elem);
                        } catch (AssertionError e) {
                            throwables.add(e);
                        }

                        // after WAIT_BEFORE_PUT ms we PUT an element that should free the lock again
                        try {
                            Thread.sleep(WAIT_BEFORE_PUT);
                            bc.putPage(page);
                        } catch (InterruptedException e) {

                        }
                    } else {
                        try {
                            secondCacheAccess.acquire();
                        } catch (InterruptedException e) {
                            throwables.add(e);
                            return;
                        }

                        BinaryPage elem = bc.getPageFromBlockingCache(page.getResourcePath());
                        // this thread will have been blocked for WAIT_BEFORE_PUT by the the
                        // first thread which acquired the lock and only freed it after the put
                        try {
                             assertNotNull(elem);
                        } catch (AssertionError e) {
                            throwables.add(e);
                        }
                    }
                }
            });
        }

        long start = System.currentTimeMillis();
        for (final Thread getter : getters) {
            Thread.sleep(10);
            getter.start();
        }

        for (final Thread getter : getters) {
            getter.join();
        }
        
        assertTrue((System.currentTimeMillis() - start) > WAIT_BEFORE_PUT);
        
        assertTrue(throwables.isEmpty());
    
    }

    @Test
    public void testGetNullIsLockedUntilTimeout() {

        Ehcache ehBinariesCache = cacheManager.getCache("binariesCache");
        BlockingCache cache = new BlockingCache(ehBinariesCache);
        // lock is held for 100 ms if item for get returns null
        final int TIME_OUT_MILLIS = 100;
        cache.setTimeoutMillis(TIME_OUT_MILLIS);
        hstBinariesCache = new HstCacheEhCacheImpl(cache);
        bc = new BinariesCache(hstBinariesCache);

        // NO put so page.getResourcePath() is not available in cache
        Thread[] getters = new Thread[2];
        final AtomicInteger counter = new AtomicInteger(0);
        final List<Exception> exceptions = Collections.synchronizedList(new LinkedList<Exception>());
        final long start = System.currentTimeMillis();
        for (int i = 0; i < getters.length; i++) {
            getters[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        BinaryPage elem = bc.getPageFromBlockingCache(page.getResourcePath());
                        int count = counter.incrementAndGet();
                        assertNull(elem);
                        if (count == 2) {
                            // THE second getter will be blocked by the FIRST one for at least cache.setTimeoutMillis(TIME_OUT_MILLIS);
                            if((System.currentTimeMillis() - start) <= TIME_OUT_MILLIS) {
                                throw new IllegalStateException("Not possible that this thread finished before the lock timeout of TIME_OUT_MILLIS");
                            }
                        }
                    } catch (RuntimeException e) {
                        exceptions.add(e);
                    }
                }
            });
        }

        for (int i = 0; i < getters.length; i++) {
            getters[i].start();
        }

        try {
            for (int i = 0; i < getters.length; i++) {
                getters[i].join();
            }
        } catch (InterruptedException e) {
        }

        if (!exceptions.isEmpty()) {
            StringBuilder exInfo = new StringBuilder();
            for (Exception ex : exceptions) {
                exInfo.append("    " + ex.toString() + " " + ex.getMessage() + "\n");
            }
            fail("Failed :\n" + exInfo + "\n");
        }
    }
}

