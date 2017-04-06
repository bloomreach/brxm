package org.onehippo.cms7.crisp.api.resource;

import java.util.Collections;

public abstract class AbstractResourceLinkResolver implements ResourceLinkResolver {

    @Override
    public ResourceLink resolve(ResourceContainer resource) throws ResourceException {
        return resolve(resource, Collections.emptyMap());
    }

}
