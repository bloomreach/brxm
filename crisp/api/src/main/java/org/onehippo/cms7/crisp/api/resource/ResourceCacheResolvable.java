package org.onehippo.cms7.crisp.api.resource;

public interface ResourceCacheResolvable {

    boolean isCacheable(ResourceContainable resourceContainer);

    Object toCacheData(ResourceContainable resourceContainer);

    ResourceContainable fromCacheData(Object cacheData);

}
