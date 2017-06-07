/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.ServletContext;

import org.hippoecm.hst.core.request.ComponentConfiguration;

/**
 * A HstComponent can be invoked by a HstComponent container
 * during three different request lifecycle phases: ACTION, RESOURCE and RENDER.
 * A HstComponent should be implemented to be thread-safe
 * because the HST container create one instance to serve each request.
 * 
 * @version $Id$
 */
public interface HstComponent {
    
    /**
     * Allows the component to initialize itself
     * 
     * @param servletContext the servletConfig of the HST container servlet
     * @param componentConfig the componentConfigBean configuration
     * @throws HstComponentException
     */
    void init(ServletContext servletContext, ComponentConfiguration componentConfig) throws HstComponentException;

    /**
     * This method is invoked before {@link #doBeforeRender(HstRequest, HstResponse)} method to give an HstComponent
     * a chance to <i>prepare</i> any business service invocation(s).
     * This method can be implemented to prepare business content objects by creating asynchronous jobs in parallel
     * without having to wait each component's {@link #doBeforeRender(HstRequest, HstResponse)} execution sequentially.
     *
     * @param request
     * @param response
     * @throws HstComponentException
     */
    default void prepareBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
    }

    /**
     * Allows the component to do some business logic processing before rendering
     * 
     * @param request
     * @param response
     * @throws HstComponentException
     */
    void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException;
    
    /**
     * Allows the component to process actions
     * 
     * @param request
     * @param response
     * @throws HstComponentException
     */
    void doAction(HstRequest request, HstResponse response) throws HstComponentException;
    
    /**
     * Allows the component to do some business logic processing before serving resource
     * 
     * @param request
     * @param response
     * @throws HstComponentException
     */
    void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException;
    
    /**
     * Allows the component to destroy itself
     * 
     * @throws HstComponentException
     */
    void destroy() throws HstComponentException;

    /**
     * Returns the ComponentConfiguration for this component or <code>null</code>
     * if not implemented by a subclass
     */
    default ComponentConfiguration getComponentConfiguration() {
        return null;
    }
    
}
