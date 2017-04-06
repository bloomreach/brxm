package org.onehippo.cms7.crisp.core.broker;

import java.util.Map;

import org.onehippo.cms7.crisp.api.broker.AbstractResourceServiceBroker;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;

public abstract class AbstractMultiResolverResourceServiceBroker extends AbstractResourceServiceBroker {

    private Map<String, ResourceResolver> resourceResolverMap;

    public AbstractMultiResolverResourceServiceBroker() {
        super();
    }

    public Map<String, ResourceResolver> getResourceResolverMap() {
        return resourceResolverMap;
    }

    public void setResourceResolverMap(Map<String, ResourceResolver> resourceResolverMap) {
        this.resourceResolverMap = resourceResolverMap;
    }

    @Override
    protected ResourceResolver getResourceResolverByResourceSpace(String resourceSpace) {
        ResourceResolver resourceResolver = null;

        if (resourceResolverMap != null) {
            resourceResolver = resourceResolverMap.get(resourceSpace);
        }

        if (resourceResolver == null) {
            throw new ResourceException("Not resource resolver found for resource space, '" + resourceSpace + "'.");
        }

        return resourceResolver;
    }
}
