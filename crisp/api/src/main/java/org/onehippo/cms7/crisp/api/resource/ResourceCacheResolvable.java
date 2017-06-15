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

import java.io.IOException;

import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;

/**
 * An abstraction responsible for the following:
 * <UL>
 * <LI>to return a {@link ResourceDataCache} as a cache store of resource representations,</LI>
 * <LI>to be able to check whether or not a specific {@link Resource} representation is cacheable,</LI>
 * <LI>to convert a {@link Resource} representation to a cacheable data object to be stored in {@link ResourceDataCache},
 * <LI>and to convert a cached data object back to a {@link Resource} representation.
 * </UL>
 * <P>
 * Note that a {@link Resource} object cannot necessarily be cached into {@link ResourceDataCache} directly in
 * every case. For example, if the underlying {@link ResourceDataCache} depends on object serialization and if
 * the given {@link Resource} instance is not practically serializable to be restored back to the original state,
 * then it could be meaningless to try to cache the {@link Resource} instance directly into the cache store.
 * Therefore, an implementation may choose to implement methods in this interface to convert a {@link Resource}
 * representation to a practically cacheable data object and convert the cached data object back to the {@link Resource}
 * representation.
 * </P>
 * <P>
 * For example, a JSON data based {@link ResourceCacheResolvable} implementation may have non-serializable JSON
 * objects when resolving data from the backend. In that case, the implementation may choose to convert the JSON
 * object to a string value to be cached. And, when restoring the {@link Resource} representation from a cached
 * data (in this case, a string for the JSON object), the implementation may create a JSON object back to restore
 * its original state. Of course, if the {@link Resource} implementation is practically cacheable, then those conversion
 * methods may return the {@link Resource} object directly without having to implement any conversion logic.
 * The same pattern can apply to other use cases. e.g, non-Serializable record object, etc.
 * </P>
 */
public interface ResourceCacheResolvable {

    /**
     * Returns true if caching is enabled with this.
     * @return true if caching is enabled with this
     */
    boolean isCacheEnabled();

    /**
     * Returns true if the given {@code resource} is cacheable.
     * <p>
     * Note that if an implementation does not want to cache {@link Resource} representations at all for any reason,
     * then it may always return false. {@link ResourceServiceBroker} implementation should not try to cache
     * any {@link Resource} representations into cache store if this method returns false.
     * </p>
     * @param resource resource representation
     * @return true if the given {@code resource} is cacheable
     */
    boolean isCacheable(Resource resource);

    /**
     * Convert the given {@code resource} to a cacheable data object to be stored in {@link ResourceDataCache}.
     * <P>
     * Implementations may simply return the {@code resource} object directly without any conversion if the the
     * {@code resource} object can be stored (for example, the {@code resource} object is serializable) into the
     * underlying cache store.
     * Otherwise, the {@code resource} object can be converted to something else and returned in order to be stored
     * into the underlying cache store by implementations.
     * </P>
     * @param resource resource representation
     * @return converted the given {@code resource} to a cacheable data object to be stored in {@link ResourceDataCache}
     * @throws IOException if IO error occurs
     */
    Object toCacheData(Resource resource) throws IOException;

    /**
     * Convert and restore back the given {@code cacheData} from the {@link ResourceDataCache} to a {@link Resource}
     * object.
     * <P>
     * Implementations may simply cast the {@code cacheData} object directly to {@link Resource} object without
     * any conversion if the the resource object was stored directly into the underlying cache store.
     * Otherwise, the {@code cacheData} object can be converted back to a {@link Resource} object by implementations.
     * </P>
     * @param cacheData cached data object that is stored in {@link ResourceDataCache}
     * @return {@link Resource} object converted from the {@code cacheData}
     * @throws IOException if IO error occurs
     */
    Resource fromCacheData(Object cacheData) throws IOException;

    /**
     * Returns a {@link ResourceDataCache} that represents the underlying cache store.
     * <p>
     * Note that an implementation may return null if it doesn't want to have its own cache store representation.
     * In that case, a {@link ResourceServiceBroker} implementation may use a default {@link ResourceDataCache}
     * instance as a fallback cache store.
     * </p>
     * @return a {@link ResourceDataCache} that represents the underlying cache store
     */
    ResourceDataCache getResourceDataCache();

}
