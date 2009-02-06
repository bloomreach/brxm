package org.hippoecm.hst.core.request;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

/**
 * The component responsible for delegating a incoming servlet request to
 * a HstComponent and possibly its child HstComponents as well.
 */
public interface HstRequestProcessor {
    
    void processAction(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) throws Exception;

    void processBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) throws Exception;
    
    void processRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) throws Exception;
    
    void processBeforeServeResource(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) throws Exception;
    
    void processServeResource(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, HstComponentConfiguration componentConfiguration) throws Exception;
    
}
