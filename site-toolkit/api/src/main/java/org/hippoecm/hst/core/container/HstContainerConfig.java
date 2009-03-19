package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;

/**
 * The HstComponent container configuration.
 * Because the container's request processor can be located in other web application
 * and loaded by other class loader for centralized management reason,
 * The HstComponent container servlet should pass an implementation of this interface
 * to the request processor. 
 * 
 * @version $Id$
 */
public interface HstContainerConfig {

    /**
     * Returns the servletConfig of the web application where the HstComponents are located.
     * @return
     */
    ServletConfig getServletConfig();
    
    /**
     * Returns the classloader of the web application where the HstComponents are located.
     * @return
     */
    ClassLoader getContextClassLoader();
    
}
