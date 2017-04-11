package org.onehippo.cms7.crisp.api.broker;

import java.util.Collections;
import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceContainer;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceLink;
import org.onehippo.cms7.crisp.api.resource.ResourceLinkResolver;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.api.resource.ResourceResolverProvider;

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
    public ResourceContainer findResources(String resourceSpace, String baseAbsPath) throws ResourceException {
        return findResources(resourceSpace, baseAbsPath, Collections.emptyMap());
    }

    @Override
    public ResourceLink resolveLink(String resourceSpace, ResourceContainer resource) throws ResourceException {
        return resolveLink(resourceSpace, resource, Collections.emptyMap());
    }

    @Override
    public ResourceLink resolveLink(String resourceSpace, ResourceContainer resource, Map<String, Object> linkVariables)
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

    protected ResourceResolver getResourceResolver(String resourceSpace) {
        if (getResourceResolverProvider() == null) {
            throw new ResourceException("No ResourceResolverProvider available.");
        }

        return getResourceResolverProvider().getResourceResolver(resourceSpace);
    }
}
