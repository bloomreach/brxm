package org.hippoecm.hst.core.container;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.request.HstRequestContext;

public class AggregationValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        
        if (!context.getServletResponse().isCommitted() && !isResourceRequest()) {

            HstComponentWindow rootWindow = context.getRootComponentWindow();
            
            if (rootWindow != null) {
                ServletRequest servletRequest = context.getServletRequest();
                ServletResponse servletResponse = context.getServletResponse();
                HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(HstRequestContext.class.getName());
                
                Map<HstComponentWindow, HstRequest> requestMap = new HashMap<HstComponentWindow, HstRequest>();
                Map<HstComponentWindow, HstResponse> responseMap = new HashMap<HstComponentWindow, HstResponse>();
                
                createHstRequestResponseForWindows(rootWindow, requestContext, servletRequest, servletResponse, requestMap, responseMap);
                
                aggregateAndProcessBeforeRender(rootWindow, requestMap, responseMap);
                aggregateAndProcessRender(rootWindow, requestMap, responseMap);
            }
        }
        
        // continue
        context.invokeNext();
    }
    
    protected void createHstRequestResponseForWindows(final HstComponentWindow window, final HstRequestContext requestContext, final ServletRequest servletRequest, final ServletResponse servletResponse, final Map<HstComponentWindow, HstRequest> requestMap, final Map<HstComponentWindow, HstResponse> responseMap) {
        HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window);
        HstResponseState responseState = new HstResponseState((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
        HstResponse response = new HstResponseImpl((HttpServletResponse) servletResponse, requestContext, window, responseState);

        requestMap.put(window, request);
        responseMap.put(window, response);
        
        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();
        
        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                createHstRequestResponseForWindows(entry.getValue(), requestContext, servletRequest, servletResponse, requestMap, responseMap);
            }
        }        
    }

    protected void aggregateAndProcessBeforeRender(final HstComponentWindow window, final Map<HstComponentWindow, HstRequest> requestMap, final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {
        
        final HstRequest request = requestMap.get(window);
        final HstResponse response = responseMap.get(window);
        
        getComponentInvoker().invokeBeforeRender(request, response);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();
        
        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                aggregateAndProcessBeforeRender(entry.getValue(), requestMap, responseMap);
            }
        }
        
    }
    
    protected void aggregateAndProcessRender(final HstComponentWindow window, final Map<HstComponentWindow, HstRequest> requestMap, final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {
        
        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();
        
        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                aggregateAndProcessRender(entry.getValue(), requestMap, responseMap);
            }
        }
        
        final HstRequest request = requestMap.get(window);
        final HstResponse response = responseMap.get(window);
        
        getComponentInvoker().invokeRender(request, response);
        
    }
}
