/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

import java.util.Collections;

/**
 * Abstract {@link ResourceLinkResolver} base class.
 */
public abstract class AbstractResourceLinkResolver implements ResourceLinkResolver {

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceLink resolve(Resource resource) throws ResourceException {
        return resolve(resource, Collections.emptyMap());
    }

}
