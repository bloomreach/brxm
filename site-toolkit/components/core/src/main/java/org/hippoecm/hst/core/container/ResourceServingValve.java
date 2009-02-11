package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.component.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ResourceServingValve extends AbstractValve {
    
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws ContainerException {

        if (!context.getServletResponse().isCommitted() && isResourceRequest()) {
            
            HstComponentWindow window = findResourceServingWindow(context.getRootComponentWindow());
            
            if (window != null) {
                this.requestProcessor.processBeforeServeResource(context.getServletRequest(), context.getServletResponse(), request, window);
                this.requestProcessor.processServeResource(context.getServletRequest(), context.getServletResponse(), request, window);
            }
        }
        
        // continue
        context.invokeNext(request);
    }

    private HstComponentWindow findResourceServingWindow(HstComponentWindow rootComponentWindow) {
        // TODO Auto-generated method stub
        return null;
    }
}
