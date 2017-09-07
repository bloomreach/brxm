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
package org.onehippo.cms7.crisp.api.broker;

import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceDataCache;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceLink;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;

/**
 * Abstract delegating Resource Service Broker adaptor class.
 */
public abstract class AbstractDelegatingResourceServiceBroker implements ResourceServiceBroker {

    private final ResourceServiceBroker delegated;

    public AbstractDelegatingResourceServiceBroker(final ResourceServiceBroker delegated) {
        this.delegated = delegated;
    }

    @Override
    public Resource resolve(String resourceSpace, String absPath) throws ResourceException {
        return delegated.resolve(resourceSpace, absPath);
    }

    @Override
    public Resource resolve(String resourceSpace, String absPath, Map<String, Object> pathVariables)
            throws ResourceException {
        return delegated.resolve(resourceSpace, absPath, pathVariables);
    }

    @Override
    public Binary resolveBinary(String resourceSpace, String absPath) throws ResourceException {
        return delegated.resolveBinary(resourceSpace, absPath);
    }

    @Override
    public Binary resolveBinary(String resourceSpace, String absPath, Map<String, Object> pathVariables)
            throws ResourceException {
        return delegated.resolveBinary(resourceSpace, absPath, pathVariables);
    }

    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath) throws ResourceException {
        return delegated.findResources(resourceSpace, baseAbsPath);
    }

    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables)
            throws ResourceException {
        return delegated.findResources(resourceSpace, baseAbsPath, pathVariables);
    }

    @Override
    public ResourceLink resolveLink(String resourceSpace, Resource resource) throws ResourceException {
        return delegated.resolveLink(resourceSpace, resource);
    }

    @Override
    public ResourceLink resolveLink(String resourceSpace, Resource resource, Map<String, Object> linkVariables)
            throws ResourceException {
        return delegated.resolveLink(resourceSpace, resource, linkVariables);
    }

    @Override
    public ResourceDataCache getResourceDataCache(String resourceSpace) throws ResourceException {
        return delegated.getResourceDataCache(resourceSpace);
    }

}
