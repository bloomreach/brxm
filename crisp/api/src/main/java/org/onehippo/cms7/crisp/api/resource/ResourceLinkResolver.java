package org.onehippo.cms7.crisp.api.resource;

public interface ResourceLinkResolver {

    ResourceLink resolve(ResourceContainer resource) throws ResourceException;

}
