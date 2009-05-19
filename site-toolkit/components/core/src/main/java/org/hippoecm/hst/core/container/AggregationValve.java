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

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.hippoecm.hst.core.component.HstServletResponseState;
import org.hippoecm.hst.core.request.HstRequestContext;

public class AggregationValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {

        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) context.getServletResponse();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(HstRequestContext.class.getName());
        boolean isDevelopmentMode = getContainerConfiguration().isDevelopmentMode();
        
        if (!context.getServletResponse().isCommitted() && requestContext.getBaseURL().getResourceWindowReferenceNamespace() == null) {
            HstComponentWindow rootWindow = context.getRootComponentWindow();

            if (rootWindow != null) {
                Map<HstComponentWindow, HstRequest> requestMap = new HashMap<HstComponentWindow, HstRequest>();
                Map<HstComponentWindow, HstResponse> responseMap = new HashMap<HstComponentWindow, HstResponse>();

                ServletRequest parentRequest = servletRequest;
                ServletResponse parentResponse = servletResponse;
                
                HstResponse topParentResponse = null;
                
                // Check if it is invoked from portlet.
                HstResponseState portletHstResponseState = (HstResponseState) servletRequest.getAttribute(HstResponseState.class.getName());
                
                if (portletHstResponseState != null) {
                    parentResponse = new HstResponseImpl((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, requestContext, null, portletHstResponseState, null);
                    topParentResponse = (HstResponse) parentResponse;
                }
                
                // make hstRequest and hstResponse for each component window.
                // note that hstResponse is hierarchically created.
                createHstRequestResponseForWindows(rootWindow, requestContext, parentRequest, parentResponse,
                        requestMap, responseMap, topParentResponse);
                
                // to avoid recursive invocation from now, just make a list by hierarchical order.
                List<HstComponentWindow> sortedComponentWindowList = new LinkedList<HstComponentWindow>();
                sortComponentWindowsByHierarchy(rootWindow, sortedComponentWindowList);
                
                // create and add a window for trace tool
                HstComponentWindow traceToolWindow = null;
                if (isDevelopmentMode && !sortedComponentWindowList.isEmpty()) {
                    HstComponentWindow lastChildWindow = sortedComponentWindowList.get(sortedComponentWindowList.size() - 1);
                    traceToolWindow = createTraceToolComponent(context, requestContext, lastChildWindow);
                    ((HstComponentWindowImpl) lastChildWindow).addChildWindow(traceToolWindow);
                    createHstRequestResponseForWindows(traceToolWindow, requestContext, 
                            requestMap.get(lastChildWindow), responseMap.get(lastChildWindow), 
                            requestMap, responseMap, topParentResponse);
                    sortedComponentWindowList.add(traceToolWindow);
                }
                
                HstComponentWindow [] sortedComponentWindows = sortedComponentWindowList.toArray(new HstComponentWindow[0]);
                
                HstContainerConfig requestContainerConfig = context.getRequestContainerConfig();
                // process doBeforeRender() of each component as sorted order, parent first.
                processWindowsBeforeRender(requestContainerConfig, sortedComponentWindows, requestMap, responseMap);
                
                // check if any invocation on sendError() exists...
                int errorCode = 0;
                String errorMessage = null;
                String errorWindowName = null;
                
                for (HstComponentWindow window : sortedComponentWindows) {
                    HstResponseState responseStateForWindow = ((HstComponentWindowImpl) window).getResponseState();
                    if (responseStateForWindow.getErrorCode() > 0) {
                        errorCode = responseStateForWindow.getErrorCode();
                        errorMessage = responseStateForWindow.getErrorMessage();
                        errorWindowName = window.getName(); 
                        break;
                    }
                }
                
                if (errorCode > 0) {
                    
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("The component window has error status code: {} - {}", errorCode, errorWindowName);
                        }
                        servletResponse.sendError(errorCode, errorMessage);
                        
                        if (log.isWarnEnabled()) {
                            // log warnings of each component execution as reversed sort order, child first.
                            logWarningsForEachComponentWindow(sortedComponentWindows);
                        }
                    } catch (IOException e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Exception invocation on sendError().", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Exception invocation on sendError().");
                        }
                    }
                    
                } else {

                    // process doRender() of each component as reversed sort order, child first.
                    processWindowsRender(requestContainerConfig, sortedComponentWindows, requestMap, responseMap, isDevelopmentMode, traceToolWindow);
    
                    if (log.isWarnEnabled()) {
                        // log warnings of each component execution as reversed sort order, child first.
                        logWarningsForEachComponentWindow(sortedComponentWindows);
                    }
                    
                    try {
                        // flush root component window content.
                        // note that the child component's contents are already flushed into the root component's response state.
                        rootWindow.getResponseState().flush();
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Exception during flushing the response state.", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Exception during flushing the response state.");
                        }
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
            final Map<HstComponentWindow, HstResponse> responseMap,
            HstResponse topComponentHstResponse) {

        HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window, HstRequest.RENDER_PHASE);
        HstResponseState responseState = new HstServletResponseState((HttpServletRequest) servletRequest,
                (HttpServletResponse) servletResponse);
        HstResponse response = new HstResponseImpl((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, requestContext, window,
                responseState, topComponentHstResponse);
        
        if (topComponentHstResponse == null) {
            topComponentHstResponse = response;
        }

        requestMap.put(window, request);
        responseMap.put(window, response);

        ((HstComponentWindowImpl) window).setResponseState(responseState);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();

        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                createHstRequestResponseForWindows(entry.getValue(), requestContext, servletRequest, response,
                        requestMap, responseMap, topComponentHstResponse);
            }
        }
    }

    protected void sortComponentWindowsByHierarchy(
            final HstComponentWindow window, 
            final List<HstComponentWindow> sortedWindowList) {
        
        sortedWindowList.add(window);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();

        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                sortComponentWindowsByHierarchy(entry.getValue(), sortedWindowList);
            }
        }
    }

    protected void processWindowsBeforeRender(
            final HstContainerConfig requestContainerConfig, 
            final HstComponentWindow [] sortedComponentWindows,
            final Map<HstComponentWindow, HstRequest> requestMap, 
            final Map<HstComponentWindow, HstResponse> responseMap)
            throws ContainerException {

        for (int i = 0; i < sortedComponentWindows.length; i++) {
            HstComponentWindow window = sortedComponentWindows[i];
            HstRequest request = requestMap.get(window);
            HstResponse response = responseMap.get(window);
            getComponentInvoker().invokeBeforeRender(requestContainerConfig, request, response);
        }

    }

    protected void processWindowsRender(
            final HstContainerConfig requestContainerConfig, 
            final HstComponentWindow [] sortedComponentWindows,
            final Map<HstComponentWindow, HstRequest> requestMap, 
            final Map<HstComponentWindow, HstResponse> responseMap,
            final boolean isDevelopmentMode,
            final HstComponentWindow traceToolWindow)
            throws ContainerException {

        boolean traceToolWindowFlushed = false;

        for (int i = sortedComponentWindows.length - 1; i >= 0; i--) {
            HstComponentWindow window = sortedComponentWindows[i];
            HstRequest request = requestMap.get(window);
            HstResponse response = responseMap.get(window);
            getComponentInvoker().invokeRender(requestContainerConfig, request, response);
            
            if (isDevelopmentMode && !traceToolWindowFlushed && window != traceToolWindow) {
                try {
                    traceToolWindowFlushed = true;
                    traceToolWindow.getResponseState().flush();
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Exception during flushing the traceToolWindow's response state.", e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Exception during flushing the traceToolWindow's response state.");
                    }                    
                }
            }
        }
    }

    protected void logWarningsForEachComponentWindow(HstComponentWindow [] sortedComponentWindows) {

        for (int i = sortedComponentWindows.length - 1; i >= 0; i--) {
            HstComponentWindow window = sortedComponentWindows[i];
            
            if (window.hasComponentExceptions()) {
                for (HstComponentException hce : window.getComponentExceptions()) {
                    if (log.isDebugEnabled()) {
                        log.warn("Component exception found: {}", hce.getMessage(), hce);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Component exception found: {}", hce.getMessage());
                    }
                }

                window.clearComponentExceptions();
            }
        }
    }
    
}
