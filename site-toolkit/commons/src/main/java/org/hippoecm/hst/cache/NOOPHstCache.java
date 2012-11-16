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
package org.hippoecm.hst.cache;

import java.util.concurrent.Callable;

/**
 * NOOPHstCache
 * @version $Id$
 */
public class NOOPHstCache implements HstCache {
    
    public CacheElement createElement(Object key, Object content) {
        return new NOOPCacheElement(key, content, true);
    }

    @Override
    public CacheElement createUncachableElement(final Object key, final Object content) {
        return new NOOPCacheElement(key, content, false);
    }

    public void put(CacheElement object) {
    }

    public boolean isKeyInCache(Object key) {
        return false;
    }

    public CacheElement get(Object key) {
        return null;
    }

    @Override
    public CacheElement get(final Object key, final Callable<? extends CacheElement> valueLoader) {
        return get(key);
    }

    public boolean remove(Object key) {
        return true;
    }

    public void clear() {
    }

    public int getMaxSize() {
        return 0;
    }

    public int getSize() {
        return 0;
    }

    public int getTimeToIdleSeconds() {
        return 0;
    }

    public int getTimeToLiveSeconds() {
        return 0;
    }
    
    public static class NOOPCacheElement implements CacheElement {
        
        private Object key;
        private Object content;
        private boolean cachable;
        
        private NOOPCacheElement(Object key, Object content, boolean cachable) {
            this.key = key;
            this.content = content;
            this.cachable = cachable;
        }
        
        public Object getContent() {
            return content;
        }

        public Object getKey() {
            return key;
        }

        public int getTimeToIdleSeconds() {
            return 0;
        }

        public int getTimeToLiveSeconds() {
            return 0;
        }

        public boolean isEternal() {
            return false;
        }

        public void setEternal(boolean eternal) {
        }

        public void setTimeToIdleSeconds(int timeToIdle) {
        }

        public void setTimeToLiveSeconds(int timeToLive) {
        }

        @Override
        public boolean isCachable() {
            return cachable;
        }
    }
}
