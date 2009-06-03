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
package org.hippoecm.hst.component.support;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;

/**
 * A bridge component which simply delegates all invocation to the dispatch url.
 * So, the dispatched servlet should do every necessary things.
 * <P>
 * The dispatched servlet can detect which lifecycle phase of the current request is by retrieving request attribute.
 * <UL>
 * <LI>If the request attribute value by {@link #LIFECYCLE_PHASE_ATTRIBUTE} is {@link #BEFORE_RENDER_PHASE},
 * then the current request is dispatched via {@link org.hippoecm.hst.core.component.HstComponent#doBeforeRender(HstRequest, HstResponse)}.</LI>
 * <LI>else if the request attribute value by {@link #LIFECYCLE_PHASE_ATTRIBUTE} is {@link #BEFORE_RESOURCE_PHASE},
 * then the current request is dispatched via {@link org.hippoecm.hst.core.component.HstComponent#doBeforeServeResource(HstRequest, HstResponse)}.</LI>
 * <LI>else if the {@link HstRequest#getLifecyclePhase()} returns {@link HstRequest#ACTION_PHASE},
 * then the current request is dispatched via {@link org.hippoecm.hst.core.component.HstComponent#doAction(HstRequest, HstResponse)}.</LI>
 * <LI>else if the {@link HstRequest#getLifecyclePhase()} returns {@link HstRequest#RENDER_PHASE},
 * then the current request is dispatched via <CODE>renderpath</CODE>.</LI>
 * <LI>else if the {@link HstRequest#getLifecyclePhase()} returns {@link HstRequest#RESOURCE_PHASE},
 * then the current request is dispatched via <CODE>serveresourcepath</CODE>.</LI>
 * </UL>
 * </P>
 * 
 * @version $Id$
 */
public class SimpleDispatcherHstComponent extends GenericHstComponent {
    
    public static final String LIFECYCLE_PHASE_ATTRIBUTE = SimpleDispatcherHstComponent.class.getName() + ".lifecycle.phase";
    
    public static final String BEFORE_RENDER_PHASE = "BEFORE_RENDER_PHASE";
    public static final String BEFORE_RESOURCE_PHASE = "BEFORE_RESOURCE_PHASE";
    
    protected String dispatchUrlParamName = "dispatch-url";
    
    @Override
    public void init(ServletConfig servletConfig, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletConfig, componentConfig);
        
        String param = servletConfig.getInitParameter("dispatch-url-param-name");
        
        if (param != null) {
            dispatchUrlParamName = param;
        }
    }
    
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        String dispatchUrl = getDispatchUrlParameter(request);
        doDispatch(dispatchUrl, request, response);
    }
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        String dispatchUrl = getDispatchUrlParameter(request);
        
        try {
            request.setAttribute(LIFECYCLE_PHASE_ATTRIBUTE, BEFORE_RENDER_PHASE);
            response.setRenderPath(dispatchUrl);
            doDispatch(dispatchUrl, request, response);
        } finally {
            request.removeAttribute(LIFECYCLE_PHASE_ATTRIBUTE);
        }
    }
    
    @Override
    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        String dispatchUrl = getDispatchUrlParameter(request);
        
        try {
            request.setAttribute(LIFECYCLE_PHASE_ATTRIBUTE, BEFORE_RESOURCE_PHASE);
            response.setServeResourcePath(dispatchUrl);
            doDispatch(dispatchUrl, request, response);
        } finally {
            request.removeAttribute(LIFECYCLE_PHASE_ATTRIBUTE);
        }
    }
    
    protected void doDispatch(String dispatchUrl, HstRequest request, HstResponse response) throws HstComponentException {
        if (dispatchUrl == null) {
            throw new HstComponentException("The dispatch url is null.");
        }
        
        try {
            getServletConfig().getServletContext().getRequestDispatcher(dispatchUrl).include(request, response);
        } catch (ServletException e) {
            throw new HstComponentException(e);
        } catch (IOException e) {
            throw new HstComponentException(e);
        }
    }
    
    protected String getDispatchUrlParameter(HstRequest request) {
        String dispatchUrl = getParameter(dispatchUrlParamName, request);
        
        if (dispatchUrl != null) {
            if (dispatchUrl.charAt(0) != '/') {
                dispatchUrl = new StringBuilder(dispatchUrl.length() + 1).append('/').append(dispatchUrl).toString();
            }
        }
        
        return dispatchUrl;
    }
    
    protected String getParameter(String name, HstRequest request) {
        return (String) this.getComponentConfiguration().getParameter(name, request.getRequestContext().getResolvedSiteMapItem());
    }
    
}
