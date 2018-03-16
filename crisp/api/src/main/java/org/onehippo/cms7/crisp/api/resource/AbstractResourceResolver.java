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
package org.onehippo.cms7.crisp.api.resource;

import java.util.Collections;
import java.util.Map;

import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;

/**
 * Abstract {@link ResourceResolver} base class.
 */
public abstract class AbstractResourceResolver extends AbstractResourceCacheResolvable implements ResourceResolver {

    /**
     * {@link ResourceLinkResolver} that resolves a link for a {@link Resource} representation.
     */
    private ResourceLinkResolver resourceLinkResolver;

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource resolve(String absPath) throws ResourceException {
        return resolve(absPath, Collections.emptyMap(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource resolve(String absPath, ExchangeHint exchangeHint) throws ResourceException {
        return resolve(absPath, Collections.emptyMap(), exchangeHint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource resolve(String absPath, Map<String, Object> pathVariables) throws ResourceException {
        return resolve(absPath, pathVariables, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Binary resolveBinary(String absPath) throws ResourceException {
        return resolveBinary(absPath, Collections.emptyMap(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Binary resolveBinary(String absPath, ExchangeHint exchangeHint) throws ResourceException {
        return resolveBinary(absPath, Collections.emptyMap(), exchangeHint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Binary resolveBinary(String absPath, Map<String, Object> pathVariables) throws ResourceException {
        return resolveBinary(absPath, pathVariables, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource findResources(String baseAbsPath) throws ResourceException {
        return findResources(baseAbsPath, Collections.emptyMap(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource findResources(String baseAbsPath, ExchangeHint exchangeHint) throws ResourceException {
        return findResources(baseAbsPath, Collections.emptyMap(), exchangeHint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource findResources(String baseAbsPath, Map<String, Object> pathVariables) throws ResourceException {
        return findResources(baseAbsPath, pathVariables, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLive() throws ResourceException {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() throws ResourceException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws ResourceException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceLinkResolver getResourceLinkResolver() {
        return resourceLinkResolver;
    }

    /**
     * Sets {@link ResourceLinkResolver}.
     * @param resourceLinkResolver {@link ResourceLinkResolver} that resolves a link for a {@link Resource} representation
     */
    public void setResourceLinkResolver(ResourceLinkResolver resourceLinkResolver) {
        this.resourceLinkResolver = resourceLinkResolver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceBeanMapper getResourceBeanMapper() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getResourceBackendOperations() throws ResourceException {
        throw new UnsupportedOperationException();
    }
}
