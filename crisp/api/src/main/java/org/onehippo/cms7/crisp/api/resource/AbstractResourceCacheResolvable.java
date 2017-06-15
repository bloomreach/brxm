/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.cms7.crisp.api.resource;

import java.io.IOException;

/**
 * Abstract {@link ResourceCacheResolvable} base class.
 */
public abstract class AbstractResourceCacheResolvable implements ResourceCacheResolvable {

    /**
     * {@link ResourceDataCache} representing an underlying cache store for {@link Resource} representations.
     */
    private ResourceDataCache resourceDataCache;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCacheable(Resource resource) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object toCacheData(Resource resource) throws IOException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource fromCacheData(Object cacheData) throws IOException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceDataCache getResourceDataCache() {
        return resourceDataCache;
    }

    /**
     * Sets {@link ResourceDataCache}.
     * @param resourceDataCache {@link ResourceDataCache} representing an underlying cache store for {@link Resource}
     *        representations
     */
    public void setResourceDataCache(ResourceDataCache resourceDataCache) {
        this.resourceDataCache = resourceDataCache;
    }
}
