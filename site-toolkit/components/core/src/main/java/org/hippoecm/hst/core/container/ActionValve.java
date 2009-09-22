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
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.component.HstServletResponseState;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ActionValve extends AbstractValve
{

    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        String actionWindowReferenceNamespace = requestContext.getBaseURL().getActionWindowReferenceNamespace();
        if (actionWindowReferenceNamespace != null) {
            HstContainerURL baseURL = requestContext.getBaseURL();
            HstComponentWindow window = null;
            window = findActionWindow(context.getRootComponentWindow(), actionWindowReferenceNamespace);
            
            HstResponseState responseState = null;
            HstContainerURLProvider urlProvider = requestContext.getURLFactory().getContainerURLProvider(requestContext);
            
            if (window == null) {
                if (log.isWarnEnabled()) {
                    log.warn("Cannot find the action window: {}", actionWindowReferenceNamespace);
                }
            } else {
                // Check if it is invoked from portlet.
                responseState = (HstResponseState) servletRequest.getAttribute(HstResponseState.class.getName());
                
                if (responseState == null) {
                    responseState = new HstServletResponseState(servletRequest, servletResponse);
                }
                
                HstRequest request = new HstRequestImpl(servletRequest, requestContext, window, HstRequest.ACTION_PHASE);
                HstResponseImpl response = new HstResponseImpl(servletRequest, servletResponse, requestContext, window, responseState, null);
                ((HstComponentWindowImpl) window).setResponseState(responseState);

                getComponentInvoker().invokeAction(context.getRequestContainerConfig(), request, response);
                
                Map<String, String []> renderParameters = response.getRenderParameters();
                response.setRenderParameters(null);
                
                if (renderParameters == null) {
                    renderParameters = Collections.emptyMap();
                }
                String referenceNamespace = window.getReferenceNamespace();
                if (getUrlFactory().isReferenceNamespaceIgnored()) {
                    referenceNamespace = "";
                }
                urlProvider.mergeParameters(baseURL, referenceNamespace, renderParameters);
                
                if (window.hasComponentExceptions() && log.isWarnEnabled()) {
                    for (HstComponentException hce : window.getComponentExceptions()) {
                        if (log.isDebugEnabled()) {
                            log.warn("Component exception found: {}", hce.toString(), hce);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Component exception found: {}", hce.toString());
                        }
                    }
                    
                    window.clearComponentExceptions();
                }
                if (responseState.getErrorCode() > 0) {
                    
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("The action window has error status code: {} - {}", Integer.valueOf(responseState.getErrorCode()), window.getName());
                        }
                        servletResponse.sendError(responseState.getErrorCode(), responseState.getErrorMessage());
                    } catch (IOException e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Exception invocation on sendError().", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Exception invocation on sendError().");
                        }
                    }
                    
                } else {
                
                    if (responseState.getRedirectLocation() == null) {
                        try {
                            // Clear action state first
                            if (baseURL.getActionParameterMap() != null) {
                                baseURL.getActionParameterMap().clear();
                            }
                            baseURL.setActionWindowReferenceNamespace(null);
                            
                            if (requestContext.isPortletContext()) {
                                responseState.sendRedirect(urlProvider.toContextRelativeURLString(baseURL, requestContext));
                            } else {
                                responseState.sendRedirect(urlProvider.toURLString(baseURL, requestContext, servletRequest.getContextPath()));
                            }
                        } catch (UnsupportedEncodingException e) {
                            throw new ContainerException(e);
                        } catch (IOException e) {
                            throw new ContainerException(e);
                        }
                    }
                    
                    try {
                        if (!requestContext.isPortletContext()) {
                            responseState.flush();
                            servletResponse.sendRedirect(responseState.getRedirectLocation());
                        }
                    } catch (IOException e) {
                        log.warn("Unexpected exception during redirect to " + responseState.getRedirectLocation(), e);
                    }
                }
            }
            
        } else {
            // continue
            context.invokeNext();
        }
    }
    
    protected HstComponentWindow findActionWindow(HstComponentWindow rootWindow, String actionWindowReferenceNamespace) {
        HstComponentWindow actionWindow = null;
        
        String rootReferenceNamespace = rootWindow.getReferenceNamespace();
        
        if (rootReferenceNamespace.equals(actionWindowReferenceNamespace)) {
            actionWindow = rootWindow;
        } else {
            String [] rootReferenceNamespaces = rootReferenceNamespace.split(getComponentWindowFactory().getReferenceNameSeparator());
            String [] referenceNamespaces = actionWindowReferenceNamespace.split(getComponentWindowFactory().getReferenceNameSeparator());
            int index = 0;
            while (index < rootReferenceNamespaces.length && index < referenceNamespaces.length && rootReferenceNamespaces[index].equals(referenceNamespaces[index])) {
                index++;
            }
            
            if (index < referenceNamespaces.length) {
                HstComponentWindow tempWindow = rootWindow;
                for ( ; index < referenceNamespaces.length; index++) {
                    if (tempWindow != null) {
                        tempWindow = tempWindow.getChildWindowByReferenceName(referenceNamespaces[index]);
                    } else {
                        break;
                    }
                }
                
                if (index == referenceNamespaces.length) {
                    actionWindow = tempWindow;
                }
            }
        }
        
        return actionWindow;
    }
}
