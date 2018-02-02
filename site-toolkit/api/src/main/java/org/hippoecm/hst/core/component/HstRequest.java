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
package org.hippoecm.hst.core.component;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * The <CODE>HstRequest</CODE> defines the interface to provide client
 * request information to a HstComponent. The HstComponent container creates these objects and 
 * passes them as  arguments to the HstComponent's <CODE>doAction</CODE>,
 * <CODE>doBeforeRender</CODE> and <CODE>doBeforeServeResource</CODE> methods.
 * 
 * @version $Id$
 */
public interface HstRequest extends HttpServletRequest {
    
    /**
     * String identifier for the HST action lifecycle phase.
     */
    static final String ACTION_PHASE = "ACTION_PHASE";
    
    /**
     * String identifier for the HST render lifecycle phase.
     */
    static final String RENDER_PHASE = "RENDER_PHASE";
    
    /**
     * String identifier for the HST resource serving lifecycle phase.
     */
    static final String RESOURCE_PHASE = "RESOURCE_PHASE";
      
    /**
     * Returns the current request context
     */
    HstRequestContext getRequestContext();
    
    /**
     * Returns the parameter map of this component window.
     * If the request is in the action lifecycle, then only action parameters can be accessible.
     * Otherwise, then only render parameters can be accessible.
     * @return an immutable java.util.Map<String, String []> containing parameter names as keys and parameter values as map values.
     *         The keys in the parameter map are of type String. 
     *         The values in the parameter map are of type String array.
     */
    Map<String, String []> getParameterMap();
    
    /**
     * The reference namespace of the component window.
     * 
     * @return the reference namespace of the component window
     */
    String getReferenceNamespace();
    
    /**
     * Returns the parameter map of the specified reference namespace component window.
     * If the request type is in the action lifecycle, the reference namespace parameter will be just ignored
     * and the operation will be equivalent to {@link #getParameterMap()}.
     * @param referenceNamespace
     */
    Map<String, String []> getParameterMap(String referenceNamespace);

    /**
     * Returns the model object associated with the given {@code name},
     * or <code>null</code> if no model object of the given {@code name} exists.
     *
     * @param name the name of the model object
     * @return the model object associated with the {@code name}, or
     *         <tt>null</tt> if the model object does not exist.
     */
    <T> T getModel(String name);

    /**
     * Returns an <code>Enumeration</code> containing the
     * names of the model objects available to this request. 
     * This method returns an empty <code>Enumeration</code>
     * if the request has no model object available to it.
     *
     * @return an <code>Enumeration</code> of strings containing the names 
     * of the request's model objects
     */
    Enumeration<String> getModelNames();

    /**
     * Returns an unmodifiable map of model objects available to this request.
     * @return an unmodifiable map of model objects available to this request
     */
    Map<String, Object> getModelsMap();

    /**
     * Stores a model object in this request.
     * <p>
     * Model objects are contributed by a controller component to this request, in general.
     * And, the contributed model objects may be accessed in view rendering or special model
     * aggregation / serialization request pipeline processing.
     * </p>
     * <p>
     * If the model object passed in is null, the effect is the same as
     * calling {@link #removeModel}.
     *
     * </p>
     * @param name the name of the model object
     * @param model the model object to be stored
     * @return the previous model object associated with <tt>name</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>name</tt>.
     */
    Object setModel(String name, Object model);

    /**
     * Removes a model object from this request.
     *
     * @param name a <code>String</code> specifying 
     * the name of the model object to remove
     */
   void removeModel(String name);

    /**
     * Returns the attribute map of this component window.
     */
    Map<String, Object> getAttributeMap();
    
    /**
     * Returns the attribute map of the specified reference namespace component window.
     */
    Map<String, Object> getAttributeMap(String referenceNamespace);
    
    /**
     * Returns the resource ID which was set by the resource HST URL.
     */
    String getResourceID();
    
    /**
     * Returns the lifecycle phase of the current HST request.
     * 
     * @see #ACTION_PHASE
     * @see #RENDER_PHASE
     * @see #RESOURCE_PHASE
     */
    String getLifecyclePhase();

}
