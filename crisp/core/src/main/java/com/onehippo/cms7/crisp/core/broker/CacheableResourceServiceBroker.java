/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.broker;

import java.util.Map;

import com.onehippo.cms7.crisp.api.broker.AbstractResourceServiceBroker;
import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.api.resource.ResourceContainer;
import com.onehippo.cms7.crisp.api.resource.ResourceDataCache;
import com.onehippo.cms7.crisp.api.resource.ResourceException;
import com.onehippo.cms7.crisp.api.resource.ResourceResolver;
import com.onehippo.cms7.crisp.api.resource.ValueMap;
import com.onehippo.cms7.crisp.core.resource.DefaultValueMap;

public class CacheableResourceServiceBroker extends AbstractResourceServiceBroker {

    private static final String OPERATION_KEY = "operationKey";
    private static final String RESOURCE_SPACE = "resourceSpace";
    private static final String RESOURCE_PATH = "resourcePath";
    private static final String VARIABLES = "variables";

    private static final String OPERATION_KEY_RESOLVE = CacheableResourceServiceBroker.class.getName() + ".resolve";
    private static final String OPERATION_KEY_FIND_RESOURCES = CacheableResourceServiceBroker.class.getName()
            + ".findResources";

    private ResourceDataCache defaultResourceDataCache;
    private boolean cacheEnabled = true;

    public CacheableResourceServiceBroker() {
        super();
    }

    public ResourceDataCache getDefaultResourceDataCache() {
        return defaultResourceDataCache;
    }

    public void setDefaultResourceDataCache(ResourceDataCache defaultResourceDataCache) {
        this.defaultResourceDataCache = defaultResourceDataCache;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    @Override
    public Resource resolve(String resourceSpace, String absResourcePath, Map<String, Object> pathVariables)
            throws ResourceException {
        Resource resource = null;
        ResourceResolver resourceResolver = getResourceResolver(resourceSpace);
        ValueMap cacheKey = null;
        ResourceDataCache resourceDataCache = getResourceDataCache(resourceSpace);

        if (isCacheEnabled() && resourceDataCache != null) {
            cacheKey = createCacheKey(OPERATION_KEY_RESOLVE, resourceSpace, absResourcePath, pathVariables);
            Object cacheData = resourceDataCache.getData(cacheKey);

            if (cacheData != null) {
                resource = (Resource) resourceResolver.fromCacheData(cacheData);
            }
        }

        if (resource == null) {
            resource = resourceResolver.resolve(absResourcePath, pathVariables);

            if (resource != null && cacheKey != null && resourceResolver.isCacheable(resource)) {
                final Object cacheData = resourceResolver.toCacheData(resource);

                if (cacheData != null) {
                    resourceDataCache.putData(cacheKey, cacheData);
                }
            }
        }

        return resource;
    }

    @Override
    public ResourceContainer findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables)
            throws ResourceException {
        ResourceContainer resource = null;
        ResourceResolver resourceResolver = getResourceResolver(resourceSpace);
        ValueMap cacheKey = null;
        ResourceDataCache resourceDataCache = getResourceDataCache(resourceSpace);

        if (isCacheEnabled() && resourceDataCache != null) {
            cacheKey = createCacheKey(OPERATION_KEY_FIND_RESOURCES, resourceSpace, baseAbsPath, pathVariables);
            Object cacheData = resourceDataCache.getData(cacheKey);

            if (cacheData != null) {
                resource = (Resource) resourceResolver.fromCacheData(cacheData);
            }
        }

        if (resource == null) {
            resource = resourceResolver.findResources(baseAbsPath, pathVariables);

            if (resource != null && cacheKey != null && resourceResolver.isCacheable(resource)) {
                final Object cacheData = resourceResolver.toCacheData(resource);

                if (cacheData != null) {
                    resourceDataCache.putData(cacheKey, cacheData);
                }
            }
        }

        return resource;
    }

    @Override
    public ResourceDataCache getResourceDataCache(String resourceSpace) {
        ResourceDataCache resourceDataCache = null;

        ResourceResolver resourceResolver = getResourceResolver(resourceSpace);

        if (resourceResolver != null) {
            resourceDataCache = resourceResolver.getResourceDataCache();
        }

        if (resourceDataCache == null) {
            resourceDataCache = getDefaultResourceDataCache();
        }

        return resourceDataCache;
    }

    protected ValueMap createCacheKey(final String operationKey, final String resourceSpace, final String resourcePath,
            final Map<String, Object> variables) {
        final ValueMap cacheKey = new DefaultValueMap();

        if (operationKey != null) {
            cacheKey.put(OPERATION_KEY, operationKey);
        }

        if (resourceSpace != null) {
            cacheKey.put(RESOURCE_SPACE, resourceSpace);
        }

        if (resourcePath != null) {
            cacheKey.put(RESOURCE_PATH, resourcePath);
        }

        if (variables != null && !variables.isEmpty()) {
            cacheKey.put(VARIABLES, variables);
        }

        return cacheKey;
    }
}
