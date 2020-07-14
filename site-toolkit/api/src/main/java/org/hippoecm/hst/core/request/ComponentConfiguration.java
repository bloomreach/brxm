/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.request;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type;
import org.hippoecm.hst.configuration.components.DynamicParameter;

/**
 * A <code>ComponentConfiguration</code> provides some configuration information to a component.
 * 
 * @version $Id$
 */
public interface ComponentConfiguration extends ParameterConfiguration {

    /**
     * Returns the property without inheritance and if an expression exists it is resolved with the help of the ResolvedSiteMapItem
     */
    String getLocalParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem);
    
    /**
     * Returns all resolved parameters  without inheritance into a map
     * @param hstResolvedSiteMapItem
     * 
     */
    Map<String, String> getLocalParameters(ResolvedSiteMapItem hstResolvedSiteMapItem);

    List<DynamicParameter> getDynamicComponentParameters();

    Optional<DynamicParameter> getDynamicComponentParameter(String name);

    /**
     * Also see {@link #getParameters(ResolvedSiteMapItem)}. Normally, you use {@link #getParameters(ResolvedSiteMapItem)}, unless you want to access
     * parameter values without having their property placeholders (like ${1}) substituded by the current request context. In other words, the parameter values
     * exactly the way they are in the {@link HstComponentConfiguration}
     * @return An unmodifiableMap of all parameters the way they are configured in the hst component configuration without having any property placeholder filled in by the current context
     */
    Map<String, String> getRawParameters();

    /**
     * Also see {@link #getLocalParameters(ResolvedSiteMapItem)}. Normally, you use {@link #getLocalParameters(ResolvedSiteMapItem)}, unless you want to access
     * parameter values without having their property placeholders (like ${1}) substituded by the current request context. In other words, the parameter values
     * exactly the way they are in the {@link HstComponentConfiguration}. Note that this method does not return inherited parameters, but only its local (directly)
     * configured ones
     * @return An unmodifiableMap of all parameters the way they are configured in the hst component configuration without having any property placeholder filled in by the current context and without parameter inheritance
     */
    Map<String, String> getRawLocalParameters();
    
    /**
     * @return the location of the view renderer
     */
    String getRenderPath();
    
    /**
     * @return return the servletpath of the servlet that must serve the resources for this <code>HstComponent</code>
     */
    String getServeResourcePath();

    /**
     * Returns the canonical path of the backing stored configuration location. Note that multiple (many) <code>ComponentConfiguration</code>'s
     * can share the same canonical path: When a component is added by a reference, this method will return the same canonical path as the
     * direct (not referenced) component.
     * 
     * @return the canonical path of the backing stored configuration location.
     */
    String getCanonicalPath();
    
    /**
     * Returns the identifier of the backing stored component configuration. Note that multiple <code>ComponentConfiguration</code>'s can share the same
     * canonical identifier due to inheritance. Also, multiple subsites can share the same backing configuration, and thus share the same canonical identifiers 
     * 
     * @return the identifier of the backing stored component configuration
     */
    String getCanonicalIdentifier();
    
    /**
     * 
     * @return the xtype of this ComponentConfiguration and <code>null</code> if the component does not have one
     */
    String getXType();

    /**
     * @return the ctype of this ComponentConfiguration and <code>null</code> if the component does not have one
     */
    String getCType();

    /**
     * 
     * @return the {@link Type} of this {@link ComponentConfiguration}
     */
    Type getComponentType();

    /**
     * @return if configured, the fully-qualified class name of the interface representing <code>ParametersInfo</code> for a component,
     * and otherwise {@code null}.
     */
    String getParametersInfoClassName();
}
