package org.onehippo.cms7.crisp.api.broker;

import java.util.Collections;

import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceContainable;
import org.onehippo.cms7.crisp.api.resource.ResourceException;

public abstract class AbstractResourceServiceBroker implements ResourceServiceBroker {

    public AbstractResourceServiceBroker() {
    }

    @Override
    public Resource resolve(String resourceSpace, String absResourcePath) throws ResourceException {
        return resolve(resourceSpace, absResourcePath, Collections.emptyMap());
    }

    @Override
    public ResourceContainable findResources(String resourceSpace, String baseAbsPath) throws ResourceException {
        return findResources(resourceSpace, baseAbsPath, Collections.emptyMap());
    }

}
