package org.hippoecm.hst.core.component;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

public interface HstComponentFactory {
    
    void registerComponentContext(String contextName, ServletConfig servletConfig, ClassLoader classLoader);
    
    void unregisterComponentContext(String contextName);
    
    ClassLoader getComponentContextClassLoader(String contextName);
    
    HstComponent getComponentInstance(HstComponentConfiguration compConfig) throws HstComponentException;
    
}
