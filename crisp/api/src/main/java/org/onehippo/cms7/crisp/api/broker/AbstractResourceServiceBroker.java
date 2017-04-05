package org.onehippo.cms7.crisp.api.broker;

import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.ResourceContainable;
import org.onehippo.cms7.crisp.api.resource.ResourceException;

public abstract class AbstractResourceServiceBroker implements ResourceServiceBroker {

    public AbstractResourceServiceBroker() {
    }

    @Override
    public ResourceContainable findResources(String resourceSpace, String baseAbsPath, Map<String, Object> variables,
            String query) throws ResourceException {
        return findResources(resourceSpace, baseAbsPath, variables, query, null);
    }

}
