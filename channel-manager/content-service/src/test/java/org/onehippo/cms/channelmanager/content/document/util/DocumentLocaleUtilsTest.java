/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms.channelmanager.content.document.util;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class DocumentLocaleUtilsTest {

    @Test
    public void translatedDocument() throws RepositoryException {
        final MockNode handle = MockNode.root().addNode("some-document", "hippo:handle");
        final MockNode variant = handle.addNode("some-document", "hippo:document");
        variant.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        variant.setProperty(HippoTranslationNodeType.LOCALE, "en");

        assertThat(DocumentLocaleUtils.getDocumentLocale(variant), equalTo("en"));
    }

    @Test
    public void unTranslatedDocument() throws RepositoryException {
        final MockNode handle = MockNode.root().addNode("some-document", "hippo:handle");
        final MockNode variant = handle.addNode("some-document", "hippo:document");
        variant.setProperty(HippoTranslationNodeType.LOCALE, "en");

        assertThat(DocumentLocaleUtils.getDocumentLocale(variant), equalTo(null));
    }

}
