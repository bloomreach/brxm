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
import java.util.Map;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.core.component.HstComponent;

public class HstComponentRegistryImpl implements HstComponentRegistry {
    
    protected Map<ServletConfig, Map<String, HstComponent>> servletConfigComponentsMap = Collections.synchronizedMap(new HashMap<ServletConfig, Map<String, HstComponent>>());

    public HstComponent getComponent(ServletConfig servletConfig, String componentId) {
        return getServletConfigComponentsMap(servletConfig, true).get(componentId);
    }

    public void registerComponent(ServletConfig servletConfig, String componentId, HstComponent component) {
        getServletConfigComponentsMap(servletConfig, true).put(componentId, component);
    }

    public void unregisterComponent(ServletConfig servletConfig, String componentId) {
        getServletConfigComponentsMap(servletConfig, true).remove(componentId);
    }
    
    protected Map<String, HstComponent> getServletConfigComponentsMap(ServletConfig servletConfig, boolean create) {
        Map<String, HstComponent> componentsMap = this.servletConfigComponentsMap.get(servletConfig);
        
        if (componentsMap == null && create) {
            componentsMap = Collections.synchronizedMap(new HashMap<String, HstComponent>());
            this.servletConfigComponentsMap.put(servletConfig, componentsMap);
        }
        
        return componentsMap;
    }
}
