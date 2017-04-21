/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

public abstract class AbstractResourceCacheResolvable implements ResourceCacheResolvable {

    private ResourceDataCache resourceDataCache;

    @Override
    public boolean isCacheable(Resource resource) {
        return false;
    }

    @Override
    public Object toCacheData(Resource resource) {
        return null;
    }

    @Override
    public Resource fromCacheData(Object cacheData) {
        return null;
    }

    @Override
    public ResourceDataCache getResourceDataCache() {
        return resourceDataCache;
    }

    public void setResourceDataCache(ResourceDataCache resourceDataCache) {
        this.resourceDataCache = resourceDataCache;
    }
}
