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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Pipeline interface.
 * {@link HstRequestProcessor} will invoke the proper {@link Pipeline} instance to serve the request.
 * 
 * @version $Id$
 */
public interface Pipeline
{
    
    /**
     * Initializes the pipeline
     * @throws ContainerException
     */
    void initialize() throws ContainerException;

    /**
     * Invokes the request processing, aka the initialization and rendering valves
     * 
     * @param requestContainerConfig the HstComponent container configuration
     * @param requestContext
     * @param servletRequest
     * @param servletResponse
     * @throws ContainerException
     */
    void invoke(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ContainerException;

    /**
     * Does post-processing step for the request processing.
     * 
     * @param requestContainerConfig the HstComponent container configuration
     * @param requestContext
     * @param servletRequest
     * @param servletResponse
     * @throws ContainerException
     */
    
    void cleanup(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ContainerException;

    /**
     * Destroys the pipeline.
     * 
     * @throws ContainerException
     */
    void destroy() throws ContainerException;
    
}
