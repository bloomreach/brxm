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
import java.util.Map.Entry;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.component.HstServletResponseState;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.w3c.dom.Comment;

/**
 * AggregationValve
 * 
 * @version $Id$
 */
public class AggregationValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {

        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();
        
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
                
                HstComponentWindow [] sortedComponentWindows = sortedComponentWindowList.toArray(new HstComponentWindow[sortedComponentWindowList.size()]);
                
                HstContainerConfig requestContainerConfig = context.getRequestContainerConfig();
                // process doBeforeRender() of each component as sorted order, parent first.
                processWindowsBeforeRender(requestContainerConfig, rootWindow, sortedComponentWindows, requestMap, responseMap);
                
                String redirectLocation = null;
                if (!requestContext.isPortletContext()) {
                    for (HstComponentWindow window : sortedComponentWindows) {
                        if (window.getResponseState().getRedirectLocation() != null) {
                            redirectLocation = window.getResponseState().getRedirectLocation();
                            break;
                        }
                    }
                }
                
                // check if it's requested to forward.
                String forwardPathInfo = rootWindow.getResponseState().getForwardPathInfo();
                
                // page error handling...
                PageErrors pageErrors = getPageErrors(sortedComponentWindows, true);
                if (pageErrors != null) {
                    PageErrorHandler.Status handled = handleComponentExceptions(pageErrors, requestContainerConfig, rootWindow, requestMap.get(rootWindow), responseMap.get(rootWindow));
                    
                    // page error handler should be able to override redirect location or forward path info.
                    if (rootWindow.getResponseState().getRedirectLocation() != null) {
                        redirectLocation = rootWindow.getResponseState().getRedirectLocation();
                    }
                    if (rootWindow.getResponseState().getForwardPathInfo() != null) {
                        forwardPathInfo = rootWindow.getResponseState().getForwardPathInfo();
                    }
                    
                    if (handled == PageErrorHandler.Status.HANDLED_TO_STOP && redirectLocation == null && forwardPathInfo == null) {
                        context.invokeNext();
                        return;
                    }
                }
                
                // check if any invocation on sendError() exists...
                HstComponentWindow errorCodeSendingWindow = findErrorCodeSendingWindow(sortedComponentWindows);
                
