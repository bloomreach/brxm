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
