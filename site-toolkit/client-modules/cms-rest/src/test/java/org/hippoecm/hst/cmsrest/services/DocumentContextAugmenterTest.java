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

import java.util.UUID;

import org.hippoecm.hst.cmsrest.AbstractCmsRestTest;
import org.hippoecm.hst.rest.DocumentService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DocumentContextAugmenterTest extends AbstractCmsRestTest {

    private DocumentService documentService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        documentService = getComponentManager().getComponent(DocumentService.class, "org.hippoecm.hst.cmsrest");
        ((DocumentsResource)documentService).addDocumentContextAugmenter(new ShortCircuitingDocumentContextAugmenter());
    }

    @Test
    public void assert_documentContextAugmenter_is_invoked() throws Exception {
        try {
            documentService.getChannels(UUID.randomUUID().toString());
            fail("Expected the #getChannels to be short-circuited by ShortCircuitingDocumentContextAugmenter");
        } catch (IllegalStateException e) {
            assertEquals(ShortCircuitingDocumentContextAugmenter.SHORT_CIRCUITING_DOCUMENT_CONTEXT_AUGMENTER_IS_WORKING, e.getMessage());
        }
    }

}
