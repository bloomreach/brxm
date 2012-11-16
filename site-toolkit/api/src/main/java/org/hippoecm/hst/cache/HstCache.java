/*
 *  Copyright 2008 - 2012 Hippo.
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
 * HST Cache Interface for cache related activities.
 * Abstraction around physical cache implementation.
 * @version $Id$
 */
public interface HstCache {

    CacheElement createElement(Object key, Object content);

    /**
     * @return a {@link CacheElement} that is marked to be uncachable
     */
    CacheElement createUncachableElement(Object key, Object content);
    
    void put(CacheElement object);

    /**
     * Returns the {@link CacheElement} for <code>key</code> and <code>null</code> otherwise. Please
     * be aware that if the underlying cache such as ehcache BlockingCache sets a lock using the <code>key</code>,
     * then typically you need to free this lock your self.
     * @param key
     * @return the {@link CacheElement} for <code>key</code> and <code>null</code> otherwise
     */
    CacheElement get(Object key);

    /**
     * 
     * @param key the  <code>key</code> to get from the cache or to put an object for in the cache when the cache does
     *            not yet contain <code>key</code>
     * @param valueLoader will be used to load the value for <code>key</code> if cache does not yet contain an 
     *                    object for <code>key</code>. The loaded value is put in the cache. 
     *                    The <code>valueLoader</code> is not allowed to return <code>null</code> but is allowed to 
     *                    return a {@link CacheElement} with <code>content</code> null.
     *                    
     * @return the element from the cache, or the just retrieved and stored element through <code>valueloader</code>
     */
    CacheElement get(Object key, Callable<? extends CacheElement> valueLoader) throws Exception;

    boolean isKeyInCache(Object key);
    
    boolean remove(Object key);
    
    void clear();
    
    int getTimeToIdleSeconds();
    
    int getTimeToLiveSeconds();
    
    int getSize();
    
    int getMaxSize();
    
}
