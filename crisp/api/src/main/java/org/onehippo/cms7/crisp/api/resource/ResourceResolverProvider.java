/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.cms7.crisp.api.resource;

/**
 * Responsible for providing a {@link ResourceResolver}.
 */
public interface ResourceResolverProvider {

    /**
     * Returns a proper {@link ResourceResolver} by the given resource space name ({@code resourceSpace}).
     * @param resourceSpace resource space name
     * @return a proper {@link ResourceResolver} by the given resource space name ({@code resourceSpace})
     */
    ResourceResolver getResourceResolver(String resourceSpace);

}
