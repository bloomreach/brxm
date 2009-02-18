package org.hippoecm.hst.core.container;

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.request.HstRequestContext;

public class AggregationValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        
        if (!context.getServletResponse().isCommitted() && !isResourceRequest()) {

            HstComponentWindow rootWindow = context.getRootComponentWindow();
            
            if (rootWindow != null) {
                ServletRequest servletRequest = context.getServletRequest();
                HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(HstRequestContext.class.getName());
                
                aggregateAndProcessBeforeRender(requestContext, servletRequest, context.getServletResponse(), rootWindow);
                aggregateAndProcessRender(requestContext, context.getServletRequest(), context.getServletResponse(), rootWindow);
            }
        }
        
        // continue
        context.invokeNext();
    }

    protected void aggregateAndProcessBeforeRender(HstRequestContext requestContext, ServletRequest servletRequest, ServletResponse servletResponse, HstComponentWindow window) throws ContainerException {
        
        HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window);
        HstResponse response = new HstResponseImpl((HttpServletResponse) servletResponse, requestContext, window);
        
        getComponentInvoker().invokeBeforeRender(request, response);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();
        
        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                aggregateAndProcessBeforeRender(requestContext, servletRequest, servletResponse, entry.getValue());
            }
        }
        
    }
    
    protected void aggregateAndProcessRender(HstRequestContext requestContext, ServletRequest servletRequest, ServletResponse servletResponse, HstComponentWindow window) throws ContainerException {
        
        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();
        
        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                aggregateAndProcessRender(requestContext, servletRequest, servletResponse, entry.getValue());
            }
        }
        
        HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window);
        HstResponse response = new HstResponseImpl((HttpServletResponse) servletResponse, requestContext, window);
        
        getComponentInvoker().invokeRender(request, response);
        
    }
}
