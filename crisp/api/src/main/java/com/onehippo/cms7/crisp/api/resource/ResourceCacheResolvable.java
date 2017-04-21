/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

public interface ResourceCacheResolvable {

    boolean isCacheable(Resource resource);

    Object toCacheData(Resource resource);

    Resource fromCacheData(Object cacheData);

    ResourceDataCache getResourceDataCache();

}
