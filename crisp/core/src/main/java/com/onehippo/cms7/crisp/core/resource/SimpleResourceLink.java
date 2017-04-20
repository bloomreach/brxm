/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.resource;

import com.onehippo.cms7.crisp.api.resource.ResourceLink;

public class SimpleResourceLink implements ResourceLink {

    private String uri;

    public SimpleResourceLink(String uri) {
        this.uri = uri;
    }

    @Override
    public String getUri() {
        return uri;
    }
}
