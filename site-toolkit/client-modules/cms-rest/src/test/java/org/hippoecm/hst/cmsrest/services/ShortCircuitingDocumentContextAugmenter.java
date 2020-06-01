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

public class ShortCircuitingDocumentContextAugmenter implements  DocumentContextAugmenter {

    public static final String SHORT_CIRCUITING_DOCUMENT_CONTEXT_AUGMENTER_IS_WORKING = "ShortCircuitingDocumentContextAugmenter is working";

    @Override
    public void apply(final HstRequestContext requestContext, final String uuid) {
        throw new IllegalStateException(SHORT_CIRCUITING_DOCUMENT_CONTEXT_AUGMENTER_IS_WORKING);
    }
}
