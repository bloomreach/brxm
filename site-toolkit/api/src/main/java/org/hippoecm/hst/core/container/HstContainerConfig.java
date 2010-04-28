package org.hippoecm.hst.core.container;

import javax.servlet.ServletContext;

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
     * Returns the servletContext of the web application where the HstComponents are located.
     * @return
     */
    ServletContext getServletContext();
    
    /**
     * Returns the classloader of the web application where the HstComponents are located.
     * @return
     */
    ClassLoader getContextClassLoader();
    
}
