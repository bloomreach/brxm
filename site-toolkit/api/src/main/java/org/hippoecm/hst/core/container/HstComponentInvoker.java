package org.hippoecm.hst.core.container;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * HstComponent invoker component. This invoker's method should be called by the components of the portal.
 */
public interface HstComponentInvoker {
    
    void setServletContext(ServletContext servletContext);
    
    void setDispatcherPath(String dispatcherPath);
    
    void invokeAction(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    void invokeBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeRender(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeBeforeServeResource(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeServeResource(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
}
