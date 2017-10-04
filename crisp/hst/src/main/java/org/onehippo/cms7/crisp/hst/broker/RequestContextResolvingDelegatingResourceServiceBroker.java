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
package org.onehippo.cms7.crisp.hst.broker;

import java.util.Map;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.crisp.api.broker.AbstractDelegatingResourceServiceBroker;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBrokerRequestContext;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceLink;

/**
 * Delegating ResourceServiceBroker with resolving ResourceServiceBrokerRequestContext.
 */
public class RequestContextResolvingDelegatingResourceServiceBroker extends AbstractDelegatingResourceServiceBroker {

    public RequestContextResolvingDelegatingResourceServiceBroker(ResourceServiceBroker delegated) {
        super(delegated);
    }

    @Override
    public Resource resolve(String resourceSpace, String absPath) throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.resolve(resourceSpace, absPath);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public Resource resolve(String resourceSpace, String absPath, ExchangeHint exchangeHint) throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.resolve(resourceSpace, absPath, exchangeHint);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public Resource resolve(String resourceSpace, String absPath, Map<String, Object> pathVariables)
            throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.resolve(resourceSpace, absPath, pathVariables);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public Resource resolve(String resourceSpace, String absPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.resolve(resourceSpace, absPath, pathVariables, exchangeHint);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public Binary resolveBinary(String resourceSpace, String absPath) throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.resolveBinary(resourceSpace, absPath);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public Binary resolveBinary(String resourceSpace, String absPath, ExchangeHint exchangeHint) throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.resolveBinary(resourceSpace, absPath, exchangeHint);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public Binary resolveBinary(String resourceSpace, String absPath, Map<String, Object> pathVariables)
            throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.resolveBinary(resourceSpace, absPath, pathVariables);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public Binary resolveBinary(String resourceSpace, String absPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.resolveBinary(resourceSpace, absPath, pathVariables, exchangeHint);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath) throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.findResources(resourceSpace, baseAbsPath);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath, ExchangeHint exchangeHint) throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.findResources(resourceSpace, baseAbsPath, exchangeHint);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables)
            throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.findResources(resourceSpace, baseAbsPath, pathVariables);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.findResources(resourceSpace, baseAbsPath, pathVariables, exchangeHint);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public ResourceLink resolveLink(String resourceSpace, Resource resource) throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.resolveLink(resourceSpace, resource);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    @Override
    public ResourceLink resolveLink(String resourceSpace, Resource resource, Map<String, Object> linkVariables)
            throws ResourceException {
        try {
            resolveResourceServiceBrokerRequestContext();
            return super.resolveLink(resourceSpace, resource, linkVariables);
        } finally {
            clearResourceServiceBrokerRequestContext();
        }
    }

    private void resolveResourceServiceBrokerRequestContext() {
        HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext != null) {
            ResourceServiceBrokerRequestContext.setCurrentServletRequest(requestContext.getServletRequest());
        }
    }

    private void clearResourceServiceBrokerRequestContext() {
        ResourceServiceBrokerRequestContext.clear();
    }
}
