package org.hippoecm.hst.core.component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;

public class HstComponentContextImpl implements HstComponentContext {

    protected String contextName;
    protected ServletConfig servletConfig;
    protected ClassLoader classLoader;
    protected Map<String, HstComponent> componentMap = Collections.synchronizedMap(new HashMap<String, HstComponent>());
    
    public HstComponentContextImpl(String contextName, ServletConfig servletConfig, ClassLoader classLoader) {
        this.contextName = contextName;
        this.servletConfig = servletConfig;
        this.classLoader = classLoader;
    }
    
    public String getContextName() {
        return this.contextName;
    }
    
    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }
    
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }
    
    public HstComponent getComponent(String name) {
        return this.componentMap.get(name);
    }
    
    public void registerComponent(String name, HstComponent component) {
        this.componentMap.put(name, component);
    }
    
    public void unregisterComponent(String name) {
        this.componentMap.remove(name);
    }

}
