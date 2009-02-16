package org.hippoecm.hst.core.component;

import javax.servlet.ServletConfig;

public interface HstComponentContext {
    
    String LOCAL_COMPONENT_CONTEXT_NAME = HstComponentContext.class.getName() + ".local";
    
    String getContextName();
    
    ServletConfig getServletConfig();
    
    ClassLoader getClassLoader();
    
    HstComponent getComponent(String name);
    
    void registerComponent(String name, HstComponent component);
    
    void unregisterComponent(String name);
    
}