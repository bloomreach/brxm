/*
 *  Copyright 2008 Hippo.
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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;

/**
 * HstCacheEhCacheImpl
 * @version $Id$
 */
public class HstCacheEhCacheImpl implements HstCache {

    private Ehcache ehcache;

    public HstCacheEhCacheImpl(Ehcache ehcache) {
        this.ehcache = ehcache;
    }

    public CacheElement get(Object key) {
        Element element = ehcache.get(key);

        if (element == null)
            return null;

        return new CacheElementEhCacheImpl(element);
    }

    public int getTimeToIdleSeconds() {
        return (int) ehcache.getCacheConfiguration().getTimeToIdleSeconds();
    }

    public int getTimeToLiveSeconds() {
        return (int) ehcache.getCacheConfiguration().getTimeToLiveSeconds();
    }

    public boolean isKeyInCache(Object key) {
        return ehcache.isKeyInCache(key);
    }

    public void put(CacheElement element) {
        CacheElementEhCacheImpl cacheElem = (CacheElementEhCacheImpl) element;
        ehcache.put(cacheElem.element);
    }

    public CacheElement createElement(Object key, Object content) {
        return new CacheElementEhCacheImpl(key, content);
    }

    public boolean remove(Object key) {
        if (ehcache.isKeyInCache(key)) {
            return ehcache.remove(key);
        }

        return false;
    }

    public void clear() {
        ehcache.removeAll();
    }

    public int getSize() {
        return ehcache.getSize();
    }

    public int getMaxSize() {
        return ehcache.getCacheConfiguration().getMaxElementsInMemory()
                + ehcache.getCacheConfiguration().getMaxElementsOnDisk();
    }

}
