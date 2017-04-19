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

        ValueWrapper cacheDataWrapper = cache.get(key);

        if (cacheDataWrapper != null) {
            data = cacheDataWrapper.get();
        }

        return data;
    }

    @Override
    public void putData(Object key, Object data) {
        cache.put(key, data);
    }

}
