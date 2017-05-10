/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.resource;

import java.util.Map;

import com.onehippo.cms7.crisp.api.resource.ResourceResolver;
import com.onehippo.cms7.crisp.api.resource.ResourceResolverProvider;

/**
 * Simple {@link ResourceResolverProvider} implementation based on a <code>Map</code> instance which has entries
 * of <strong>resource space</strong> name and <code>ResourceResolver</code> object pairs.
 */
public class MapResourceResolverProvider implements ResourceResolverProvider {

    /**
     * Internal <code>Map</code> instance which has entries of <strong>resource space</strong> name and <code>ResourceResolver</code>
     * object pairs.
     */
    private Map<String, ResourceResolver> resourceResolverMap;

    /**
     * Default constructor.
     */
    public MapResourceResolverProvider() {
    }

    /**
     * Returns the internal <code>Map</code> instance which has entries of <strong>resource space</strong> name
     * and <code>ResourceResolver</code> object pairs.
     * @return the internal <code>Map</code> instance which has entries of <strong>resource space</strong> name
     *         and <code>ResourceResolver</code> object pairs
     */
    public Map<String, ResourceResolver> getResourceResolverMap() {
        return resourceResolverMap;
    }

    /**
     * Sets the internal <code>Map</code> instance which has entries of <strong>resource space</strong> name
     * and <code>ResourceResolver</code> object pairs
     * @param resourceResolverMap the internal <code>Map</code> instance which has entries of <strong>resource
     *        space</strong> name and <code>ResourceResolver</code> object pairs
     */
    public void setResourceResolverMap(Map<String, ResourceResolver> resourceResolverMap) {
        this.resourceResolverMap = resourceResolverMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceResolver getResourceResolver(String resourceSpace) {
        if (resourceResolverMap != null) {
            return resourceResolverMap.get(resourceSpace);
        }

        return null;
    }

}
