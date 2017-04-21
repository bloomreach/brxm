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

public abstract class AbstractResourceServiceBroker implements ResourceServiceBroker {

    private ResourceResolverProvider resourceResolverProvider;

    public AbstractResourceServiceBroker() {
    }

    public ResourceResolverProvider getResourceResolverProvider() {
        return resourceResolverProvider;
    }

    public void setResourceResolverProvider(ResourceResolverProvider resourceResolverProvider) {
        this.resourceResolverProvider = resourceResolverProvider;
    }

    @Override
    public Resource resolve(String resourceSpace, String absResourcePath) throws ResourceException {
        return resolve(resourceSpace, absResourcePath, Collections.emptyMap());
    }

    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath) throws ResourceException {
        return findResources(resourceSpace, baseAbsPath, Collections.emptyMap());
    }

    @Override
    public ResourceLink resolveLink(String resourceSpace, Resource resource) throws ResourceException {
        return resolveLink(resourceSpace, resource, Collections.emptyMap());
    }

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

    @Override
    public ResourceDataCache getResourceDataCache(String resourceSpace) {
        return null;
    }

    protected ResourceResolver getResourceResolver(String resourceSpace) {
        if (getResourceResolverProvider() == null) {
            throw new ResourceException("No ResourceResolverProvider available.");
        }

        return getResourceResolverProvider().getResourceResolver(resourceSpace);
    }
}
