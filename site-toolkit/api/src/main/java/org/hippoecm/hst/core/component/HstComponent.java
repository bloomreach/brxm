package org.hippoecm.hst.core.component;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

/**
 * A HstComponent can be invoked by a HstComponent container
 * during three different request lifecycle phases: ACTION, RESOURCE and RENDER
 */
public interface HstComponent {

    void init(HstComponentConfiguration config);
    
    void beforeRender(HstRequest request, HstResponse response);
    
    void doRender(HstRequest request, HstResponse response);
    
    void doAction(HstRequest request, HstResponse response);
    
    void doServingResource(HstRequest request, HstResponse response);
    
    void destroy();
    
}
