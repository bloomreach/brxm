package org.onehippo.cms7.crisp.api.resource;

public abstract class AbstractResourceResolver implements ResourceResolver {

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