                if (errorCodeSendingWindow != null) {
                    
                    try {
                        int errorCode = errorCodeSendingWindow.getResponseState().getErrorCode();
                        String errorMessage = errorCodeSendingWindow.getResponseState().getErrorMessage();
                        String componentClassName = errorCodeSendingWindow.getComponentName();
                        
                        if (log.isDebugEnabled()) {
                            log.debug("The component window has error status code, {} from {}.", errorCode, componentClassName);
                        }
                        
                        if (errorMessage != null) {
                            servletResponse.sendError(errorCode, errorMessage);
                        } else {
                            servletResponse.sendError(errorCode);
                        }
                    } catch (IOException e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Exception invocation on sendError().", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Exception invocation on sendError(). {}", e.toString());
                        }
                    }
                
                } else if (redirectLocation != null) {
                    
                    try {
                        for (int i = sortedComponentWindows.length - 1; i >= 0; i--) {
                            HstComponentWindow window = sortedComponentWindows[i];
                            window.getResponseState().flush();
                        }
                        
                        if (redirectLocation.startsWith("http:") || redirectLocation.startsWith("https:")) {
                            servletResponse.sendRedirect(redirectLocation);
                        } else {
                            if (!redirectLocation.startsWith("/")) {
                                throw new ContainerException("Can only redirect to a context relative path starting with a '/'.");
                            }
                            
                            /* 
                             * We will redirect to a URL containing the scheme + hostname + portnumber to avoid problems
                             * when redirecting behind a proxy by default.
                             */
                            if (isAlwaysRedirectLocationToAbsoluteUrl()) {
                                String absoluteRedirectUrl = requestContext.getVirtualHost().getBaseURL(servletRequest) + redirectLocation;
                                servletResponse.sendRedirect(absoluteRedirectUrl);
                            } else {
                                servletResponse.sendRedirect(redirectLocation);
                            }
                        }
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Exception during sendRedirect.", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Exception during sendRedirect. {}", e.toString());
                        }
                    }
                    
                } else if (forwardPathInfo != null) {
                    
                    servletRequest.setAttribute(ContainerConstants.HST_FORWARD_PATH_INFO, forwardPathInfo);
                    
                } else {
                    
                    // process doRender() of each component as reversed sort order, child first.
                    processWindowsRender(requestContainerConfig, sortedComponentWindows, requestMap, responseMap);
                    
                    // page error handling...
                    pageErrors = getPageErrors(sortedComponentWindows, true);
                    if (pageErrors != null) {
                        PageErrorHandler.Status handled = handleComponentExceptions(pageErrors, requestContainerConfig, rootWindow, requestMap.get(rootWindow), responseMap.get(rootWindow));
                        if (handled == PageErrorHandler.Status.HANDLED_TO_STOP) {
                            // just ignore because we don't support forward or redirect during rendering...
                        }
                    }
                    
                    try {
                        // add the X-HST-VERSION as a response header if we are in preview:
                        if (requestContext.isPreview() && requestContext.getResolvedMount().getMount().isVersionInPreviewHeader()) {
                            rootWindow.getResponseState().addHeader("X-HST-VERSION", HstServices.getImplementationVersion());
                        }
                        // flush root component window content.
                        // note that the child component's contents are already flushed into the root component's response state.
                        rootWindow.getResponseState().flush();
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Exception during flushing the response state.", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Exception during flushing the response state. {}", e.toString());
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
            final HstComponentWindow rootWindow,
            final HstComponentWindow [] sortedComponentWindows,
            final Map<HstComponentWindow, HstRequest> requestMap, 
            final Map<HstComponentWindow, HstResponse> responseMap)
            throws ContainerException {

        for (int i = 0; i < sortedComponentWindows.length; i++) {
            HstComponentWindow window = sortedComponentWindows[i];
            HstRequest request = requestMap.get(window);
            HstResponse response = responseMap.get(window);
            getComponentInvoker().invokeBeforeRender(requestContainerConfig, request, response);
            
            if (window.getResponseState().getRedirectLocation() != null) {
                break;
            }
            
            if (rootWindow.getResponseState().getForwardPathInfo() != null) {
                break;
            }
            
            HttpSession session = request.getSession(false);
            
            if(session != null ) {
                Boolean composerMode = (Boolean) session.getAttribute(ContainerConstants.COMPOSER_MODE_ATTR_NAME);
                if (composerMode != null) {
                    Mount mount = request.getRequestContext().getResolvedMount().getMount();
                    // we are in render host mode. Add the wrapper elements that are needed for the composer around all components
                    HstComponentConfiguration compConfig  = ((HstComponentConfiguration)window.getComponentInfo());
                    if (window == rootWindow) {
                        rootWindow.getResponseState().addHeader("HST-Mount-Id", mount.getIdentifier());
                        rootWindow.getResponseState().addHeader("HST-Site-Id", mount.getHstSite().getCanonicalIdentifier());
                        rootWindow.getResponseState().addHeader("HST-Page-Id", compConfig.getCanonicalIdentifier());
                        boolean isPreviewConfig = false;
                        if(mount.getHstSite().getConfigurationPath().endsWith("-"+Mount.PREVIEW_NAME)) {
                            isPreviewConfig = true;
                        }
                        rootWindow.getResponseState().addHeader("HST-Site-HasPreviewConfig", String.valueOf(isPreviewConfig));
                        //"-" + Mount.PREVIEW_NAME;
                    } else if(Boolean.TRUE.equals(composerMode)) {
                         // TODO replace by json marshaller
                        
                        HashMap<String, String> attributes = new HashMap<String, String>();
                        attributes.put("uuid", compConfig.getCanonicalIdentifier());
                        if(compConfig.getXType() != null) {
                            attributes.put("xtype", compConfig.getXType());
                        }
                        if(compConfig.isInherited()) {
                            attributes.put("inherited", "true"); 
                        }
                        attributes.put("type", compConfig.getComponentType().toString());
                        Comment comment = createCommentWithAttr(attributes, response);
                        response.addPreamble(comment);
                    }
                } 
            }
        }
    }
    
    private Comment createCommentWithAttr(HashMap<String, String> attributes, HstResponse response) {
        StringBuilder builder = new StringBuilder();
        for(Entry<String, String> attr : attributes.entrySet()) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append("\"").append(attr.getKey()).append("\":").append("\"").append(attr.getValue()).append("\"");
        }
        Comment comment = response.createComment("{ " + builder.toString() +"}");
        return comment;
    }

    protected void processWindowsRender(final HstContainerConfig requestContainerConfig,
            final HstComponentWindow[] sortedComponentWindows, final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {

        for (int i = sortedComponentWindows.length - 1; i >= 0; i--) {
            HstComponentWindow window = sortedComponentWindows[i];
            HstRequest request = requestMap.get(window);
            HstResponse response = responseMap.get(window);
            getComponentInvoker().invokeRender(requestContainerConfig, request, response);
        }
    }
    
}
