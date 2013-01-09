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
package org.hippoecm.hst.core.container;

import java.util.Map;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.ResolvedMount;

/**
 * The HstComponent container URL.
 * This is responsible for managing the request URL states of all the HstComponents.
 * 
 * @version $Id$
 */
public interface HstContainerURL {

    /**
     * Returns the current response's character encoding.
     * 
     * @return
     */
    String getCharacterEncoding();

    /**
     * Returns the host name of the request: note that with reverse proxies, this can be the original host informations requested 
     * by the client or the proxies 
     * @return the host name of the request
     */
    String getHostName();

    /**
     * Returns the current container's servlet context path.
     * 
     * @return
     */
    String getContextPath();

    /**
     * Returns the current request after the context path but before the queryString. This is thus the servletPath plus pathInfo. 
     * 
     * @return 
     */
    String getRequestPath();

    /**
     * Returns the portnumber of the request: note that with reverse proxies, this can be the original port requested 
     * by the client or the proxies 
     * @return the portnumber of the request
     */
    int getPortNumber();

    /**
     * 
     * @return {@link ResolvedMount} path or <code>null</code> when not yet resolved
     */
    String getResolvedMountPath();

    /**
     * Returns the current request's path info (part after context path and {@link Mount} path)
     * 
     * @return path info or <code>null</code> when the resolved mount path is not yet resolved
     */
    String getPathInfo();

    /**
     * Returns the reference namespace of the current action window if it exists.
     * Returns null if there's no action window in the currernt request.
     * 
     * @return
     */
    String getActionWindowReferenceNamespace();

    /**
     * Sets the reference namespace of the current action window if it is necessary.
     * This is invoked by the container's request processor to manage the request states.
     * 
     * @param actionWindowReferenceNamespace
     */
    void setActionWindowReferenceNamespace(String actionWindowReferenceNamespace);

    /**
     * Returns the reference namespace of the current resource serving window if it exists.
     * Returns null if there's no resource serving window in the currernt request.
     * 
     * @return
     */
    String getResourceWindowReferenceNamespace();

    /**
     * Sets the reference namespace of the current resource serving window if it is necessary.
     * This is invoked by the container's request processor to manage the request states.
     * 
     * @param resourceWindowReferenceNamespace
     */
    void setResourceWindowReferenceNamespace(String resourceWindowReferenceNamespace);

    /**
     * Returns the reference namespace of the current rendering component if it exists. If this is not
     * a request for component rendering (rendering a single component), then this returns <code>null</code>
     * @return the componentRenderingWindowReferenceNamespace and <code>null</code> if the request is not a component rendering request
     */
    String getComponentRenderingWindowReferenceNamespace();

    /**
     * Sets the reference namespace of the current component rendering window if it is necessary.
     * This is invoked by the container's request processor to manage the request states.
     * @param componentRenderingWindowReferenceNamespace
     */
    void setComponentRenderingWindowReferenceNamespace(String componentRenderingWindowReferenceNamespace);

    /**
     * Returns the resource ID if the current request is for serving resource in a component window.
     * Otherwise, it returns null.
     * 
     * @return
     */
    String getResourceId();

    /**
     * Sets the resource ID if the current request is for serving resource in a component window.
     * @param resourceId
     */
    void setResourceId(String resourceId);

    /**
     * Sets the render parameter for a HstComponent.
     * If the value is null, it will remove the parameter.
     * 
     * @param name
     * @param value
     */
    void setParameter(String name, String value);

    /**
     * Sets the render parameter value array for a HstComponent.
     * If the values is null, it will remove the parameter.
     * 
     * @param name
     * @param values
     */
    void setParameter(String name, String[] values);

    /**
     * Sets the render parameter map.
     * 
     * @param parameters
     */
    void setParameters(Map<String, String[]> parameters);

    /**
     * Returns the render parameter map.
     * 
     * @return
     */
    Map<String, String[]> getParameterMap();

    /**
     * Sets the action parameter for the target HstComponent.
     * If the value is null, it will remove the parameter.
     * 
     * @param name
     * @param value
     */
    void setActionParameter(String name, String value);

    /**
     * Sets the action parameter values for the target HstComponent.
     * If the values is null, it will remove the parameter.
     * 
     * @param name
     * @param values
     */
    void setActionParameter(String name, String[] values);

    /**
     * Sets the action parameter map.
     * 
     * @param parameters
     */
    void setActionParameters(Map<String, String[]> parameters);

    /**
     * Returns the action parameter map.
     * 
     * @return
     */
    Map<String, String[]> getActionParameterMap();

}
