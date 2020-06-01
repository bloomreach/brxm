/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.api.resource;

/**
 * Abstraction that represents the underlying cache store.
 */
public interface ResourceDataCache {

    /**
     * Finds a cached data object for a {@link Resource} representation from the underlying cache store
     * by the given {@code key}.
     * @param key cache key
     * @return a cached data object for a {@link Resource} representation from the underlying cache store by the
     *         given {@code key}
     */
    public Object getData(Object key);

    /**
     * Associates the specified cacheable {@code data} object for a {@link Resource} representation with the specified
     * {@code key} in the underlying cache store.
     * <p>If the cache previously contained a mapping for this key, the old cache data object is replaced by the
     * specified cacheable {@code data} object.</p>
     * @param key cache key
     * @param data cacheable {@code data} object for a {@link Resource} representation
     */
    public void putData(Object key, Object data);

    /**
     * Associates the specified cacheable {@code data} object for a {@link Resource} representation with the specified
     * {@code key} in the underlying cache store if it is not set already.
     * <p>If the cache previously contained a mapping for this key, the existing cache data object is not going
     * to be replaced by the specified cacheable {@code data} object, but the existing cache data object will be
     * simply returned without replacement. If there was no mapping for this key, then it should return <code>null</code>
     * instead.
     * @param key cache key
     * @param data cacheable {@code data} object for a {@link Resource} representation
     * @return If the cache previously contained a mapping for this key, the existing cache data object is not
     *         going to be replaced by the specified cacheable {@code data} object, but the existing cache data
     *         object will be simply returned without replacement. If there was no mapping for this key, then it
     *         should return <code>null</code> instead.
     */
    public Object putDataIfAbsent(Object key, Object data);

    /**
     * Evict the mapping for this {@code key} from this cache if it is present.
     * @param key cache key
     */
    public void evictData(Object key);

    /**
     * Remove all mappings from the cache.
     */
    public void clear();

}
