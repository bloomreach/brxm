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

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;

import org.hippoecm.hst.servlet.utils.BinariesCache;
import org.hippoecm.hst.servlet.utils.BinaryPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BinariesCacheTest {

    BinaryPage page;
    BinariesCache bc;
    
    @Before
    public void setup() throws UnsupportedEncodingException {
        page = new BinaryPage("/content/binaries/my:pdffile");
        bc = new BinariesCache();
        bc.init();
    }
    
    @After
    public void tearDown() {
        bc.destroy();
    }

    /**
     * Repeatedly setup and destroy cache.
     */
    @Test
    public void testSetupDestroyCache() {
        BinariesCache bc1 = new BinariesCache("test");
        bc1.init();
        bc1.destroy();
        bc1.init();
        bc1.destroy();
        bc1.init();
        bc1.destroy();
    }

    /**
     * Make sure caches are independent.
     */
    @Test
    public void testSetupDestroyTwoCaches() {
        BinariesCache bc1 = new BinariesCache("one");
        BinariesCache bc2 = new BinariesCache("two");
        bc1.init();
        bc2.init();
        bc2.destroy();
        bc1.destroy();

        bc1.init();
        bc2.init();
        bc1.destroy();
        bc2.destroy();

        bc1.init();
        bc1.destroy();
        bc2.init();
        bc2.destroy();
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
        page.setExpirationTime(System.currentTimeMillis() - 5000L);
        assertTrue(bc.hasPageExpired(page));
    }

    @Test
    public void testNotExpired() {
        page.setExpirationTime(System.currentTimeMillis() + 5000L);
        assertFalse(bc.hasPageExpired(page));
    }
    
    @Test
    public void testUpdateExpired() {
        page.setExpirationTime(System.currentTimeMillis() - 5000L);
        assertTrue(bc.hasPageExpired(page));
        bc.updateExpirationTime(page);
        assertFalse(bc.hasPageExpired(page));
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
