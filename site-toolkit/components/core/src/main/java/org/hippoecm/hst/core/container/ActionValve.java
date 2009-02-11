package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.component.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ActionValve extends AbstractValve
{

    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws ContainerException
    {
        if (isActionRequest()) {
            
            HstComponentWindow actionWindow = findActionWindow(context.getRootComponentWindow());
            
            if (actionWindow != null) {
                this.requestProcessor.processAction(context.getServletRequest(), context.getServletResponse(), request, actionWindow);
            } else {
                throw new ContainerException("No action window.");
            }
        }
        
        // continue
        context.invokeNext(request);
    }
    
    protected void sendRedirectNavigation(HstRequestContext request) {
        
    }
    
    protected HstComponentWindow findActionWindow(HstComponentWindow rootWindow) {
        HstComponentWindow actionWindow = null;
        
        return actionWindow;
    }
}
