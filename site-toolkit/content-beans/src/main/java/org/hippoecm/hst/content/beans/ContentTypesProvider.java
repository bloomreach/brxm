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

import javax.jcr.RepositoryException;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;

/**
 * Provides access to {@link ContentTypes} bound to {@link HstRequestContext}
 */
public class ContentTypesProvider {

    private ContentTypeService contentTypeService;

    public ContentTypesProvider(final ContentTypeService contentTypeService) {
        this.contentTypeService = contentTypeService;
    }

    /**
     * @return {@link ContentTypes} from current {@link HstRequestContext} or in case there
     * is no {@link HstRequestContext} it fetches the {@link ContentTypes} directly from the {@link ContentTypeService}
     */
    public ContentTypes getContentTypes() throws ObjectBeanManagerException {
        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            try {
                return contentTypeService.getContentTypes();
            } catch (RepositoryException e) {
                throw new ObjectBeanManagerException("Exception while getting content types",e);
            }
        } else {
            return requestContext.getContentTypes();
        }
    }
}
