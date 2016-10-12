/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content;

import org.junit.Test;
import org.onehippo.cms.channelmanager.content.MockResponse;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MockResponseTest {

    @Test
    public void testDocument() {
        final Document document = MockResponse.createTestDocument("test");
        assertThat(document.getId(), equalTo("test"));
    }

    @Test
    public void testDocumentType() {
        final DocumentType docType = MockResponse.createTestDocumentType();
        assertThat(docType.getId(), equalTo("ns:testdocument"));
    }
}