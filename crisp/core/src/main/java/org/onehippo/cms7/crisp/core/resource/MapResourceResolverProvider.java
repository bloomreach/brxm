/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.crisp.core.resource;

import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.api.resource.ResourceResolverProvider;

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
