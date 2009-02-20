/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.container;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstComponentException;
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
                Map<HstComponentWindow, HstResponseState> responseStateMap = new HashMap<HstComponentWindow, HstResponseState>();
                Map<HstComponentWindow, HstResponse> responseMap = new HashMap<HstComponentWindow, HstResponse>();
                
                createHstRequestResponseForWindows(rootWindow, requestContext, servletRequest, servletResponse, requestMap, responseStateMap, responseMap);
                
                aggregateAndProcessBeforeRender(rootWindow, requestMap, responseMap, context.getServletConfig());
                aggregateAndProcessRender(rootWindow, requestMap, responseMap, context.getServletConfig());
                
                if (log.isWarnEnabled()) {
                    logWarningsForEachComponentWindow(rootWindow);
                }
                
                try {
                    rootWindow.flushContent();
                } catch (Exception e) {
                    if (log.isWarnEnabled()) {
                        log.warn("Exception during flushing the response state.", e);
                    }
                }
            }
        }
        
        // continue
        context.invokeNext();
    }
    
    protected void createHstRequestResponseForWindows(
            final HstComponentWindow window, 
            final HstRequestContext requestContext, 
            final ServletRequest servletRequest, 
            final ServletResponse servletResponse, 
            final Map<HstComponentWindow, HstRequest> requestMap, 
            final Map<HstComponentWindow, HstResponseState> responseStateMap, 
            final Map<HstComponentWindow, HstResponse> responseMap) {
        
        HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window, getUrlFactory().getParameterNameComponentSeparator());
        HstResponseState responseState = new HstResponseState((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
        HstResponse response = new HstResponseImpl((HttpServletResponse) servletResponse, requestContext, window, responseState);

        requestMap.put(window, request);
        responseStateMap.put(window, responseState);
        responseMap.put(window, response);

        ((HstComponentWindowImpl) window).setResponseState(responseState);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();
        
        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                createHstRequestResponseForWindows(entry.getValue(), requestContext, servletRequest, response, requestMap, responseStateMap, responseMap);
            }
        }
    }

    protected void aggregateAndProcessBeforeRender(
            final HstComponentWindow window, 
            final Map<HstComponentWindow, HstRequest> requestMap, 
            final Map<HstComponentWindow, HstResponse> responseMap,
            final ServletConfig servletConfig) throws ContainerException {
        
        final HstRequest request = requestMap.get(window);
        final HstResponse response = responseMap.get(window);
        
        getComponentInvoker().invokeBeforeRender(servletConfig, request, response);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();
        
        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                aggregateAndProcessBeforeRender(entry.getValue(), requestMap, responseMap, servletConfig);
            }
        }
        
    }
    
    protected void aggregateAndProcessRender(
            final HstComponentWindow window, 
            final Map<HstComponentWindow, HstRequest> requestMap, 
            final Map<HstComponentWindow, HstResponse> responseMap,
            final ServletConfig servletConfig) throws ContainerException {
        
        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();
        
        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                aggregateAndProcessRender(entry.getValue(), requestMap, responseMap, servletConfig);
            }
        }
        
        final HstRequest request = requestMap.get(window);
        final HstResponse response = responseMap.get(window);
        
        getComponentInvoker().invokeRender(servletConfig, request, response);
    }
    
    protected void logWarningsForEachComponentWindow(HstComponentWindow window) {

        if (window.hasComponentExceptions()) {
            for (HstComponentException hce : window.getComponentExceptions()) {
                log.warn("Component exception found: " + hce.getMessage(), hce);
            }
            
            window.clearComponentExceptions();
        }
        
        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();
        
        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                logWarningsForEachComponentWindow(entry.getValue());
            }
        }
        
    }

}
