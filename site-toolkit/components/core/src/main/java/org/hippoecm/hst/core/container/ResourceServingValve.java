package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;

import org.hippoecm.hst.container.ContainerConstants;
import org.hippoecm.hst.core.component.HstComponentWindow;

public class ResourceServingValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {

        if (!context.getServletResponse().isCommitted() && isResourceRequest()) {
            
            HstComponentWindow window = findResourceServingWindow(context.getRootComponentWindow());
            
            if (window != null) {
                ServletRequest servletRequest = context.getServletRequest();
                servletRequest.setAttribute(ContainerConstants.HST_COMPONENT_WINDOW, window);
                HstComponentInvoker invoker = getComponentInvoker(window.getContextName());
                invoker.invokeBeforeServeResource(servletRequest, context.getServletResponse());
                invoker.invokeServeResource(servletRequest, context.getServletResponse());
            }
        }
        
        // continue
        context.invokeNext();
    }

    private HstComponentWindow findResourceServingWindow(HstComponentWindow rootComponentWindow) {
        // TODO Auto-generated method stub
        return null;
    }
}
