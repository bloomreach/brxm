package org.onehippo.cms7.crisp.api.resource;

import java.util.Collections;

public abstract class AbstractResourceResolver extends AbstractResourceCacheResolvable implements ResourceResolver {

    private ResourceLinkResolver resourceLinkResolver;

    @Override
    public Resource resolve(String absPath) throws ResourceException {
        return resolve(absPath, Collections.emptyMap());
    }

    @Override
    public ResourceContainer findResources(String baseAbsPath) throws ResourceException {
        return findResources(baseAbsPath, Collections.emptyMap());
    }

    @Override
    public boolean isLive() throws ResourceException {
        return true;
    }

    @Override
    public void refresh() throws ResourceException {
    }

    @Override
    public void close() throws ResourceException {
    }

    @Override
    public ResourceLinkResolver getResourceLinkResolver() {
        return resourceLinkResolver;
    }

    public void setResourceLinkResolver(ResourceLinkResolver resourceLinkResolver) {
        this.resourceLinkResolver = resourceLinkResolver;
    }
}
