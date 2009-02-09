package org.hippoecm.hst.core.container;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ActionValve extends AbstractValve
{
    
    protected HstRequestProcessor requestProcessor;

    public ActionValve(HstRequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }
    
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws ContainerException
    {
        if (isActionRequest()) {
            HstComponentConfiguration target = null;
            
            if (target != null) {
                this.requestProcessor.processAction(context.getServletRequest(), context.getServletResponse(), request, target);
            }
        }
        
        // continue
        context.invokeNext(request);
    }
    
    protected void sendRedirectNavigation(HstRequestContext request) {
        
    }
}
