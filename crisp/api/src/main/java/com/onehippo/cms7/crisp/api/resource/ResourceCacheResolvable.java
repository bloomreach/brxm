package com.onehippo.cms7.crisp.api.resource;

public interface ResourceCacheResolvable {

    boolean isCacheable(ResourceContainer resourceContainer);

    Object toCacheData(ResourceContainer resourceContainer);

    ResourceContainer fromCacheData(Object cacheData);

    ResourceDataCache getResourceDataCache();

}
