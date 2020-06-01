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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.api.resource.ResourceResolverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * Simple {@link ResourceResolverProvider} implementation based on a <code>Map</code> instance which has entries
 * of <strong>resource space</strong> name and <code>ResourceResolver</code> object pairs.
 */
public class MapResourceResolverProvider implements ResourceResolverProvider, BeanFactoryAware {

    private static Logger log = LoggerFactory.getLogger(MapResourceResolverProvider.class);

    /**
     * Internal <code>Map</code> instance which has entries of <strong>resource space</strong> name and <code>ResourceResolver</code>
     * object pairs.
     */
    private Map<String, ResourceResolver> resourceResolverMap = new ConcurrentHashMap<>();

    private BeanFactory beanFactory;

    /**
     * Default constructor.
     */
    public MapResourceResolverProvider() {
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * Returns the internal <code>Map</code> instance which has entries of <strong>resource space</strong> name
     * and <code>ResourceResolver</code> object pairs.
     * @return the internal <code>Map</code> instance which has entries of <strong>resource space</strong> name
     *         and <code>ResourceResolver</code> object pairs
     */
    public Map<String, ResourceResolver> getResourceResolverMap() {
        return Collections.unmodifiableMap(resourceResolverMap);
    }

    /**
     * Sets the internal <code>Map</code> instance which has entries of <strong>resource space</strong> name
     * and <code>ResourceResolver</code> object pairs
     * @param resourceResolverMap the internal <code>Map</code> instance which has entries of <strong>resource
     *        space</strong> name and <code>ResourceResolver</code> object pairs
     */
    public synchronized void setResourceResolverMap(Map<String, ResourceResolver> resourceResolverMap) {
        this.resourceResolverMap.clear();

        if (resourceResolverMap != null) {
            this.resourceResolverMap.putAll(resourceResolverMap);
        }
    }

    @Override
    public ResourceResolver getResourceResolver(String resourceSpace) {
        ResourceResolver resourceResolver = resourceResolverMap.get(resourceSpace);

        // If a resourceResolver is not found in the internal map, fall back to the spring bean assembly
        // by finding the resourceResolver by the resourceSpace name as bean name.
        if (resourceResolver == null && beanFactory != null) {
            try {
                resourceResolver = beanFactory.getBean(resourceSpace, ResourceResolver.class);
            } catch (BeansException ignore) {
            }
        }

        return resourceResolver;
    }

    /**
     * Set a resource resolver by the {@code resourceSpace}.
     * @param resourceSpace resource space name
     * @param resourceResolver a {@link ResourceResolver}
     */
    public void setResourceResolver(String resourceSpace, ResourceResolver resourceResolver) {
        resourceResolverMap.put(resourceSpace, resourceResolver);
    }

}
