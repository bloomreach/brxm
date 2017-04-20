package com.onehippo.cms7.crisp.api.resource;

import java.util.Map;

public interface ResourceLinkResolver {

    ResourceLink resolve(ResourceContainer resource) throws ResourceException;

    ResourceLink resolve(ResourceContainer resource, Map<String, Object> variables) throws ResourceException;

}
