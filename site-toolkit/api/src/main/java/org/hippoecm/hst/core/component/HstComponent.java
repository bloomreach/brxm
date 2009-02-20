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
package org.hippoecm.hst.core.component;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.components.HstComponentConfigurationBean;

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
     * @param servletConfig the servletConfig of the HST container servlet
     * @param componentConfigBean the componentConfigBean configuration
     * @throws HstComponentException
     */
    void init(ServletConfig servletConfig, HstComponentConfigurationBean componentConfigBean) throws HstComponentException;
    
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
    
}
