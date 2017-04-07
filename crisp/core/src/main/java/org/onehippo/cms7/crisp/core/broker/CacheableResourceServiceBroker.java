package org.onehippo.cms7.crisp.core.broker;

import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceContainer;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.api.resource.ValueMap;
import org.onehippo.cms7.crisp.core.resource.DefaultValueMap;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;

public class CacheableResourceServiceBroker extends AbstractMultiResolverResourceServiceBroker {

    private static final String OPERATION_KEY = "operationKey";
    private static final String RESOURCE_SPACE = "resourceSpace";
    private static final String RESOURCE_PATH = "resourcePath";
    private static final String VARIABLES = "variables";

    private static final String OPERATION_KEY_RESOLVE = CacheableResourceServiceBroker.class.getName() + ".resolve";
    private static final String OPERATION_KEY_FIND_RESOURCES = CacheableResourceServiceBroker.class.getName()
            + ".findResources";

    private Cache resourceCache;
    private boolean cacheEnabled = true;

    public CacheableResourceServiceBroker() {
        super();
    }

    public Cache getResourceCache() {
        return resourceCache;
    }

    public void setResourceCache(Cache resourceCache) {
        this.resourceCache = resourceCache;
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
        ResourceResolver resourceResolver = getResourceResolverByResourceSpace(resourceSpace);
        ValueMap cacheKey = null;

        if (isCacheEnabled() && resourceCache != null) {
            cacheKey = createCacheKey(OPERATION_KEY_RESOLVE, resourceSpace, absResourcePath, pathVariables);
            ValueWrapper cacheDataWrapper = resourceCache.get(cacheKey);

            if (cacheDataWrapper != null) {
                Object cachedData = cacheDataWrapper.get();
                resource = (Resource) resourceResolver.fromCacheData(cachedData);
            }
        }

        if (resource == null) {
            resource = resourceResolver.resolve(absResourcePath, pathVariables);

            if (resource != null && cacheKey != null && resourceResolver.isCacheable(resource)) {
                final Object cacheData = resourceResolver.toCacheData(resource);

                if (cacheData != null) {
                    resourceCache.put(cacheKey, cacheData);
                }
            }
        }

        return resource;
    }

    @Override
    public ResourceContainer findResources(String resourceSpace, String baseAbsPath,
            Map<String, Object> pathVariables) throws ResourceException {
        ResourceContainer resource = null;
        ResourceResolver resourceResolver = getResourceResolverByResourceSpace(resourceSpace);
        ValueMap cacheKey = null;

        if (isCacheEnabled() && resourceCache != null) {
            cacheKey = createCacheKey(OPERATION_KEY_FIND_RESOURCES, resourceSpace, baseAbsPath, pathVariables);
            ValueWrapper cacheDataWrapper = resourceCache.get(cacheKey);

            if (cacheDataWrapper != null) {
                Object cachedData = cacheDataWrapper.get();
                resource = (Resource) resourceResolver.fromCacheData(cachedData);
            }
        }

        if (resource == null) {
            resource = resourceResolver.findResources(baseAbsPath, pathVariables);

            if (resource != null && cacheKey != null && resourceResolver.isCacheable(resource)) {
                final Object cacheData = resourceResolver.toCacheData(resource);

                if (cacheData != null) {
                    resourceCache.put(cacheKey, cacheData);
                }
            }
        }

        return resource;
    }

    protected ValueMap createCacheKey(final String operationKey, final String resourceSpace, final String resourcePath,
            final Map<String, Object> variables) {
        final ValueMap cacheKey = new DefaultValueMap();
        cacheKey.put(OPERATION_KEY, operationKey);
        cacheKey.put(RESOURCE_SPACE, resourceSpace);
        cacheKey.put(RESOURCE_PATH, resourcePath);
        cacheKey.put(VARIABLES, variables);
        return cacheKey;
    }
}
