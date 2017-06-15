/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.cms7.crisp.core.resource;

import org.onehippo.cms7.crisp.api.resource.ResourceDataCache;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;

/**
 * {@link ResourceDataCache} implementation using Spring Framework's {@link Cache} instance.
 */
public class SpringResourceDataCache implements ResourceDataCache {

    /**
     * The delegate cache representation of Spring Framework's {@link Cache}.
     */
    private final Cache cache;

    /**
     * Constructs with a Spring Framework's {@link Cache} instance.
     * @param cache a Spring Framework's {@link Cache} instance
     */
    public SpringResourceDataCache(final Cache cache) {
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getData(Object key) {
        Object data = null;

        ValueWrapper dataWrapper = cache.get(key);

        if (dataWrapper != null) {
            data = dataWrapper.get();
        }

        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putData(Object key, Object data) {
        cache.put(key, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object putDataIfAbsent(Object key, Object data) {
        Object existingData = null;

        ValueWrapper dataWrapper = cache.putIfAbsent(key, data);

        if (dataWrapper != null) {
            existingData = dataWrapper.get();
        }

        return existingData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void evictData(Object key) {
        cache.evict(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cache.clear();
    }

}
