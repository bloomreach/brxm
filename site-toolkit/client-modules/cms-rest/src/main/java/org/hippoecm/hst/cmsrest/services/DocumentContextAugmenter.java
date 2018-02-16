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
package org.hippoecm.hst.cmsrest.services;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * For every invocation of {@link org.hippoecm.hst.rest.DocumentService#getChannels(String)} and
 * {@link org.hippoecm.hst.rest.DocumentService#getUrl(String, String)} this {@link #apply(HstRequestContext, String)}
 * will be invoked for every {@link DocumentContextAugmenter} that is added via
 * {@link DocumentsResource#addDocumentContextAugmenter(DocumentContextAugmenter)}
 */
@FunctionalInterface
public interface DocumentContextAugmenter {

    void apply(HstRequestContext requestContext, String uuid);

}

