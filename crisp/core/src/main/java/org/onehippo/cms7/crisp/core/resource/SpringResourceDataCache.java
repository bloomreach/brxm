package org.onehippo.cms7.crisp.core.resource;

import org.onehippo.cms7.crisp.api.resource.ResourceDataCache;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;

public class SpringResourceDataCache implements ResourceDataCache {

    private final Cache cache;

    public SpringResourceDataCache(final Cache cache) {
        this.cache = cache;
    }

    @Override
    public Object getData(Object key) {
        Object data = null;

        ValueWrapper dataWrapper = cache.get(key);

        if (dataWrapper != null) {
            data = dataWrapper.get();
        }

        return data;
    }

    @Override
    public void putData(Object key, Object data) {
        cache.put(key, data);
    }

    @Override
    public Object putDataIfAbsent(Object key, Object data) {
        Object existingData = null;

        ValueWrapper dataWrapper = cache.putIfAbsent(key, data);

        if (dataWrapper != null) {
            existingData = dataWrapper.get();
        }

        return existingData;
    }

    @Override
    public void evictData(Object key) {
        cache.evict(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

}
