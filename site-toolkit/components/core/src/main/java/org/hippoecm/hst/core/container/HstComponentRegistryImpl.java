/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentMetadata;
import org.hippoecm.hst.core.component.HstComponentMetadataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstComponentRegistryImpl
 *
 */
public class HstComponentRegistryImpl implements HstComponentRegistry {
    
    static Logger log = LoggerFactory.getLogger(HstComponentRegistryImpl.class);

    protected Map<HstContainerConfig, Map<String, HstComponentHolder>> servletConfigComponentsMap =
            new ConcurrentHashMap<HstContainerConfig, Map<String, HstComponentHolder>>(128);

    public HstComponent getComponent(HstContainerConfig requestContainerConfig, String componentId) {
        HstComponentHolder holder = getServletConfigComponentsMap(requestContainerConfig, true).get(componentId);
        
        if (holder != null) {
            return holder.getComponent();
        }
        return null;
    }

    public HstComponentMetadata getComponentMetadata(HstContainerConfig requestContainerConfig, String componentId) {
        HstComponentHolder holder = getServletConfigComponentsMap(requestContainerConfig, true).get(componentId);
        
        if (holder != null) {
            return holder.getComponentMetadata();
        }
        return null;
    }

    public void registerComponent(HstContainerConfig requestContainerConfig, String componentId, HstComponent component) {
        HstComponentMetadata componentMetadata = HstComponentMetadataReader.getHstComponentMetadata(component.getClass());
        HstComponentHolder componentHolder = new HstComponentHolder(component, componentMetadata);
        getServletConfigComponentsMap(requestContainerConfig, true).put(componentId, componentHolder);
    }

    public void unregisterComponent(HstContainerConfig requestContainerConfig, String componentId) {
        HstComponentHolder holder = getServletConfigComponentsMap(requestContainerConfig, true).remove(componentId);
        
        if (holder != null) {
            try {
                holder.getComponent().destroy();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception occurred during destroying component: {}", e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Exception occurred during destroying component: {}", e.toString());
                }
            }
        }
    }
    
    public void unregisterAllComponents() {
        if (this.servletConfigComponentsMap.isEmpty()) {
            return;
        }
        
        Map<HstContainerConfig, Map<String, HstComponentHolder>> copiedMap = Collections.synchronizedMap(new HashMap<HstContainerConfig, Map<String, HstComponentHolder>>());
        
        synchronized (this.servletConfigComponentsMap) {
            for (HstContainerConfig requestContainerConfig : this.servletConfigComponentsMap.keySet()) {
                copiedMap.put(requestContainerConfig, new HashMap<String, HstComponentHolder>());
            }
        }
        
        for (HstContainerConfig requestContainerConfig : copiedMap.keySet()) {
            Map<String, HstComponentHolder> compMap = getServletConfigComponentsMap(requestContainerConfig, false);
            
            if (compMap != null) {
                Map<String, HstComponentHolder> copiedCompMap = new HashMap<String, HstComponentHolder>();
                
                synchronized (compMap) {
                    for (Map.Entry<String, HstComponentHolder> compEntry : compMap.entrySet()) {
                        copiedCompMap.put(compEntry.getKey(), compEntry.getValue());
                    }
                }

                copiedMap.put(requestContainerConfig, copiedCompMap);
            }
        }
        
        for (Map.Entry<HstContainerConfig, Map<String, HstComponentHolder>> entry : copiedMap.entrySet()) {
            for (Map.Entry<String, HstComponentHolder> compEntry : entry.getValue().entrySet()) {
                unregisterComponent(entry.getKey(), compEntry.getKey());
            }
        }
        
        this.servletConfigComponentsMap.clear();
    }
    
    protected Map<String, HstComponentHolder> getServletConfigComponentsMap(HstContainerConfig requestContainerConfig, boolean create) {
        Map<String, HstComponentHolder> componentsMap = this.servletConfigComponentsMap.get(requestContainerConfig);
        
        if (componentsMap == null && create) {
            componentsMap = new ConcurrentHashMap<String, HstComponentHolder>();
            this.servletConfigComponentsMap.put(requestContainerConfig, componentsMap);
        }
        
        return this.servletConfigComponentsMap.get(requestContainerConfig);
    }

    private static class HstComponentHolder {

        private HstComponent component;
        private HstComponentMetadata componentMetadata;

        private HstComponentHolder(final HstComponent component, final HstComponentMetadata componentMetadata) {
            this.component = component;
            this.componentMetadata = componentMetadata;
        }

        public HstComponent getComponent() {
            return component;
        }

        public HstComponentMetadata getComponentMetadata() {
            return componentMetadata;
        }
    }
    
}
