package com.onehippo.cms7.crisp.core.resource;

import java.util.Map;

import com.onehippo.cms7.crisp.api.resource.ResourceResolver;
import com.onehippo.cms7.crisp.api.resource.ResourceResolverProvider;

public class MapResourceResolverProvider implements ResourceResolverProvider {

    private Map<String, ResourceResolver> resourceResolverMap;

    public MapResourceResolverProvider() {
    }

    public Map<String, ResourceResolver> getResourceResolverMap() {
        return resourceResolverMap;
    }

    public void setResourceResolverMap(Map<String, ResourceResolver> resourceResolverMap) {
        this.resourceResolverMap = resourceResolverMap;
    }

    @Override
    public ResourceResolver getResourceResolver(String resourceSpace) {
        if (resourceResolverMap != null) {
            return resourceResolverMap.get(resourceSpace);
        }

        return null;
    }

}
