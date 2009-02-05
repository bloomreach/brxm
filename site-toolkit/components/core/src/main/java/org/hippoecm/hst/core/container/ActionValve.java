package org.hippoecm.hst.core.container;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstRequestProcessor;

public class ActionValve extends AbstractValve
{
    
    protected HstRequestProcessor requestProcessor;

    public ActionValve(HstRequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }
    
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws Exception
    {
        if (isActionRequest()) {
            HstComponentConfiguration target = null;
            
            if (target != null) {
                this.requestProcessor.processAction(request, target);
                sendRedirectNavigation(request);
            }
        }
        
        // continue
        context.invokeNext(request);
    }
    
    protected void sendRedirectNavigation(HstRequestContext request) {
        
    }
}
