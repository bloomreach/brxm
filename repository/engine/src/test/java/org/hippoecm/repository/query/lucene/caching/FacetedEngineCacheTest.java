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
package org.hippoecm.repository.query.lucene.caching;

import static org.junit.Assert.assertTrue;

import java.util.BitSet;
import java.util.HashMap;

import org.apache.lucene.util.DocIdBitSet;
import org.hippoecm.repository.FacetedNavigationEngine.Count;
import org.junit.Test;

public class FacetedEngineCacheTest {

    @Test
    public void testCacheMaximumSize() {
        FacetedEngineCache facetedEngineCache = new FacetedEngineCache(250, 500);
        for(int i = 0 ; i < 11000; i++) {
            facetedEngineCache.putDocIdSet(String.valueOf(i), new DocIdBitSet(new BitSet()));
            Object[] objs = new Object[1];
            objs[0] = new Object();
            facetedEngineCache.putFacetValueCountMap(new FacetedEngineCache.FECacheKey(objs), new HashMap<String, Count>());
        }
        assertTrue("docIdSetCache size should be 250 ", facetedEngineCache.getDocIdSetCacheSize() == 250);
        assertTrue("facetValueCountMapCache size should be 500 ", facetedEngineCache.getFacetValueCountMapCacheSize() == 500);
    }
    
    @Test
    public void testCacheMinimumSize() {
        // the cache size is at a minimum of 100, even if configured lower
        FacetedEngineCache facetedEngineCache = new FacetedEngineCache(90,90);for(int i = 0 ; i < 11000; i++) {
            facetedEngineCache.putDocIdSet(String.valueOf(i), new DocIdBitSet(new BitSet()));
            Object[] objs = new Object[1];
            objs[0] = new Object();
            facetedEngineCache.putFacetValueCountMap(new FacetedEngineCache.FECacheKey(objs), new HashMap<String, Count>());
        }
        assertTrue("docIdSetCache size should be 100 because 100 is minumum ", facetedEngineCache.getDocIdSetCacheSize() == 100);
        assertTrue("facetValueCountMapCache size should be 100 because 100 is minumum ", facetedEngineCache.getFacetValueCountMapCacheSize() == 100);
    }
    
}
