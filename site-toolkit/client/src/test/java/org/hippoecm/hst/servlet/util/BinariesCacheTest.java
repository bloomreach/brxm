/*
 *  Copyright 2010 Hippo.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.cache.ehcache.HstCacheEhCacheImpl;
import org.hippoecm.hst.servlet.utils.BinariesCache;
import org.hippoecm.hst.servlet.utils.BinaryPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        hstBinariesCache = new HstCacheEhCacheImpl(ehBinariesCache);
        bc = new BinariesCache(hstBinariesCache);
    }
    
    @After
    public void tearDown() {
        cacheManager.shutdown();
    }

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

}
