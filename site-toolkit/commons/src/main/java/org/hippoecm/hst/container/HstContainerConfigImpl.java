package org.hippoecm.hst.container;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.core.container.HstContainerConfig;

public class HstContainerConfigImpl implements HstContainerConfig {
    
    private final ServletConfig servletConfig;
    private final ClassLoader contextClassLoader;

    public HstContainerConfigImpl(final ServletConfig servletConfig, final ClassLoader contextClassLoader) {
        this.servletConfig = servletConfig;
        this.contextClassLoader = contextClassLoader;
    }
    
    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }

    public ClassLoader getContextClassLoader() {
        return this.contextClassLoader;
    }

}
