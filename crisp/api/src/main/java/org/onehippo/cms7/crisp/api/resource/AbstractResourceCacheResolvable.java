package org.onehippo.cms7.crisp.api.resource;

public abstract class AbstractResourceCacheResolvable implements ResourceCacheResolvable {

    @Override
    public boolean isCacheable(ResourceContainer resourceContainer) {
        return false;
    }

    @Override
    public Object toCacheData(ResourceContainer resourceContainer) {
        return null;
    }

    @Override
    public ResourceContainer fromCacheData(Object cacheData) {
        return null;
    }
}
