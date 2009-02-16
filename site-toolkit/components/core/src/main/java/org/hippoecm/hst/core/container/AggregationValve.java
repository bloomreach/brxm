package org.hippoecm.hst.core.container;

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.component.HstComponentWindow;

public class AggregationValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        
        if (!context.getServletResponse().isCommitted() && !isResourceRequest()) {

            HstComponentWindow rootWindow = context.getRootComponentWindow();
            
            if (rootWindow != null) {
                ServletRequest servletRequest = context.getServletRequest();
                servletRequest.setAttribute(HstComponentWindow.class.getName() + ".render.root", rootWindow);
                
                aggregateAndProcessBeforeRender(servletRequest, context.getServletResponse(), rootWindow);
                aggregateAndProcessRender(context.getServletRequest(), context.getServletResponse(), rootWindow);
            }
        }
        
        // continue
        context.invokeNext();
    }

    protected void aggregateAndProcessBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse, HstComponentWindow window) throws ContainerException {
        HstComponentInvoker invoker = getComponentInvoker(window.getContextName());
        invoker.invokeBeforeRender(servletRequest, servletResponse);

        for (Map.Entry<String, HstComponentWindow> entry : window.getChildWindowMap().entrySet()) {
            aggregateAndProcessBeforeRender(servletRequest, servletResponse, entry.getValue());
        }
        
    }
    
    protected void aggregateAndProcessRender(ServletRequest servletRequest, ServletResponse servletResponse, HstComponentWindow window) throws ContainerException {
        
        for (Map.Entry<String, HstComponentWindow> entry : window.getChildWindowMap().entrySet()) {
            aggregateAndProcessRender(servletRequest, servletResponse, entry.getValue());
        }
    
        HstComponentInvoker invoker = getComponentInvoker(window.getContextName());
        invoker.invokeRender(servletRequest, servletResponse);
    }
}
