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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Request processor. This request processor is called by the HstFilter.
 * This request processor can be initialized and run by another web application and its own classloader.
 * So, the HstFilter or other components should not assume that this
 * request processor is loaded by the same classloader.
 */
public interface HstRequestProcessor {
    
    /**
     * processes request
     * 
     * @param requestContainerConfig the holder for the servletConfig and classloader of the HST container
     * @param requestContext the requestContext of the HST request
     * @param servletRequest the servletRequest of the HST request
     * @param servletResponse the servletResponse of the HST response
     * @param namedPipeline the pipeline to use for this request
     * @throws ContainerException
     */
    void processRequest(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse, String namedPipeline) throws ContainerException;

    /**
     * Returns the current request container config object used in the current request processing context.
     * @return
     */
    HstContainerConfig getCurrentHstContainerConfig();

}
