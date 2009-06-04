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

import javax.servlet.ServletException;

import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

/**
 * A bridge component which simply delegates all invocation to the dispatch path.
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
    
    /**
     * Default dispatch path for every invocation. If you set this parameter in the configuration,
     * the servlet indicated by this path should handle everything by itself.
     */
    public static final String DISPATCH_PATH_PARAM_NAME = "dispatch-path";
    
    /**
     * The dispatch path for <CODE>BEFORE_RENDER_PHASE</CODE>.
     * This component would dispatch to this path in its {@link #doBeforeRender(HstRequest, HstResponse)}.
     */
    public static final String BEFORE_RENDER_PATH_PARAM_NAME = "before-render-path";

    /**
     * The dispatch path for <CODE>RENDER_PHASE</CODE>.
     * This component would dispatch to this path in its {@link #doRender(HstRequest, HstResponse)}.
     */
    public static final String RENDER_PATH_PARAM_NAME = "render-path";
    
    /**
     * The dispatch path for <CODE>ACTION_PHASE</CODE>.
     * This component would dispatch to this path in its {@link #doAction(HstRequest, HstResponse)}.
     */
    public static final String ACTION_PATH_PARAM_NAME = "action-path";
    
    /**
     * The dispatch path for <CODE>BEFORE_RESOURCE_PHASE</CODE>.
     * This component would dispatch to this path in its {@link #doBeforeResource(HstRequest, HstResponse)}.
     */
    public static final String BEFORE_RESOURCE_PATH_PARAM_NAME = "before-resource-path";
    
    /**
     * The dispatch path for <CODE>RESOURCE_PHASE</CODE>.
     * This component would dispatch to this path in its {@link #doServeResource(HstRequest, HstResponse)}.
     */
    public static final String RESOURCE_PATH_PARAM_NAME = "resource-path";
    
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        doDispatch(getDispatchPathParameter(request, request.getLifecyclePhase()), request, response);
    }
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        try {
            request.setAttribute(LIFECYCLE_PHASE_ATTRIBUTE, BEFORE_RENDER_PHASE);
            response.setRenderPath(getDispatchPathParameter(request, request.getLifecyclePhase()));
            doDispatch(getDispatchPathParameter(request, BEFORE_RENDER_PHASE), request, response);
        } finally {
            request.removeAttribute(LIFECYCLE_PHASE_ATTRIBUTE);
        }
    }
    
    @Override
    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        try {
            request.setAttribute(LIFECYCLE_PHASE_ATTRIBUTE, BEFORE_RESOURCE_PHASE);
            response.setServeResourcePath(getDispatchPathParameter(request, request.getLifecyclePhase()));
            doDispatch(getDispatchPathParameter(request, BEFORE_RESOURCE_PHASE), request, response);
        } finally {
            request.removeAttribute(LIFECYCLE_PHASE_ATTRIBUTE);
        }
    }
    
    protected void doDispatch(String dispatchPath, HstRequest request, HstResponse response) throws HstComponentException {
        if (dispatchPath != null) {
            try {
                getServletConfig().getServletContext().getRequestDispatcher(dispatchPath).include(request, response);
            } catch (ServletException e) {
                throw new HstComponentException(e);
            } catch (IOException e) {
                throw new HstComponentException(e);
            }
        }
    }
    
    protected String getDispatchPathParameter(HstRequest request, String lifecyclePhase) {
        String dispatchPath = null;
        
        if (BEFORE_RENDER_PHASE.equals(lifecyclePhase)) {
            dispatchPath = getParameter(BEFORE_RENDER_PATH_PARAM_NAME, request);
        } else if (HstRequest.RENDER_PHASE.equals(lifecyclePhase)) {
            dispatchPath = getParameter(RENDER_PATH_PARAM_NAME, request);
        } else if (HstRequest.ACTION_PHASE.equals(lifecyclePhase)) {
            dispatchPath = getParameter(ACTION_PATH_PARAM_NAME, request);
        } else if (BEFORE_RESOURCE_PHASE.equals(lifecyclePhase)) {
            dispatchPath = getParameter(BEFORE_RESOURCE_PATH_PARAM_NAME, request);
        } else if (HstRequest.RESOURCE_PHASE.equals(lifecyclePhase)) {
            dispatchPath = getParameter(RESOURCE_PATH_PARAM_NAME, request);
        }
        
        if (dispatchPath == null) {
            dispatchPath = getParameter(DISPATCH_PATH_PARAM_NAME, request);
        }
        
        if (dispatchPath != null) {
            if (dispatchPath.charAt(0) != '/') {
                dispatchPath = new StringBuilder(dispatchPath.length() + 1).append('/').append(dispatchPath).toString();
            }
        }
        
        return dispatchPath;
    }
    
    protected String getParameter(String name, HstRequest request) {
        return (String) this.getComponentConfiguration().getParameter(name, request.getRequestContext().getResolvedSiteMapItem());
    }
    
}
