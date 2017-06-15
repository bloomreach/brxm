/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.cms7.crisp.core.broker;

import java.io.IOException;
import java.util.Map;

import org.onehippo.cms7.crisp.api.broker.AbstractResourceServiceBroker;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceDataCache;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.api.resource.ValueMap;
import org.onehippo.cms7.crisp.core.resource.DefaultValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ResourceServiceBroker} implementation enabling resource data caching in a generic way.
 * <P>
 * This implementation generates a cache key for a result resource object using {@link #createCacheKey(String, String, String, Map)} method.
 * </P>
 */
public class CacheableResourceServiceBroker extends AbstractResourceServiceBroker {

    private static Logger log = LoggerFactory.getLogger(CacheableResourceServiceBroker.class);

    /**
     * Cache key attribute name for an invoked operation name.
     */
    private static final String OPERATION_KEY = "operationKey";

    /**
     * Cache key attribute name for the resource space name in an invocation.
     */
    private static final String RESOURCE_SPACE = "resourceSpace";

    /**
     * Cache key attribute name for the relative resource path in an invocation.
     */
    private static final String RESOURCE_PATH = "resourcePath";

    /**
     * Cache key attribute name for the path variables to be used in the physical invocation path (or URI) expansion.
     */
    private static final String VARIABLES = "variables";

    /**
     * Cache key value of {@link #OPERATION_KEY} in {@link #resolve(String, String, Map)} operation invocation.
     */
    private static final String OPERATION_KEY_RESOLVE = CacheableResourceServiceBroker.class.getName() + ".resolve";

    /**
     * Cache key value of {@link #OPERATION_KEY} in {@link #findResources(String, String, Map)} operation invocation.
     */
    private static final String OPERATION_KEY_FIND_RESOURCES = CacheableResourceServiceBroker.class.getName()
            + ".findResources";

    /**
     * Default global {@link ResourceDataCache} instance shared by all the {@link ResourceResolver}s.
     * If a {@link ResourceResolver} doesn't have its own {@link ResourceDataCache} property, this default global
     * {@link ResourceDataCache} instance when caching the result resources after operation invocations.
     */
    private ResourceDataCache defaultResourceDataCache;

    /**
     * Flag whether or not resource caching is enabled. True by default.
     */
    private boolean cacheEnabled = true;

    /**
     * Default constructor.
     */
    public CacheableResourceServiceBroker() {
        super();
    }

    /**
     * Returns the default global {@link ResourceDataCache} instance shared by all the {@link ResourceResolver}s.
     * @return the default global {@link ResourceDataCache} instance shared by all the {@link ResourceResolver}s
     */
    public ResourceDataCache getDefaultResourceDataCache() {
        return defaultResourceDataCache;
    }

    /**
     * Sets the default global {@link ResourceDataCache} instance shared by all the {@link ResourceResolver}s.
     * @param defaultResourceDataCache the default global {@link ResourceDataCache} instance shared by all the {@link ResourceResolver}s
     */
    public void setDefaultResourceDataCache(ResourceDataCache defaultResourceDataCache) {
        this.defaultResourceDataCache = defaultResourceDataCache;
    }

    /**
     * Returns true if resource caching is enabled.
     * @return true if resource caching is enabled
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * Sets the flag whether or not resource caching is enabled
     * @param cacheEnabled the flag whether or not resource caching is enabled
     */
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    /**
     * {@inheritDoc}
     */
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
                try {
                    resource = (Resource) resourceResolver.fromCacheData(cacheData);
                } catch (IOException e) {
                    log.error("Failed to retrieve resource from cache data.", e);
                }
            }
        }

        if (resource == null) {
            resource = resourceResolver.resolve(absResourcePath, pathVariables);

            if (resource != null && cacheKey != null && resourceResolver.isCacheable(resource)) {
                try {
                    final Object cacheData = resourceResolver.toCacheData(resource);

                    if (cacheData != null) {
                        resourceDataCache.putData(cacheKey, cacheData);
                    }
                } catch (IOException e) {
                    log.error("Failed to convert resource to cache data.", e);
                }
            }
        }

        return resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables)
            throws ResourceException {
        Resource resource = null;
        ResourceResolver resourceResolver = getResourceResolver(resourceSpace);
        ValueMap cacheKey = null;
        ResourceDataCache resourceDataCache = getResourceDataCache(resourceSpace);

        if (isCacheEnabled() && resourceDataCache != null) {
            cacheKey = createCacheKey(OPERATION_KEY_FIND_RESOURCES, resourceSpace, baseAbsPath, pathVariables);
            Object cacheData = resourceDataCache.getData(cacheKey);

            if (cacheData != null) {
                try {
                    resource = (Resource) resourceResolver.fromCacheData(cacheData);
                } catch (IOException e) {
                    log.error("Failed to retrieve resource from cache data.", e);
                }
            }
        }

        if (resource == null) {
            resource = resourceResolver.findResources(baseAbsPath, pathVariables);

            if (resource != null && cacheKey != null && resourceResolver.isCacheable(resource)) {
                try {
                    final Object cacheData = resourceResolver.toCacheData(resource);

                    if (cacheData != null) {
                        resourceDataCache.putData(cacheKey, cacheData);
                    }
                } catch (IOException e) {
                    log.error("Failed to convert resource to cache data.", e);
                }
            }
        }

        return resource;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * Creates a cache key to cache a result resource object in the {@link ResourceDataCache}.
     * @param operationKey operation key as cache key attribute
     * @param resourceSpace resource space name as cache key attribute
     * @param resourcePath relative resource path as cache key attribute
     * @param variables path resolution variables map as cache key attribute
     * @return a cache key to cache a result resource object in the {@link ResourceDataCache}
     */
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
