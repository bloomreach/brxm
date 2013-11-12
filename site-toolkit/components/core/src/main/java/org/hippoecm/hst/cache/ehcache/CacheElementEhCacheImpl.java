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

import org.hippoecm.hst.cache.CacheElement;

import net.sf.ehcache.Element;

/**
 * CacheElementEhCacheImpl
 * @version $Id$
 */
class CacheElementEhCacheImpl implements CacheElement {
    
    final Element element;
    private final boolean cacheable;
    
    CacheElementEhCacheImpl(final Element element) {
        this.element = element;
        cacheable = true;
    }
    
    CacheElementEhCacheImpl(final Object key, final Object value) {
        this.element = new Element(key, value);
        cacheable = true;
    }

    public CacheElementEhCacheImpl(final Object key, final Object value, final boolean cacheable) {
        this.element = new Element(key, value);
        this.cacheable = cacheable;
    }

    public Object getKey() {
        return element.getObjectKey();
    }

    public Object getContent() {
        return element.getObjectValue();
    }

    public int getTimeToIdleSeconds() {
        return element.getTimeToIdle();
    }

    public int getTimeToLiveSeconds() {
        return element.getTimeToLive();
    }

    public boolean isEternal() {
        return element.isEternal();
    }

    public void setEternal(boolean eternal) {
        element.setEternal(eternal);
    }

    public void setTimeToIdleSeconds(int timeToIdle) {
        element.setTimeToIdle(timeToIdle);
    }

    public void setTimeToLiveSeconds(int timeToLive) {
        element.setTimeToLive(timeToLive);
    }

    @Override
    public boolean isCacheable() {
        return cacheable;
    }

}
