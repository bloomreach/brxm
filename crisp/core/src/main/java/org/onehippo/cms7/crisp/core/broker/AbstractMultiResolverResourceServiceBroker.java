package org.onehippo.cms7.crisp.core.broker;

import java.util.Map;

import org.onehippo.cms7.crisp.api.broker.AbstractResourceServiceBroker;
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

    protected ResourceResolver getResourceResolverByResourceSpace(String resourceSpace) {
        if (resourceResolverMap == null) {
            return null;
        }

        return resourceResolverMap.get(resourceSpace);
    }
}
