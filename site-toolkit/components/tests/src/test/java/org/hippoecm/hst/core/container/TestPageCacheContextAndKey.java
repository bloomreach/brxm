/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.io.Serializable;
import java.util.LinkedHashMap;

import org.hippoecm.hst.site.request.HstRequestContextImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestPageCacheContextAndKey {

    @Test
    public void testPageCacheContext() {
        PageCacheContext pcc = createEmptyValveContext().getPageCacheContext();
        assertNotNull(pcc);
        assertTrue(pcc.isCacheable());
        pcc.markUncacheable();
        assertFalse(pcc.isCacheable());
        pcc.markUncacheable("because I said so");
        assertEquals("because I said so", pcc.getReasonsUncacheable().get(0));
        assertTrue(pcc.getReasonsUncacheable().size() == 1);
        assertNotNull(pcc.getPageCacheKey());
    }

    @Test
    public void testPageCacheKeyEqualsAndHashCode() {
        final PageCacheKey pageCacheKey1 = createEmptyValveContext().getPageCacheContext().getPageCacheKey();
        final PageCacheKey pageCacheKey2 = createEmptyValveContext().getPageCacheContext().getPageCacheKey();
        final PageCacheKey pageCacheKey3 = createEmptyValveContext().getPageCacheContext().getPageCacheKey();

        assertTrue(pageCacheKey1.hashCode() == new LinkedHashMap<String, Serializable>().hashCode());
        pageCacheKey1.setAttribute("key1", "val1");
        pageCacheKey1.setAttribute("key2", "val2");

        pageCacheKey2.setAttribute("key1", "val1");
        pageCacheKey2.setAttribute("key2", "val2");

        pageCacheKey3.setAttribute("foo", "bar");

        assertTrue(pageCacheKey1.hashCode() == pageCacheKey2.hashCode());
        assertTrue(pageCacheKey1.equals(pageCacheKey2));

        assertFalse(pageCacheKey1.equals(pageCacheKey3));
    }

    @Test
    public void testPageCacheKeyEqualsWhenSettingSameSubKeyMultipleTimes() {
        final PageCacheKey pageCacheKey1 = createEmptyValveContext().getPageCacheContext().getPageCacheKey();
        final PageCacheKey pageCacheKey2 = createEmptyValveContext().getPageCacheContext().getPageCacheKey();

        assertTrue(pageCacheKey1.hashCode() == new LinkedHashMap<String, Serializable>().hashCode());
        pageCacheKey1.setAttribute("key1", "val1");

        pageCacheKey2.setAttribute("key1", "val1");
        pageCacheKey2.setAttribute("key1", "val1");

        assertTrue(pageCacheKey1.hashCode() == pageCacheKey2.hashCode());
        assertTrue(pageCacheKey1.equals(pageCacheKey2));

        pageCacheKey2.setAttribute("key1", "newval");

        assertFalse(pageCacheKey1.hashCode() == pageCacheKey2.hashCode());
        assertFalse(pageCacheKey1.equals(pageCacheKey2));
    }

    @Test
    public void testPageCacheKeyOrderMatters() {
        final PageCacheKey pageCacheKey1 = createEmptyValveContext().getPageCacheContext().getPageCacheKey();
        final PageCacheKey pageCacheKey2 = createEmptyValveContext().getPageCacheContext().getPageCacheKey();
        assertTrue(pageCacheKey1.hashCode() == new LinkedHashMap<String, Serializable>().hashCode());

        pageCacheKey1.setAttribute("key1", "val1");
        pageCacheKey1.setAttribute("key2", "val2");

        // reverse order of key1, key2
        pageCacheKey2.setAttribute("key2", "val2");
        pageCacheKey2.setAttribute("key1", "val1");

        // since page cache key impl backed by LinkedHashMap we expect
        // 2) pageCacheKey1 is NOT equal to pageCacheKey2 because the ORDER DOES matter

        assertFalse(pageCacheKey1.equals(pageCacheKey2));
    }
    
    private ValveContext createEmptyValveContext() {
        return new HstSitePipeline.Invocation(null,new HstRequestContextImpl(null),null);
    }

}
