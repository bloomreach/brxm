/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

import java.util.Map;

public interface ResourceLinkResolver {

    ResourceLink resolve(Resource resource) throws ResourceException;

    ResourceLink resolve(Resource resource, Map<String, Object> variables) throws ResourceException;

}
