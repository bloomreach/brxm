package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * The component responsible for delegating a incoming servlet request to
 * a HstComponent and possibly its child HstComponents as well.
 */
public interface HstComponentInvoker {
    
    void invokeAction(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;

    void invokeBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeRender(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeBeforeServeResource(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    void invokeServeResource(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
}
