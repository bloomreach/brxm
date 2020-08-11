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

    private static ThreadLocal<String> tlResourceSpace = new ThreadLocal<>();
    private static ThreadLocal<ServletRequest> tlRequest = new ThreadLocal<>();

    private ResourceServiceBrokerRequestContext() {
    }

    /**
     * Return the current resource space name in the {@link ResourceServiceBroker} invocation.
     * @return the current resource space name
     */
    public static String getCurrentResourceSpace() {
        return tlResourceSpace.get();
    }

    /**
     * Set the current resource space name in the {@link ResourceServiceBroker} invocation.
     * @param servletRequest the current resource space name
     */
    public static void setCurrentResourceSpace(String resourceSpace) {
        tlResourceSpace.set(resourceSpace);
    }

    /**
     * Return true if the current request object if available.
     * @return true if the current request object if available
     */
    public static boolean hasCurrentServletRequest() {
        return tlRequest.get() != null;
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
        tlResourceSpace.remove();
        tlRequest.remove();
    }

}
