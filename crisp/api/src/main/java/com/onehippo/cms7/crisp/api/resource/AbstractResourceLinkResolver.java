/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

import java.util.Collections;

public abstract class AbstractResourceLinkResolver implements ResourceLinkResolver {

    @Override
    public ResourceLink resolve(Resource resource) throws ResourceException {
        return resolve(resource, Collections.emptyMap());
    }

}
