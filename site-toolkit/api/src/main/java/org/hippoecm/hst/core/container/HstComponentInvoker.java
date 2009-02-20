package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * HstComponent invoker component. This invoker's method should be called by the components of the portal.
 */
public interface HstComponentInvoker {
    
    void invokeAction(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    void invokeBeforeRender(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeRender(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeBeforeServeResource(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeServeResource(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
}
