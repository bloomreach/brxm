/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.mock.broker;

import java.util.Map;

import org.onehippo.cms7.crisp.api.broker.AbstractResourceServiceBroker;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.mock.module.MockCrispHstServices;
import org.onehippo.cms7.crisp.mock.resource.MockJacksonResourceResolverAdapter;
import org.onehippo.cms7.crisp.mock.resource.MockJdomResourceResolverAdapter;

/**
 * Simple {@link ResourceServiceBroker} adapter class for mocking in unit tests.
 * <P>
 * Basically, you can construct one with a map of pairs of {@link ResourceResolver}, each entry of which consists
 * of the resource space name entry key and the {@link ResourceResolver} entry value.
 * This adapter simply looks up a {@link ResourceResolver} by the resource space name and delegate the call to
 * the {@link ResourceResolver} if found.
 * <P>
 * Here's a code example:
 * <PRE>
 *       final URL productJsonFileUrl = getClass().getResource("product-output.json");
 *
 *       // mocking resourceResolver to override #findResources() only
 *       // and return a json output directly from a classpath resource.
 *       final Map<String, ResourceResolver> resourceResolverMap = new HashMap<>();
 *       resourceResolverMap.put("productResources", new MockJacksonResourceResolverAdapter() {
 *           &#64;Override
 *           public Resource findResources(String baseAbsPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
 *                   throws ResourceException {
 *               return urlToResource(productJsonFileUrl);
 *           }
 *       });
 *       // you can add more resourceResolver(s)...
 *
 *       // mocking resourceServiceBroker.
 *       ResourceServiceBroker mockBroker = new MockResourceServiceBroker(resourceResolverMap);
 *       // register the mocking resourceServiceBroker.
 *       MockCrispHstServices.setDefaultResourceServiceBroker(mockBroker);
 *
 *       // From now on, CrispHstServices.getDefaultResourceServiceBroker() will return the {@code resourceServiceBroker}
 *       // which was mocked above...
 *       ResourceServiceBroker broker = CrispHstServices.getDefaultResourceServiceBroker(); // broker == mockBroker
 *       // ...
 * </PRE>
 *
 * @see MockCrispHstServices
 * @see MockJacksonResourceResolverAdapter
 * @see MockJdomResourceResolverAdapter
 */
public class MockResourceServiceBroker extends AbstractResourceServiceBroker {

    private final Map<String, ResourceResolver> resourceResolverMap;

    public MockResourceServiceBroker() {
        this(null);
    }

    public MockResourceServiceBroker(final Map<String, ResourceResolver> resourceResolverMap) {
        this.resourceResolverMap = resourceResolverMap;
    }

    @Override
    public Resource resolve(String resourceSpace, String absPath, Map<String, Object> pathVariables,
            ExchangeHint exchangeHint) throws ResourceException {
        if (resourceResolverMap == null || !resourceResolverMap.containsKey(resourceSpace)) {
            return null;
        }

        return resourceResolverMap.get(resourceSpace).resolve(absPath, pathVariables, exchangeHint);
    }

    @Override
    public Binary resolveBinary(String resourceSpace, String absPath, Map<String, Object> pathVariables,
            ExchangeHint exchangeHint) throws ResourceException {
        if (resourceResolverMap == null || !resourceResolverMap.containsKey(resourceSpace)) {
            return null;
        }

        return resourceResolverMap.get(resourceSpace).resolveBinary(absPath, pathVariables, exchangeHint);
    }

    @Override
    public Resource resolveBinaryAsResource(String resourceSpace, String absPath, Map<String, Object> pathVariables,
            ExchangeHint exchangeHint) throws ResourceException {
        if (resourceResolverMap == null || !resourceResolverMap.containsKey(resourceSpace)) {
            return null;
        }

        return resourceResolverMap.get(resourceSpace).resolveBinaryAsResource(absPath, pathVariables, exchangeHint);
    }

    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables,
            ExchangeHint exchangeHint) throws ResourceException {
        if (resourceResolverMap == null || !resourceResolverMap.containsKey(resourceSpace)) {
            return null;
        }

        return resourceResolverMap.get(resourceSpace).findResources(baseAbsPath, pathVariables, exchangeHint);
    }

    @Override
    public ResourceBeanMapper getResourceBeanMapper(String resourceSpace) throws ResourceException {
        if (resourceResolverMap == null || !resourceResolverMap.containsKey(resourceSpace)) {
            throw new UnsupportedOperationException();
        }

        return resourceResolverMap.get(resourceSpace).getResourceBeanMapper();
    }

}
