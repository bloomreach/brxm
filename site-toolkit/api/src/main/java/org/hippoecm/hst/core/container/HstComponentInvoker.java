package org.hippoecm.hst.core.container;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * HstComponent invoker component. This invoker's method should be called by the components of the portal.
 */
public interface HstComponentInvoker {
    
    void invokeAction(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    void invokeBeforeRender(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeRender(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeBeforeServeResource(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeServeResource(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
}
