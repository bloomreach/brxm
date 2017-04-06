package org.onehippo.cms7.crisp.api.broker;

import java.util.Collections;
import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceContainer;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceLink;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;

public abstract class AbstractResourceServiceBroker implements ResourceServiceBroker {

    public AbstractResourceServiceBroker() {
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
        ResourceResolver resolver = getResourceResolverByResourceSpace(resourceSpace);

        if (resolver != null) {
            return resolver.getResourceLinkResolver().resolve(resource, linkVariables);
        }

        return null;
    }

    protected abstract ResourceResolver getResourceResolverByResourceSpace(String resourceSpace);

}
