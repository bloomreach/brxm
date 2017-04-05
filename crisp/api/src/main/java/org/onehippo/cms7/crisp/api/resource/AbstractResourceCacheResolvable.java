package org.onehippo.cms7.crisp.api.resource;

public abstract class AbstractResourceCacheResolvable implements ResourceCacheResolvable {

    @Override
    public boolean isCacheable(ResourceContainable resourceContainer) {
        return false;
    }

    @Override
    public Object toCacheData(ResourceContainable resourceContainer) {
        return null;
    }

    @Override
    public ResourceContainable fromCacheData(Object cacheData) {
        return null;
    }
}
