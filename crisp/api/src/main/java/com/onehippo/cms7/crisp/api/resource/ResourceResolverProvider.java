package com.onehippo.cms7.crisp.api.resource;

public interface ResourceResolverProvider {

    ResourceResolver getResourceResolver(String resourceSpace);

}
