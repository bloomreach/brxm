/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.api.broker;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceDataCache;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceLink;
import org.onehippo.cms7.crisp.api.resource.ResourceLinkResolver;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.api.resource.ResourceResolverProvider;

/**
 * Abstract CRISP Resource Service Broker base class.
 */
public abstract class AbstractResourceServiceBroker implements ResourceServiceBroker {

    /**
     * Key name for an invoked operation name, which can be used in cache key generation as an attribute name.
     */
    public static final String OPERATION_KEY = "operationKey";

    /**
     * Key name for the resource space name in an invocation, which can be used in cache key generation as an attribute
     * name.
     */
    public static final String RESOURCE_SPACE = "resourceSpace";

    /**
     * Key name for the relative resource path in an invocation, which can be used in cache key generation as an
     * attribute name.
     */
    public static final String RESOURCE_PATH = "resourcePath";

    /**
     * Key name for the path variables to be used in the physical invocation path (or URI) expansion, which can
     * be used in cache key generation as an attribute name.
     */
    public static final String PATH_VARIABLES = "variables";

    /**
     * Key name for the exchange hint, which can be used in cache key generation as an attribute name.
     */
    public static final String EXCHANGE_HINT = "exchangeHint";

    /**
     * Key name of {@link #OPERATION_KEY} in {@link #resolve(String, String, Map)} operation invocation, which
     * can be used in cache key generation as an attribute name.
     */
    public static final String OPERATION_KEY_RESOLVE = AbstractResourceServiceBroker.class.getName() + ".resolve";

    /**
     * Key name of {@link #OPERATION_KEY} in {@link #findResources(String, String, Map)} operation invocation,
     * which can be used in cache key generation as an attribute name.
     */
    public static final String OPERATION_KEY_FIND_RESOURCES = AbstractResourceServiceBroker.class.getName()
            + ".findResources";

    /**
     * {@link ResourceResolverProvider} that finds and returns a proper {@link ResourceResolver}.
     */
    private ResourceResolverProvider resourceResolverProvider;

    /**
     * Default constructor.
     */
    public AbstractResourceServiceBroker() {
    }

    /**
     * Returns {@link ResourceResolverProvider} that finds and returns a proper {@link ResourceResolver}.
     * @return {@link ResourceResolverProvider} that finds and returns a proper {@link ResourceResolver}
     */
    public ResourceResolverProvider getResourceResolverProvider() {
        return resourceResolverProvider;
    }

    /**
     * Sets {@link ResourceResolverProvider} that finds and returns a proper {@link ResourceResolver}.
     * @param resourceResolverProvider {@link ResourceResolverProvider} that finds and returns a proper {@link ResourceResolver}
     */
    public void setResourceResolverProvider(ResourceResolverProvider resourceResolverProvider) {
        this.resourceResolverProvider = resourceResolverProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource resolve(String resourceSpace, String absResourcePath) throws ResourceException {
        return resolve(resourceSpace, absResourcePath, Collections.emptyMap(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource resolve(String resourceSpace, String absResourcePath, ExchangeHint exchangeHint) throws ResourceException {
        return resolve(resourceSpace, absResourcePath, Collections.emptyMap(), exchangeHint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource resolve(String resourceSpace, String absResourcePath, Map<String, Object> pathVariables) throws ResourceException {
        return resolve(resourceSpace, absResourcePath, pathVariables, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Binary resolveBinary(String resourceSpace, String absResourcePath) throws ResourceException {
        return resolveBinary(resourceSpace, absResourcePath, Collections.emptyMap(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Binary resolveBinary(String resourceSpace, String absResourcePath, ExchangeHint exchangeHint) throws ResourceException {
        return resolveBinary(resourceSpace, absResourcePath, Collections.emptyMap(), exchangeHint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Binary resolveBinary(String resourceSpace, String absResourcePath, Map<String, Object> pathVariables) throws ResourceException {
        return resolveBinary(resourceSpace, absResourcePath, pathVariables, null);
    }

    @Override
    public Resource resolveBinaryAsResource(String resourceSpace, String absResourcePath) throws ResourceException {
        return resolveBinaryAsResource(resourceSpace, absResourcePath, Collections.emptyMap(), null);
    }

    @Override
    public Resource resolveBinaryAsResource(String resourceSpace, String absResourcePath, ExchangeHint exchangeHint) throws ResourceException {
        return resolveBinaryAsResource(resourceSpace, absResourcePath, Collections.emptyMap(), exchangeHint);
    }

    @Override
    public Resource resolveBinaryAsResource(String resourceSpace, String absResourcePath, Map<String, Object> pathVariables) throws ResourceException {
        return resolveBinaryAsResource(resourceSpace, absResourcePath, pathVariables, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath) throws ResourceException {
        return findResources(resourceSpace, baseAbsPath, Collections.emptyMap(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath, ExchangeHint exchangeHint) throws ResourceException {
        return findResources(resourceSpace, baseAbsPath, Collections.emptyMap(), exchangeHint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables) throws ResourceException {
        return findResources(resourceSpace, baseAbsPath, pathVariables, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceLink resolveLink(String resourceSpace, Resource resource) throws ResourceException {
        return resolveLink(resourceSpace, resource, Collections.emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceLink resolveLink(String resourceSpace, Resource resource, Map<String, Object> linkVariables)
            throws ResourceException {
        ResourceResolver resolver = getResourceResolver(resourceSpace);

        if (resolver != null) {
            ResourceLinkResolver linkResolver = resolver.getResourceLinkResolver();

            if (linkResolver != null) {
                return linkResolver.resolve(resource, linkVariables);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceDataCache getResourceDataCache(String resourceSpace) throws ResourceException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceBeanMapper getResourceBeanMapper(String resourceSpace) throws ResourceException {
        ResourceResolver resolver = getResourceResolver(resourceSpace);

        if (resolver != null) {
            return resolver.getResourceBeanMapper();
        }

        return null;
    }

    @Override
    public URI resolveFullURI(String resourceSpace, String absPath) throws ResourceException {
        ResourceResolver resolver = getResourceResolver(resourceSpace);

        if (resolver != null) {
            return resolver.resolveFullURI(absPath);
        }

        return null;
    }

    @Override
    public URI resolveFullURI(String resourceSpace, String absPath, Map<String, Object> pathVariables) throws ResourceException {
        ResourceResolver resolver = getResourceResolver(resourceSpace);

        if (resolver != null) {
            return resolver.resolveFullURI(absPath, pathVariables);
        }

        return null;
    }

    /**
     * Finds and returns a {@link ResourceResolver} by the specified {@code resourceSpace}.
     * @param resourceSpace resource space name
     * @return a {@link ResourceResolver} by the specified {@code resourceSpace}
     * @throws ResourceException if resource resolve is not found
     */
    protected ResourceResolver getResourceResolver(String resourceSpace) throws ResourceException {
        if (getResourceResolverProvider() == null) {
            throw new ResourceException("No ResourceResolverProvider available.");
        }

        ResourceResolver resourceResolver = getResourceResolverProvider().getResourceResolver(resourceSpace);

        if (resourceResolver == null) {
            throw new ResourceException("No resource space for '" + resourceSpace + "'.");
        }

        return resourceResolver;
    }
}
