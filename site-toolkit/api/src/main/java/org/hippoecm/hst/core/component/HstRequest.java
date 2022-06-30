/*
 * Copyright 2008-2022 Bloomreach
 */
package org.hippoecm.hst.core.component;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.ModelContributable;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * The <CODE>HstRequest</CODE> defines the interface to provide client
 * request information to a HstComponent. The HstComponent container creates these objects and 
 * passes them as  arguments to the HstComponent's <CODE>doAction</CODE>,
 * <CODE>doBeforeRender</CODE> and <CODE>doBeforeServeResource</CODE> methods.
 * 
 * @version $Id$
 */
public interface HstRequest extends HttpServletRequest, ModelContributable {
    
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
     * @return an immutable {@code java.util.Map<String, String []>} containing parameter names as keys and parameter values as map values.
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
