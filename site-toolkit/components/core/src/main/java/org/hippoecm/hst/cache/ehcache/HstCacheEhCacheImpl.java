/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.concurrent.Callable;

import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * HstCacheEhCacheImpl
 * @version $Id$
 */
public class HstCacheEhCacheImpl implements HstCache {

    private static final Logger log = LoggerFactory.getLogger(HstCacheEhCacheImpl.class);
    private Ehcache ehcache;
    private volatile int invalidationCounter;

    public HstCacheEhCacheImpl(Ehcache ehcache) {
        this.ehcache = ehcache;
    }

    public void setStatisticsEnabled(boolean statisticsEnabled) {
        ehcache.setStatisticsEnabled(statisticsEnabled);
    }
    
    public CacheElement get(Object key) {
        Element element = ehcache.get(key);
        if (element == null) {
            return null;
        }
        return new CacheElementEhCacheImpl(element);
    }

    @Override
    public CacheElement get(final Object key, final Callable<? extends CacheElement> valueLoader) throws Exception {
        if (valueLoader == null) {
            throw new IllegalArgumentException("valueLoader is not allowed to be null");
        }
        CacheElement cached = get(key);
        if (cached != null) {
            return cached;
        }

        int preCallInvalidationCounter = invalidationCounter;
        // to make sure the lock is freed in case of blocking cache, make sure we start with an non null element,
        CacheElement element = null;
        try {
            element = valueLoader.call();
            if (element == null) {
                log.debug("valueLoader '{}#call()' did " +
                        "return null for key '{}'",valueLoader.getClass().getName(), key);
            }
        } finally {
            int postCallInvalidationCounter = invalidationCounter;
            // if exceptions (also unchecked) happened or the CacheElement is uncacheable, we put an empty element (createElement(key,null))
            // to make sure that if a blocking cache is used, the lock on the key is freed. Also see ehcache BlockingCache
            if (element == null) {
                element = createElement(key, null);
                put(element);
            } else if (!element.isCacheable() || postCallInvalidationCounter != preCallInvalidationCounter){
                put(createElement(key, null));
            } else {
                put(element);
            }
        }
        
        return element;
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

    public CacheElement createUncacheableElement(Object key, Object content) {
        return new CacheElementEhCacheImpl(key, content, false);
    }

    public boolean remove(Object key) {
        return ehcache.remove(key);
    }

    public void clear() {
        invalidationCounter++;
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
