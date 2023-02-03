/*
 *  Copyright 2018-2023 Bloomreach
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

public class PlatformRequestContextProvider {

    private final RequestContextProvider.ModifiableRequestContextProvider modifiableRequestContextProvider;

    public PlatformRequestContextProvider() {
        /**
         * Note the '{}' below looks funny but very important to avoid IllegalAccessError because the RequestContextProvider
         * lives in the shared lib and from the RequestContextProvider we do not want to expose
         * RequestContextProvider#set(HstRequestContext) or RequestContextProvider#clear(), hence we need to extent the
         * protected ModifiableRequestContextProvider
         */
        modifiableRequestContextProvider = new RequestContextProvider.ModifiableRequestContextProvider() {
        };
    }

    public void set(final HstRequestContext requestContext) {
        modifiableRequestContextProvider.set(requestContext);
    }
    public void clear() {
        modifiableRequestContextProvider.clear();
    }

}
