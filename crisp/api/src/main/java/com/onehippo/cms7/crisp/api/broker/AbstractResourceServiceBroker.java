/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.broker;

import java.util.Collections;
import java.util.Map;

import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.api.resource.ResourceDataCache;
import com.onehippo.cms7.crisp.api.resource.ResourceException;
import com.onehippo.cms7.crisp.api.resource.ResourceLink;
import com.onehippo.cms7.crisp.api.resource.ResourceLinkResolver;
import com.onehippo.cms7.crisp.api.resource.ResourceResolver;
import com.onehippo.cms7.crisp.api.resource.ResourceResolverProvider;

/**
 * Abstract CRISP Resource Service Broker base class.
 */
public abstract class AbstractResourceServiceBroker implements ResourceServiceBroker {

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
        return resolve(resourceSpace, absResourcePath, Collections.emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath) throws ResourceException {
        return findResources(resourceSpace, baseAbsPath, Collections.emptyMap());
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
