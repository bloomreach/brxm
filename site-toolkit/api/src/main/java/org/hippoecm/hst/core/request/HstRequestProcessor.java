package org.hippoecm.hst.core.request;

import org.hippoecm.hst.core.component.HstComponent;

/**
 * The component responsible for delegating a incoming (Servlet or Portlet) request to
 * a HstComponent and possibly its child HstComponents as well.
 */
public interface HstRequestProcessor {
    
    void processAction(HstRequestContext requestContext, HstComponent component) throws Exception;

    void processBeforeRender(HstRequestContext requestContext, HstComponent component) throws Exception;
    
    void processRender(HstRequestContext requestContext, HstComponent component) throws Exception;
    
    void processServeResource(HstRequestContext requestContext, HstComponent component) throws Exception;
    
}
