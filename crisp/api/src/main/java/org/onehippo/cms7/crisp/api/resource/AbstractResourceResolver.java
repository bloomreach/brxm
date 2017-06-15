/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.cms7.crisp.api.resource;

import java.util.Collections;

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
        return resolve(absPath, Collections.emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource findResources(String baseAbsPath) throws ResourceException {
        return findResources(baseAbsPath, Collections.emptyMap());
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
}
