package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Request processor. This processor should be called by HstComponent dispatcher servlet.
 */
public interface HstRequestProcessor {
    
    /**
     * processes Hst request
     * 
     * @param servletConfig the servletConfig of the HST container servlet
     * @param servletRequest the servletRequest of the HST request
     * @param servletResponse the servletResponse of the HST response
     * @throws ContainerException
     */
    void processRequest(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

}
