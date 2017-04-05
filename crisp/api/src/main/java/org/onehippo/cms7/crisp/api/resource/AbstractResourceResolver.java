package org.onehippo.cms7.crisp.api.resource;

import java.util.Map;

public abstract class AbstractResourceResolver extends AbstractResourceCacheResolvable implements ResourceResolver {

    @Override
    public ResourceContainable findResources(String baseAbsPath, Map<String, Object> variables, String query)
            throws ResourceException {
        return findResources(baseAbsPath, variables, query, null);
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

}
