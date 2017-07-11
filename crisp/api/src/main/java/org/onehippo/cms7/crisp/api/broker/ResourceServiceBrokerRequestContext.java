/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.api.broker;

import javax.servlet.ServletRequest;

/**
 * Provides an access method to the current request object if available.
 */
public class ResourceServiceBrokerRequestContext {

    private static ThreadLocal<ServletRequest> tlRequest = new ThreadLocal<ServletRequest>();

    private ResourceServiceBrokerRequestContext() {
    }

    /**
     * Return the current request object if available.
     * @return the current request object
     */
    public static ServletRequest getCurrentServletRequest() {
        return tlRequest.get();
    }

    /**
     * Set the current request object if available.
     * @param servletRequest the current request object
     */
    public static void setCurrentServletRequest(ServletRequest servletRequest) {
        tlRequest.set(servletRequest);
    }

    /**
     * Clear all the attributes stored for the current request context.
     */
    public static void clear() {
        tlRequest.remove();
    }

}
