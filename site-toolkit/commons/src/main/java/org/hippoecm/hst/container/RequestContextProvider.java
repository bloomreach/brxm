/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.container;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * RequestContextProvider provides an easy access method to the request context object
 * in the current thread's active request.
 */
public final class RequestContextProvider {

    private static ThreadLocal<HstRequestContext> tlRequestContextHolder = new ThreadLocal<HstRequestContext>();

    private RequestContextProvider() {

    }

    /**
     * Returns the {@link HstRequestContext} for the current threads active request.
     *
     */
    public static HstRequestContext get() {
        return tlRequestContextHolder.get();
    }

    /**
     * Sets the {@link HstRequestContext} for the current threads active request.
     * 
     * @param requestContext
     */
    static void set(HstRequestContext requestContext) {
        tlRequestContextHolder.set(requestContext);
    }

    /**
     * Clears the {@link HstRequestContext} for the current threads active request.
     *
     */
    static void clear() {
        tlRequestContextHolder.remove();
    }
}
