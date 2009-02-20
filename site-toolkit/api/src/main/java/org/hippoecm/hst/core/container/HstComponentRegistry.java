package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.core.component.HstComponent;

public interface HstComponentRegistry {

    void registerComponent(ServletConfig servletConfig, String componentId, HstComponent component);
    
    void unregisterComponent(ServletConfig servletConfig, String componentId);
    
    HstComponent getComponent(ServletConfig servletConfig, String componentId);
    
}
