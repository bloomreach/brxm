package org.hippoecm.hst.core.component;

import java.util.Map;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * A HstComponent can be invoked by a HstComponent container
 * during three different request lifecycle phases: ACTION, RESOURCE and RENDER
 */
public interface HstComponent {

    void init(Map<String, Object> properties);
    
    void doBeforeRender(HstRequestContext requestContext, HstRequest request, HstResponse response);
    
    void doRender(HstRequestContext requestContext, HstRequest request, HstResponse response);
    
    void doAction(HstRequestContext requestContext, HstRequest request, HstResponse response);
    
    void doBeforeServeResource(HstRequestContext requestContext, HstRequest request, HstResponse response);
    
    void doServeResource(HstRequestContext requestContext, HstRequest request, HstResponse response);
    
    void destroy();
    
}
