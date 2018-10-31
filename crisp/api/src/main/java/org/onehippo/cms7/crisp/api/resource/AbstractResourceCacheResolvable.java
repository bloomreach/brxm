/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.crisp.api.resource;

import java.io.IOException;
import java.util.Map;

import org.onehippo.cms7.crisp.api.broker.AbstractResourceServiceBroker;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;

/**
 * Abstract {@link ResourceCacheResolvable} base class.
 */
public abstract class AbstractResourceCacheResolvable implements ResourceCacheResolvable {

    /**
     * Flag whether or not this is enabled with caching.
     */
    private boolean cacheEnabled;

    /**
     * {@link ResourceDataCache} representing an underlying cache store for {@link Resource} representations.
     */
    private ResourceDataCache resourceDataCache;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * Sets flag whether or not this is enabled with caching.
     * @param cacheEnabled flag whether or not this is enabled with caching
     */
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCacheable(Resource resource) {
        return false;
    }

    @Override
    public ValueMap createCacheKey(final String resourceSpace, final String operationKey, final String resourcePath,
            final Map<String, Object> pathVariables, final ExchangeHint exchangeHint) {
        final ValueMap cacheKey = new DefaultValueMap();

        if (operationKey != null) {
            cacheKey.put(AbstractResourceServiceBroker.OPERATION_KEY, operationKey);
        }

        if (resourceSpace != null) {
            cacheKey.put(AbstractResourceServiceBroker.RESOURCE_SPACE, resourceSpace);
        }

        if (resourcePath != null) {
            cacheKey.put(AbstractResourceServiceBroker.RESOURCE_PATH, resourcePath);
        }

        if (pathVariables != null && !pathVariables.isEmpty()) {
            cacheKey.put(AbstractResourceServiceBroker.PATH_VARIABLES, pathVariables);
        }

        if (exchangeHint != null) {
            cacheKey.put(AbstractResourceServiceBroker.EXCHANGE_HINT, exchangeHint.getCacheKey());
        }

        return cacheKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object toCacheData(Resource resource) throws IOException {
        if (!isCacheEnabled()) {
            return null;
        }

        return resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource fromCacheData(Object cacheData) throws IOException {
        if (!isCacheEnabled()) {
            return null;
        }

        return (Resource) cacheData;
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
