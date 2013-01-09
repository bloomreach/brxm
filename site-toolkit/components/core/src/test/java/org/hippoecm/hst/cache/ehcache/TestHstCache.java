/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cache.ehcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;

public class TestHstCache extends AbstractSpringTestCase {

    public void setUp() throws Exception {
        super.setUp();
    }
    
    @Test
    public void testNoopCache() throws Exception {
        HstCache noopCache = getComponent("noopCache");
        assertNotNull(noopCache);
        CacheElement element = noopCache.createElement("key1", "content1");
        assertNotNull(element);
        noopCache.put(element);
        assertFalse(noopCache.isKeyInCache("key1"));
        assertNull(noopCache.get("key1"));
    }
    
    @Test
    public void testSimpleCache() throws Exception {
        HstCache simpleCache = getComponent("simpleCache");
        assertNotNull(simpleCache);
        CacheElement element = simpleCache.createElement("key1", "content1");
        assertNotNull(element);
        simpleCache.put(element);
        assertTrue(simpleCache.isKeyInCache("key1"));
        element = simpleCache.get("key1");
        assertNotNull(element);
        assertEquals("content1", element.getContent());
    }

    // TODO: Improve binariesCache testing. For now, keep this to check ehcache library compatibility.
    @Test
    public void testBinariesCache() throws Exception {
        HstCache defaultBinariesCache = getComponent("defaultBinariesCache");
        assertNotNull(defaultBinariesCache);
        CacheElement element = defaultBinariesCache.createElement("key1", "content1");
        assertNotNull(element);
        defaultBinariesCache.put(element);
        assertTrue(defaultBinariesCache.isKeyInCache("key1"));
        element = defaultBinariesCache.get("key1");
        assertNotNull(element);
        assertEquals("content1", element.getContent());
    }
}
