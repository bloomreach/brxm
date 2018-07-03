/**
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.container;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * ModifiableRequestContextProvider provides a way to modify the current thread's request context object
 * which can be accessed via {@link org.hippoecm.hst.container.RequestContextProvider#get()}.
 */
public final class ModifiableRequestContextProvider {

    private ModifiableRequestContextProvider() {
    }

    /**
     * Returns the {@link HstRequestContext} for the current threads active request.
     *
     */
    public static HstRequestContext get() {
        return RequestContextProvider.get();
    }

    /**
     * Sets the {@link HstRequestContext} for the current threads active request.
     * 
     * @param requestContext
     */
    public static void set(HstRequestContext requestContext) {
        new RequestContextProvider.ModifiableRequestContextProvider(){}.set(requestContext);
    }

    /**
     * Clears the {@link HstRequestContext} for the current threads active request.
     *
     */
    public static void clear() {
        new RequestContextProvider.ModifiableRequestContextProvider(){}.clear();
    }
}
