/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans;

import org.hippoecm.hst.container.RequestContextProvider;
import org.onehippo.cms7.services.contenttype.ContentTypes;

/**
 * Provides access to {@link ContentTypes} bound to {@link org.hippoecm.hst.core.request.HstRequestContext}
 */
public class ContentTypesProvider {

    /**
     * @return {@link ContentTypes} from current {@link org.hippoecm.hst.core.request.HstRequestContext}
     */
    public ContentTypes getContentTypes() {
        return RequestContextProvider.get().getContentTypes();
    }
}
