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
