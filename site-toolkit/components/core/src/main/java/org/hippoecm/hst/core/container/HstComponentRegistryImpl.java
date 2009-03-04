/*
 *  Copyright 2008 Hippo.
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.core.component.HstComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstComponentRegistryImpl implements HstComponentRegistry {
    
    static Logger log = LoggerFactory.getLogger(HstComponentRegistryImpl.class);
    
    protected Map<ServletConfig, Map<String, HstComponent>> servletConfigComponentsMap = Collections.synchronizedMap(new HashMap<ServletConfig, Map<String, HstComponent>>());

    public HstComponent getComponent(ServletConfig servletConfig, String componentId) {
        return getServletConfigComponentsMap(servletConfig, true).get(componentId);
    }

    public void registerComponent(ServletConfig servletConfig, String componentId, HstComponent component) {
        getServletConfigComponentsMap(servletConfig, true).put(componentId, component);
    }

    public void unregisterComponent(ServletConfig servletConfig, String componentId) {
        HstComponent component = getServletConfigComponentsMap(servletConfig, true).remove(componentId);
        
        if (component != null) {
            try {
                component.destroy();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception occurred during destroying component: {}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Exception occurred during destroying component: {}", e.getMessage());
                }
            }
        }
    }
    
    public void unregisterAllComponents() {
        if (this.servletConfigComponentsMap.isEmpty()) {
            return;
        }
        
        Map<ServletConfig, Map<String, HstComponent>> copiedMap = Collections.synchronizedMap(new HashMap<ServletConfig, Map<String, HstComponent>>());
        
        synchronized (this.servletConfigComponentsMap) {
            for (ServletConfig servletConfig : this.servletConfigComponentsMap.keySet()) {
                copiedMap.put(servletConfig, new HashMap<String, HstComponent>());
            }
        }
        
        for (ServletConfig servletConfig : copiedMap.keySet()) {
            Map<String, HstComponent> compMap = getServletConfigComponentsMap(servletConfig, false);
            
            if (compMap != null) {
                Map<String, HstComponent> copiedCompMap = new HashMap<String, HstComponent>();
                
                synchronized (compMap) {
                    for (Map.Entry<String, HstComponent> compEntry : compMap.entrySet()) {
                        copiedCompMap.put(compEntry.getKey(), compEntry.getValue());
                    }
                }

                copiedMap.put(servletConfig, copiedCompMap);
            }
        }
        
        for (Map.Entry<ServletConfig, Map<String, HstComponent>> entry : copiedMap.entrySet()) {
            for (Map.Entry<String, HstComponent> compEntry : entry.getValue().entrySet()) {
                unregisterComponent(entry.getKey(), compEntry.getKey());
            }
        }
        
        this.servletConfigComponentsMap.clear();
    }
    
    protected Map<String, HstComponent> getServletConfigComponentsMap(ServletConfig servletConfig, boolean create) {
        Map<String, HstComponent> componentsMap = this.servletConfigComponentsMap.get(servletConfig);
        
        if (componentsMap == null && create) {
            componentsMap = Collections.synchronizedMap(new HashMap<String, HstComponent>());
            this.servletConfigComponentsMap.put(servletConfig, componentsMap);
        }
        
        return this.servletConfigComponentsMap.get(servletConfig);
    }
}
