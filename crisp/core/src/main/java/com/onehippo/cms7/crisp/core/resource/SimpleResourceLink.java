/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.resource;

import com.onehippo.cms7.crisp.api.resource.ResourceLink;

/**
 * Simple {@link ResourceLink} implementation.
 */
public class SimpleResourceLink implements ResourceLink {

    /**
     * Link URI.
     */
    private String uri;

    /**
     * Constructs with URI string.
     * @param uri URI string
     */
    public SimpleResourceLink(String uri) {
        this.uri = uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUri() {
        return uri;
    }
}
