package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.component.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * The component responsible for delegating a incoming servlet request to
 * a HstComponent and possibly its child HstComponents as well.
 */
public interface HstRequestProcessor {
    
    void processAction(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentWindow componentWindow) throws ContainerException;

    void processBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentWindow componentWindow) throws ContainerException;
    
    void processRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentWindow componentWindow) throws ContainerException;
    
    void processBeforeServeResource(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentWindow componentWindow) throws ContainerException;
    
    void processServeResource(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentWindow componentWindow) throws ContainerException;
    
}
