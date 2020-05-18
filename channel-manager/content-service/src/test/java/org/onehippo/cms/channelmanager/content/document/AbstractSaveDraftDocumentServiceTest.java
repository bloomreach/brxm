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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.DocumentInfo;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.cms.channelmanager.content.document.AbstractSaveDraftDocumentService.EDIT_DRAFT;
import static org.onehippo.cms.channelmanager.content.document.AbstractSaveDraftDocumentService.SAVE_DRAFT;

/**
 * <p>Test the business logics of the DocumentService</p>
 */
public class AbstractSaveDraftDocumentServiceTest {


    private TestSaveDraftDocumentService testSaveDraftDocumentService;
    private AbstractSaveDraftDocumentService documentsService;
    private Document persistedDraft;
    private DocumentType documentType;

    @Before
    public void setUp() throws Exception {
        testSaveDraftDocumentService = new TestSaveDraftDocumentService("id", "master", null);
        documentsService = testSaveDraftDocumentService;
        persistedDraft = new Document();
        persistedDraft.setId("id");
        documentType = new DocumentType();
        documentType.setId("docTypeId");
        testSaveDraftDocumentService.addDocumentType("docTypeId", documentType);
        testSaveDraftDocumentService.associateDocumentWithNodeType("id", "docTypeId");
        testSaveDraftDocumentService.setDraft(persistedDraft);
    }

    @Test
    public void editDraft() {
        Map<String, Serializable> hints = new HashMap<>();
        hints.put(EDIT_DRAFT, Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        final Document expected = new Document();
        expected.setId("id");
        final Document actual = documentsService.editDraft();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void editDraft_info_nonRetainable() {
        Map<String, Serializable> hints = new HashMap<>();
        hints.put(EDIT_DRAFT, Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        final Document actual = documentsService.editDraft();
        final Document expected = new Document();
        expected.setId("id");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void editDraft_info_Retainable() {
        Map<String, Serializable> hints = new HashMap<>();
        hints.put(EDIT_DRAFT, Boolean.TRUE);
        hints.put(SAVE_DRAFT, Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        persistedDraft.getInfo().setRetainable(true);
        final Document actual = documentsService.editDraft();
        final Document expected = new Document();
        expected.getInfo().setCanKeepDraft(true);
        expected.getInfo().setRetainable(true);
        expected.setId("id");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void editDraftNotEditable() throws Exception {
        try {
            documentsService.editDraft();
            fail("No exception");
        } catch (final ForbiddenException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    @Test
    public void editDraftNoDocumentType() throws Exception {
        Map<String, Serializable> hints = new HashMap<>();
        hints.put(EDIT_DRAFT, Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        testSaveDraftDocumentService.associateDocumentWithNodeType("id", "other");

        try {
            documentsService.editDraft();
            fail("No exception");
        } catch (final InternalServerErrorException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    @Test
    public void editDraftUnkownValidator() throws Exception {
        Map<String, Serializable> hints = new HashMap<>();
        hints.put(EDIT_DRAFT, Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        documentType.setReadOnlyDueToUnsupportedValidator(true);
        final ErrorInfo documentInfo = new ErrorInfo(ErrorInfo.Reason.CREATE_WITH_UNSUPPORTED_VALIDATOR);
        testSaveDraftDocumentService.setDisplayName("Display name");
        testSaveDraftDocumentService.setPublicationState("unpublished");
        try {
            documentsService.editDraft();
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
        hints.put(EDIT_DRAFT, Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        testSaveDraftDocumentService.setDraft(null);
        try {
            documentsService.editDraft();
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    @Test
    public void saveDraft_EditNotAllowed() throws Exception {
        testSaveDraftDocumentService.setHints(Collections.emptyMap());
        try {
            documentsService.saveDraft(new Document());
            fail("Save draft should throw and exception");
        } catch (final ForbiddenException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    @Test
    public void saveDraft_EditAllowed_SaveNotAllowed() throws Exception {
        Map<String, Serializable> hints = new HashMap<>();
        hints.put(EDIT_DRAFT, Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        try {
            documentsService.saveDraft(new Document());
            fail("Save draft should throw and exception");
        } catch (final ForbiddenException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    @Test
    public void saveDraft() throws Exception {
        Map<String, Serializable> hints = new HashMap<>();
        hints.put(EDIT_DRAFT, Boolean.TRUE);
        hints.put(SAVE_DRAFT, Boolean.TRUE);
        Document input = new Document();
        input.setId("id");
        testSaveDraftDocumentService.setHints(hints);
        final Map<String, List<FieldValue>> fields = Collections.singletonMap("propertyName", Collections.singletonList(new FieldValue("propertyValue")));
        input.setFields(fields);
        Document expected = new Document();
        expected.setId("id");
        expected.getInfo().setCanKeepDraft(true);
        expected.getInfo().setRetainable(true);
        expected.getInfo().setDirty(false);
        expected.setFields(fields);
        Document actual = documentsService.saveDraft(input);
        assertEquals(expected, actual);
    }

    @Test
    public void addDocumentInfo() throws Exception{
        Document input = new Document();
        input.setId("id");
        input.setFields(Collections.singletonMap("key", Collections.singletonList(new FieldValue("value"))));
        Map<String, Serializable> hints = new HashMap<>();
        hints.put(EDIT_DRAFT, Boolean.TRUE);
        hints.put(SAVE_DRAFT, Boolean.TRUE);
        // publish and request publication are not added to the document info
        // because they are always false when a document is transferable
        hints.put(EditingUtils.HINT_PUBLISH, Boolean.TRUE);
        hints.put(EditingUtils.HINT_REQUEST_PUBLICATION, Boolean.TRUE);
        testSaveDraftDocumentService.setHints(hints);
        persistedDraft.getInfo().setRetainable(true);
        final DocumentInfo actual = testSaveDraftDocumentService.addDocumentInfo(input);
        final DocumentInfo expected =  new DocumentInfo();
        expected.setDirty(true);
        expected.setRetainable(true);
        expected.setCanPublish(false);
        expected.setCanRequestPublication(false);
        expected.setCanKeepDraft(true);
        assertEquals(expected, actual);


    }


}
