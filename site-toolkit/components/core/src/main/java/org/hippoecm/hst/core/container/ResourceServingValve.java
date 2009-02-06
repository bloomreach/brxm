package org.hippoecm.hst.core.container;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstRequestProcessor;

public class ResourceServingValve extends AbstractValve {
    
    protected HstRequestProcessor requestProcessor;
    
    public ResourceServingValve(HstRequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }
    
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws Exception {

        if (!context.getServletResponse().isCommitted() && isResourceRequest()) {
            HstComponentConfiguration target = null;
            
            if (target != null) {
                this.requestProcessor.processBeforeServeResource(context.getServletRequest(), context.getServletResponse(), request, target);
                this.requestProcessor.processServeResource(context.getServletRequest(), context.getServletResponse(), request, target);
            }
        }
        
        // continue
        context.invokeNext(request);
    }
}
