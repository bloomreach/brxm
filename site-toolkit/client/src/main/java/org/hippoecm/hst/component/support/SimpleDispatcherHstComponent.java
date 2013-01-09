/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static Logger log = LoggerFactory.getLogger(SimpleDispatcherHstComponent.class);
    
    public static final String LIFECYCLE_PHASE_ATTRIBUTE = SimpleDispatcherHstComponent.class.getName() + ".lifecycle.phase";
    
    public static final String BEFORE_RENDER_PHASE = "BEFORE_RENDER_PHASE";
    public static final String BEFORE_RESOURCE_PHASE = "BEFORE_RESOURCE_PHASE";
    
    /**
     * The parameter name for the default dispatch path for every invocation. If you set this parameter in the configuration,
     * the servlet indicated by this path should handle everything by itself.
     */
    public static final String DISPATCH_PATH_PARAM_NAME = "dispatch-path";
    
    /**
     * The parameter name for the dispatch path for <CODE>BEFORE_RENDER_PHASE</CODE>.
     * This component would dispatch to this path in its {@link #doBeforeRender(HstRequest, HstResponse)}.
     */
    public static final String BEFORE_RENDER_PATH_PARAM_NAME = "before-render-path";

    /**
     * The parameter name for the dispatch path for <CODE>RENDER_PHASE</CODE>.
     * This component would dispatch to this path in its {@link #doRender(HstRequest, HstResponse)}.
     */
    public static final String RENDER_PATH_PARAM_NAME = "render-path";
    
    /**
     * The parameter name for the dispatch path for <CODE>ACTION_PHASE</CODE>.
     * This component would dispatch to this path in its {@link #doAction(HstRequest, HstResponse)}.
     */
    public static final String ACTION_PATH_PARAM_NAME = "action-path";
    
    /**
     * The parameter name for the dispatch path for <CODE>BEFORE_RESOURCE_PHASE</CODE>.
     * This component would dispatch to this path in its {@link #doBeforeResource(HstRequest, HstResponse)}.
     */
    public static final String BEFORE_RESOURCE_PATH_PARAM_NAME = "before-resource-path";
    
    /**
     * The parameter name for the dispatch path for <CODE>RESOURCE_PHASE</CODE>.
     * This component would dispatch to this path in its {@link #doServeResource(HstRequest, HstResponse)}.
     */
    public static final String RESOURCE_PATH_PARAM_NAME = "resource-path";
    
    /**
     * The parameter name for the flag if the request attributes set during action phase are passed into render phase.
     */
    public static final String SHARED_REQUEST_ATTRIBUTES_PARAM_NAME = "shared-request-attributes";
    
    /**
     * The parameter name for the session attribute name by which the request attributes during action phase are stored temporarily
     * to pass the attributes to request of the following render phase. 
     */
    public static final String SHARED_REQUEST_ATTRIBUTES_SESSION_ATTRIBUTE_NAME_PREFIX_PARAM_NAME = "shared-request-attributes-session-attribute-name-prefix";
    
    /**
     * The default session attribute name prefix to store shared request attributes during action phase.
     */
    public static final String DEFAULT_SHARED_REQUEST_ATTRIBUTES_SESSION_ATTRIBUTE_NAME_PREFIX = SimpleDispatcherHstComponent.class.getName() + ".shared.request.attributes-";
    
    
    private ServletContext servletContext;

    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletContext, componentConfig);
        this.servletContext = servletContext;
    }
    
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        doDispatch(getDispatchPathParameter(request, request.getLifecyclePhase()), request, response);
        
        if (Boolean.parseBoolean(getParameter(SHARED_REQUEST_ATTRIBUTES_PARAM_NAME, request, null))) {
            String sharedAttributeNamePrefix = getParameter(SHARED_REQUEST_ATTRIBUTES_SESSION_ATTRIBUTE_NAME_PREFIX_PARAM_NAME, request, DEFAULT_SHARED_REQUEST_ATTRIBUTES_SESSION_ATTRIBUTE_NAME_PREFIX);
            String sharedAttributeName = sharedAttributeNamePrefix + response.getNamespace();
            Map<String, Object> sharedAttrMap = new HashMap<String, Object>();
            String attrName = null;
            Object attrValue = null;
            
            for (Enumeration attrNames = request.getAttributeNames(); attrNames.hasMoreElements(); ) {
                attrName = (String) attrNames.nextElement();
                
                if (!attrName.startsWith("javax.")) {
                    attrValue = request.getAttribute(attrName);
                    
                    if (attrValue != null) {
                        sharedAttrMap.put(attrName, attrValue);
                    }
                }
            }
            
            request.getSession(true).setAttribute(sharedAttributeName, sharedAttrMap);
        }
    }
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        request.setAttribute(LIFECYCLE_PHASE_ATTRIBUTE, BEFORE_RENDER_PHASE);

        String dispatchPath = getDispatchPathParameter(request, request.getLifecyclePhase());
        
        if (dispatchPath != null) {
            response.setRenderPath(dispatchPath);
        }

        if (Boolean.parseBoolean(getParameter(SHARED_REQUEST_ATTRIBUTES_PARAM_NAME, request, null))) {
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                String sharedAttributeNamePrefix = getParameter(SHARED_REQUEST_ATTRIBUTES_SESSION_ATTRIBUTE_NAME_PREFIX_PARAM_NAME, request, DEFAULT_SHARED_REQUEST_ATTRIBUTES_SESSION_ATTRIBUTE_NAME_PREFIX);
                String sharedAttributeName = sharedAttributeNamePrefix + response.getNamespace();
                Map<String, Object> sharedAttrMap = (Map<String, Object>) session.getAttribute(sharedAttributeName);
                
                if (sharedAttrMap != null) {
                    String attrName = null;
                    Object attrValue = null;
                    
                    for (Map.Entry<String, Object> entry : sharedAttrMap.entrySet()) {
                        attrName = entry.getKey();
                        attrValue = request.getAttribute(attrName);
                        
                        if (attrValue == null) {
                            request.setAttribute(attrName, entry.getValue());
                        }
                    }
                    
                    session.removeAttribute(sharedAttributeName);
                }
            }
        }

        try {
            doDispatch(getDispatchPathParameter(request, BEFORE_RENDER_PHASE), request, response);
        } finally {
            request.removeAttribute(LIFECYCLE_PHASE_ATTRIBUTE);
        }
    }
    
    @Override
    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        
        super.doBeforeServeResource(request, response);
        
        request.setAttribute(LIFECYCLE_PHASE_ATTRIBUTE, BEFORE_RESOURCE_PHASE);
        
        String dispatchPath = getDispatchPathParameter(request, request.getLifecyclePhase());
        
        if (dispatchPath != null) {
            response.setServeResourcePath(dispatchPath);
        }

        try {
            doDispatch(getDispatchPathParameter(request, BEFORE_RESOURCE_PHASE), request, response);
        } finally {
            request.removeAttribute(LIFECYCLE_PHASE_ATTRIBUTE);
        }
    }
    
    protected void doDispatch(String dispatchPath, HstRequest request, HstResponse response) throws HstComponentException {
        if (dispatchPath != null) {
            try {
                servletContext.getRequestDispatcher(dispatchPath).include(request, response);
            } catch (ServletException e) {
                throw new HstComponentException(e);
            } catch (IOException e) {
                throw new HstComponentException(e);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("The dispatch path is null. The lifecycle phase is {} and the dispatch lifecycle phase is {}", 
                        request.getLifecyclePhase(), 
                        request.getAttribute(LIFECYCLE_PHASE_ATTRIBUTE));
            }
        }
    }
    
    protected String getDispatchPathParameter(HstRequest request, String lifecyclePhase) {
        String dispatchPath = null;
        
        if (BEFORE_RENDER_PHASE.equals(lifecyclePhase)) {
            dispatchPath = getParameter(BEFORE_RENDER_PATH_PARAM_NAME, request, null);
        } else if (HstRequest.RENDER_PHASE.equals(lifecyclePhase)) {
            dispatchPath = getParameter(RENDER_PATH_PARAM_NAME, request, null);
        } else if (HstRequest.ACTION_PHASE.equals(lifecyclePhase)) {
            dispatchPath = getParameter(ACTION_PATH_PARAM_NAME, request, null);
        } else if (BEFORE_RESOURCE_PHASE.equals(lifecyclePhase)) {
            dispatchPath = getParameter(BEFORE_RESOURCE_PATH_PARAM_NAME, request, null);
        } else if (HstRequest.RESOURCE_PHASE.equals(lifecyclePhase)) {
            dispatchPath = getParameter(RESOURCE_PATH_PARAM_NAME, request, null);
        }
        
        if (dispatchPath == null) {
            dispatchPath = getParameter(DISPATCH_PATH_PARAM_NAME, request, null);
        }
        
        dispatchPath = StringUtils.trim(dispatchPath);
        
        if (dispatchPath != null) {
            if (dispatchPath.charAt(0) != '/') {
                dispatchPath = new StringBuilder(dispatchPath.length() + 1).append('/').append(dispatchPath).toString();
            }
        }
        
        return dispatchPath;
    }
    
    protected String getParameter(String name, HstRequest request, String defaultValue) {
        String value = (String) this.getComponentConfiguration().getParameter(name, request.getRequestContext().getResolvedSiteMapItem());
        return (value != null ? value : defaultValue);
    }
    
}
