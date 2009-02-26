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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ActionValve extends AbstractValve
{

    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) context.getServletResponse();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(HstRequestContext.class.getName());
        
        if (requestContext.getBaseURL().getActionWindowReferenceNamespace() != null) {
            HstContainerURL baseURL = requestContext.getBaseURL();
            String redirectLocation = null;
            HstComponentWindow window = findActionWindow(context.getRootComponentWindow(), baseURL.getActionWindowReferenceNamespace());
            
            if (window == null) {
                log.warn("Cannot find the action window: " + requestContext.getBaseURL().getActionWindowReferenceNamespace());
            } else {
                HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window);
                HstResponseState responseState = new HstResponseState((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
                HstResponseImpl response = new HstResponseImpl((HttpServletResponse) servletResponse, requestContext, window, responseState);
                ((HstComponentWindowImpl) window).setResponseState(responseState);

                getComponentInvoker().invokeAction(context.getServletConfig(), request, response);
                
                redirectLocation = responseState.getRedirectLocation();
                
                Map<String, String []> renderParameters = response.getRenderParamerters();
                
                if (renderParameters != null) {
                    getUrlFactory().getUrlProvider().mergeParameters(baseURL, window.getReferenceNamespace(), renderParameters);
                    response.setRenderParameters(null);
                }
                
                if (window.hasComponentExceptions() && log.isWarnEnabled()) {
                    for (HstComponentException hce : window.getComponentExceptions()) {
                        log.warn("Component exception found: " + hce.getMessage(), hce);
                    }
                    
                    window.clearComponentExceptions();
                }
            }
            
            if (baseURL.getActionParameterMap() != null) {
                baseURL.getActionParameterMap().clear();
            }
            
            baseURL.setActionWindowReferenceNamespace(null);
            
            if (redirectLocation == null) {
                redirectLocation = getUrlFactory().getUrlProvider().toURLString(baseURL);
            }
            
            try {
                servletResponse.sendRedirect(redirectLocation);
            } catch (IOException e) {
                log.warn("Unexpected exception during redirect to " + redirectLocation, e);
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
            String [] referenceNamespaces = actionWindowReferenceNamespace.split(getComponentWindowFactory().getReferenceNameSeparator());
            int start = ((referenceNamespaces.length > 0 && rootReferenceNamespace.equals(referenceNamespaces[0])) ? 1 : 0);
            
            HstComponentWindow tempWindow = rootWindow;
            int index = start;
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
        
        return actionWindow;
    }
}
