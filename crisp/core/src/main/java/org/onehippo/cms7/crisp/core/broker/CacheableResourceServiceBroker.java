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
package org.onehippo.cms7.crisp.core.broker;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.onehippo.cms7.crisp.api.broker.AbstractResourceServiceBroker;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBrokerRequestContext;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceDataCache;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.api.resource.ValueMap;
import org.onehippo.cms7.crisp.core.resource.DefaultValueMap;
import org.onehippo.cms7.crisp.core.resource.SpringResourceDataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.concurrent.ConcurrentMapCache;

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
     * Cache key attribute name for the exchange hint.
     */
    private static final String EXCHANGE_HINT = "exchangeHint";

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
     * Servlet request attribute name for the attribute in which the request level ResourceDataCache is stored.
     */
    private static final String REQUEST_LEVEL_RESOURCE_DATA_CACHE_ATTR_NAME = CacheableResourceServiceBroker.class
            .getName() + ".requestLevelResourceDataCache";

    /**
     * Resource instance representing NULL in cache.
     */
    private static final Resource NULL_RESOURCE_INSTANCE = new NullResource();

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
     * Flag whether or not resource caching in request level is enabled. True by default.
     */
    private boolean cacheInRequestEnabled = true;

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
     * Return true if resource caching in request level is enabled.
     * @return true if resource caching in request level is enabled
     */
    public boolean isCacheInRequestEnabled() {
        return cacheInRequestEnabled;
    }

    /**
     * Set the flag whether or not resource caching in request level is enabled.
     * @param cacheInRequestEnabled the flag whether or not resource caching in request level is enabled
     */
    public void setCacheInRequestEnabled(boolean cacheInRequestEnabled) {
        this.cacheInRequestEnabled = cacheInRequestEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource resolve(String resourceSpace, String absResourcePath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        Resource resource = null;
        ValueMap cacheKey = null;

        if (isCacheInRequestEnabled() && ResourceServiceBrokerRequestContext.hasCurrentServletRequest()) {
            cacheKey = createCacheKey(OPERATION_KEY_RESOLVE, resourceSpace, absResourcePath, pathVariables, exchangeHint);
            resource = getResourceCacheInRequestLevelCache(cacheKey);

            if (resource == NULL_RESOURCE_INSTANCE) {
                return null;
            } else if (resource != null) {
                return resource;
            }
        }

        ResourceResolver resourceResolver = getResourceResolver(resourceSpace);
        ResourceDataCache resourceDataCache = getResourceDataCache(resourceSpace);

        if (isCacheEnabled() && resourceDataCache != null) {
            if (cacheKey == null) {
                cacheKey = createCacheKey(OPERATION_KEY_RESOLVE, resourceSpace, absResourcePath, pathVariables, exchangeHint);
            }

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
            resource = resourceResolver.resolve(absResourcePath, pathVariables, exchangeHint);

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

        if (isCacheInRequestEnabled() && ResourceServiceBrokerRequestContext.hasCurrentServletRequest()) {
            putResourceCacheInRequestLevelCache(cacheKey, resource);
        }

        return resource;
    }


    @Override
    public Binary resolveBinary(String resourceSpace, String absPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        ResourceResolver resourceResolver = getResourceResolver(resourceSpace);
        return resourceResolver.resolveBinary(absPath, pathVariables, exchangeHint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        Resource resource = null;
        ResourceResolver resourceResolver = getResourceResolver(resourceSpace);
        ValueMap cacheKey = null;

        if (isCacheInRequestEnabled() && ResourceServiceBrokerRequestContext.hasCurrentServletRequest()) {
            cacheKey = createCacheKey(OPERATION_KEY_RESOLVE, resourceSpace, baseAbsPath, pathVariables, exchangeHint);
            resource = getResourceCacheInRequestLevelCache(cacheKey);

            if (resource == NULL_RESOURCE_INSTANCE) {
                return null;
            } else if (resource != null) {
                return resource;
            }
        }

        ResourceDataCache resourceDataCache = getResourceDataCache(resourceSpace);

        if (isCacheEnabled() && resourceDataCache != null) {
            if (cacheKey == null) {
                cacheKey = createCacheKey(OPERATION_KEY_FIND_RESOURCES, resourceSpace, baseAbsPath, pathVariables, exchangeHint);
            }

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
            resource = resourceResolver.findResources(baseAbsPath, pathVariables, exchangeHint);

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

        if (isCacheInRequestEnabled() && ResourceServiceBrokerRequestContext.hasCurrentServletRequest()) {
            putResourceCacheInRequestLevelCache(cacheKey, resource);
        }

        return resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceDataCache getResourceDataCache(String resourceSpace) {
        ResourceResolver resourceResolver = getResourceResolver(resourceSpace);

        // If a resourceResolver is explicitly disabled on caching, return null.
        if (!resourceResolver.isCacheEnabled()) {
            return null;
        }

        ResourceDataCache resourceDataCache = resourceResolver.getResourceDataCache();

        // If a resourceResolver doesn't have its own cache, return the default cache.
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
            final Map<String, Object> variables, final ExchangeHint exchangeHint) {
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

        if (exchangeHint != null) {
            cacheKey.put(EXCHANGE_HINT, exchangeHint.getCacheKey());
        }

        return cacheKey;
    }

    private Resource getResourceCacheInRequestLevelCache(final ValueMap cacheKey) {
        ResourceDataCache requestLevelResourceDataCache = getRequestLevelResourceDataCache();
        return (Resource) requestLevelResourceDataCache.getData(cacheKey);
    }

    private void putResourceCacheInRequestLevelCache(final ValueMap cacheKey, final Resource resource) {
        ResourceDataCache requestLevelResourceDataCache = getRequestLevelResourceDataCache();
        requestLevelResourceDataCache.putData(cacheKey, (resource != null) ? resource : NULL_RESOURCE_INSTANCE);
    }

    private ResourceDataCache getRequestLevelResourceDataCache() {
        ResourceDataCache requestLevelResourceDataCache = null;
        final ServletRequest request = ResourceServiceBrokerRequestContext.getCurrentServletRequest();

        if (request != null) {
            requestLevelResourceDataCache = (ResourceDataCache) request
                    .getAttribute(REQUEST_LEVEL_RESOURCE_DATA_CACHE_ATTR_NAME);

            if (requestLevelResourceDataCache == null) {
                synchronized (request) {
                    requestLevelResourceDataCache = (ResourceDataCache) request
                            .getAttribute(REQUEST_LEVEL_RESOURCE_DATA_CACHE_ATTR_NAME);

                    if (requestLevelResourceDataCache == null) {
                        requestLevelResourceDataCache = new SpringResourceDataCache(
                                new ConcurrentMapCache(REQUEST_LEVEL_RESOURCE_DATA_CACHE_ATTR_NAME));
                        request.setAttribute(REQUEST_LEVEL_RESOURCE_DATA_CACHE_ATTR_NAME,
                                requestLevelResourceDataCache);
                    }
                }
            }
        }

        return requestLevelResourceDataCache;
    }
}
