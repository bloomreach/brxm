package org.hippoecm.hst.core.container;

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.container.ContainerConstants;

public class AggregationValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        
        if (!context.getServletResponse().isCommitted() && !isResourceRequest()) {

            HstComponentWindow rootWindow = context.getRootComponentWindow();
            
            if (rootWindow != null) {
                ServletRequest servletRequest = context.getServletRequest();
                servletRequest.setAttribute(ContainerConstants.HST_COMPONENT_METHOD_ID, ContainerConstants.METHOD_RENDER);
                servletRequest.setAttribute(ContainerConstants.HST_COMPONENT_WINDOW, rootWindow);
                
                aggregateAndProcessBeforeRender(servletRequest, context.getServletResponse(), rootWindow);
                aggregateAndProcessRender(context.getServletRequest(), context.getServletResponse(), rootWindow);
            }
        }
        
        // continue
        context.invokeNext();
    }

    protected void aggregateAndProcessBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse, HstComponentWindow window) throws ContainerException {
        
        HstComponentInvoker invoker = getComponentInvoker();
        invoker.invokeBeforeRender(servletRequest, servletResponse);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();
        
        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                aggregateAndProcessBeforeRender(servletRequest, servletResponse, entry.getValue());
            }
        }
    }
    
    protected void aggregateAndProcessRender(ServletRequest servletRequest, ServletResponse servletResponse, HstComponentWindow window) throws ContainerException {
        
        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();
        
        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                aggregateAndProcessRender(servletRequest, servletResponse, entry.getValue());
            }
        }
        
        HstComponentInvoker invoker = getComponentInvoker();
        invoker.invokeRender(servletRequest, servletResponse);
        
    }
}
