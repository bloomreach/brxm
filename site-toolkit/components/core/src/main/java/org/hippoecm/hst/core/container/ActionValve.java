package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;

import org.hippoecm.hst.container.ContainerConstants;
import org.hippoecm.hst.core.component.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ActionValve extends AbstractValve
{

    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        if (isActionRequest()) {
            
            HstComponentWindow actionWindow = findActionWindow(context.getRootComponentWindow());
            
            if (actionWindow != null) {
                ServletRequest servletRequest = context.getServletRequest();
                servletRequest.setAttribute(ContainerConstants.HST_COMPONENT_WINDOW, actionWindow);
                HstComponentInvoker invoker = getComponentInvoker(actionWindow.getContextName());
                invoker.invokeAction(servletRequest, context.getServletResponse());
            } else {
                throw new ContainerException("No action window.");
            }
        }
        
        // continue
        context.invokeNext();
    }
    
    protected void sendRedirectNavigation(HstRequestContext request) {
        
    }
    
    protected HstComponentWindow findActionWindow(HstComponentWindow rootWindow) {
        HstComponentWindow actionWindow = null;
        
        return actionWindow;
    }
}
