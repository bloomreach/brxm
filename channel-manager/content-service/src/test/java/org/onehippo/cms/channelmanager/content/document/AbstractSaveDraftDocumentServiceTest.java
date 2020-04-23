/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.cms.channelmanager.content.document;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * <p>Test the business logics of the DocumentService</p>
 */
public class AbstractSaveDraftDocumentServiceTest {


    private TestSaveDraftDocumentService testSaveDraftDocumentService;
    private AbstractSaveDraftDocumentService documentsService;
    private Document expected;
    private DocumentType documentType;

    @Before
    public void setUp() throws Exception {
        testSaveDraftDocumentService = new TestSaveDraftDocumentService();
        documentsService = testSaveDraftDocumentService;
        expected = new Document();
        expected.setId("id");
        documentType = new DocumentType();
        documentType.setId("docTypeId");
        testSaveDraftDocumentService.addDocumentType("docTypeId", documentType);
        testSaveDraftDocumentService.associateDocumentWithNodeType("id", "docTypeId");
        testSaveDraftDocumentService.setDraft(expected);
    }

    @Test
    public void editDraft() {
        Map<String, Serializable> hints = new HashMap<>();
        hints.put("editDraft", Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        final Document actual = documentsService.editDraft("id", null);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void editDraftNotEditable() throws Exception {
        try {
            documentsService.editDraft("id", null);
            fail("No exception");
        } catch (final ForbiddenException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    @Test
    public void editDraftNoDocumentType() throws Exception {
        Map<String, Serializable> hints = new HashMap<>();
        hints.put("editDraft", Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        testSaveDraftDocumentService.associateDocumentWithNodeType("id", "other");

        try {
            documentsService.editDraft("id", null);
            fail("No exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    @Test
    public void editDraftUnkownValidator() throws Exception {
        Map<String, Serializable> hints = new HashMap<>();
        hints.put("editDraft", Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        documentType.setReadOnlyDueToUnsupportedValidator(true);
        final ErrorInfo documentInfo = new ErrorInfo(ErrorInfo.Reason.CREATE_WITH_UNSUPPORTED_VALIDATOR);
        testSaveDraftDocumentService.setDisplayName("Display name");
        testSaveDraftDocumentService.setPublicationState("unpublished");
        try {
            documentsService.editDraft("id", null);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.CREATE_WITH_UNSUPPORTED_VALIDATOR));
            assertThat(errorInfo.getParams().get("displayName"), equalTo("Display name"));
        }
    }

    @Test
    public void editDraftFailed() throws Exception {
        Map<String, Serializable> hints = new HashMap<>();
        hints.put("editDraft", Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        testSaveDraftDocumentService.setDraft(null);
        try {
            documentsService.editDraft("id", null);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.SERVER_ERROR));
        }
    }


}
