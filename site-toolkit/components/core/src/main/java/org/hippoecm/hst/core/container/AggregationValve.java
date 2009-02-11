package org.hippoecm.hst.core.container;

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.component.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;

public class AggregationValve extends AbstractValve {
    
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws ContainerException {
        
        if (!context.getServletResponse().isCommitted() && !isResourceRequest()) {

            HstComponentWindow rootWindow = null;
            
            if (rootWindow != null) {
                aggregateAndProcessBeforeRender(context.getServletRequest(), context.getServletResponse(), request, rootWindow);
                aggregateAndProcessRender(context.getServletRequest(), context.getServletResponse(), request, rootWindow);
            }
        }
        
        // continue
        context.invokeNext(request);
    }

    protected void aggregateAndProcessBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext context, HstComponentWindow window) throws ContainerException {
        
        this.requestProcessor.processBeforeRender(servletRequest, servletResponse, context, window);

        for (Map.Entry<String, HstComponentWindow> entry : window.getChildWindowMap().entrySet()) {
            aggregateAndProcessBeforeRender(servletRequest, servletResponse, context, entry.getValue());
        }
    }
    
    protected void aggregateAndProcessRender(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext context, HstComponentWindow window) throws ContainerException {
        for (Map.Entry<String, HstComponentWindow> entry : window.getChildWindowMap().entrySet()) {
            aggregateAndProcessRender(servletRequest, servletResponse, context, entry.getValue());
        }
    
        this.requestProcessor.processRender(servletRequest, servletResponse, context, window);
    }
}
