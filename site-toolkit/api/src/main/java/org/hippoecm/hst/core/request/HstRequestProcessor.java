package org.hippoecm.hst.core.request;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

/**
 * The component responsible for delegating a incoming (Servlet or Portlet) request to
 * a HstComponent and possibly its child HstComponents as well.
 */
public interface HstRequestProcessor {
    
    void processAction(HstRequestContext requestContext, HstComponentConfiguration component) throws Exception;

    void processBeforeRender(HstRequestContext requestContext, HstComponentConfiguration component) throws Exception;
    
    void processRender(HstRequestContext requestContext, HstComponentConfiguration component) throws Exception;
    
    void processBeforeServeResource(HstRequestContext requestContext, HstComponentConfiguration component) throws Exception;
    
    void processServeResource(HstRequestContext requestContext, HstComponentConfiguration component) throws Exception;
    
}
