package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;

public interface HstContainerConfig {

    ServletConfig getServletConfig();
    
    ClassLoader getContextClassLoader();
    
}
