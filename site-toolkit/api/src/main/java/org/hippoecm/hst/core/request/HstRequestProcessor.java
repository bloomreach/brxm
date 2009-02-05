package org.hippoecm.hst.core.request;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

/**
 * The component responsible for delegating a incoming (Servlet or Portlet) request to
 * a HstComponent and possibly its child HstComponents as well.
 */
public interface HstRequestProcessor {
    
    void action(HstRequestContext requestContext, HstComponentConfiguration component) throws Exception;

    void render(HstRequestContext requestContext, HstComponentConfiguration component) throws Exception;
    
    void serveResource(HstRequestContext requestContext, HstComponentConfiguration component) throws Exception;
    
}
