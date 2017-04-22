/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

import com.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;

/**
 * An abstraction responsible for the following:
 * <P>
 * <UL>
 * <LI>to return a {@link ResourceDataCache} as a cache store of resource representations,</LI>
 * <LI>to be able to check whether or not a specific {@link Resource} representation is cacheable,</LI>
 * <LI>to convert a {@link Resource} representation to a cacheable data object to be stored in {@link ResourceDataCache},
 * <LI>and to convert a cached data object back to a {@link Resource} representation.
 * </UL>
 * </P>
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
     * @param resource
     * @return
     */
    Object toCacheData(Resource resource);

    /**
     * Convert and restore back the given {@code cacheData} from the {@link ResourceDataCache} to a {@link Resource}
     * object.
     * @param cacheData cached data object that is stored in {@link ResourceDataCache}
     * @return {@link Resource} object converted from the {@code cacheData}
     */
    Resource fromCacheData(Object cacheData);

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
