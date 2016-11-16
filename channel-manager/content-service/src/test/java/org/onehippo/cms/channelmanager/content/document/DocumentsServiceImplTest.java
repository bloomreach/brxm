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

package org.onehippo.cms.channelmanager.content.document;

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.repository.mock.MockNode;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DocumentsServiceImpl.class, WorkflowUtils.class})
public class DocumentsServiceImplTest {
    private Node rootNode;
    private Session session;
    private Locale locale;
    private DocumentsServiceImpl documentsService = (DocumentsServiceImpl) DocumentsService.get();

    @Before
    public void setup() throws RepositoryException {
        rootNode = MockNode.root();
        session = rootNode.getSession();
        locale = new Locale("en");
    }

    @Test(expected = NotFoundException.class)
    public void nodeNotFound() throws Exception {
        documentsService.getPublished("unknown-uuid", session, locale);
    }

    @Test(expected = NotFoundException.class)
    public void nodeNotHandle() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "invalid-type");
        final String id = handle.getIdentifier();
        documentsService.getPublished(id, session, locale);
    }

/*
    @Test(expected = DocumentNotFoundException.class)
    public void returnNotFoundWhenDocumentHandleHasNoVariantNode() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "hippo:handle");
        final String id = handle.getIdentifier();

        handle.addNode("otherName", "ns:doctype");

        documentsService.getDocument(id, session, null);
    }

    @Test
    public void successfulButStubbedDocumentRetrieval() throws Exception {
        final Node handle = rootNode.addNode("testDocument", "hippo:handle");
        final String id = handle.getIdentifier();
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final EditingInfo info = new EditingInfo();
        final Document document = new Document();
        final Locale locale = new Locale("en");

        PowerMock.createMock(Document.class);
        PowerMock.expectNew(Document.class).andReturn(document);
        PowerMock.replayAll();

        handle.addNode("testDocument", "ns:doctype");
        handle.setProperty(HippoNodeType.HIPPO_NAME, "Test Document");
        documentsService = createMockBuilder(DocumentsServiceImpl.class)
                .addMockedMethod("retrieveWorkflow")
                .addMockedMethod("determineEditingInfo")
                .addMockedMethod("determineDocumentFields")
                .createMock();
        expect(documentsService.retrieveWorkflow(handle)).andReturn(workflow);
        expect(documentsService.determineEditingInfo(session, workflow)).andReturn(info);
        documentsService.determineDocumentFields(document, handle, workflow, locale);
        expectLastCall();
        replay(documentsService);

        assertThat(documentsService.getDocument(id, session, locale), equalTo(document));

        verify(documentsService);

        assertThat(document.getId(), equalTo(id));
        assertThat(document.getDisplayName(), equalTo("Test Document"));
        assertThat(document.getInfo().getType().getId(), equalTo("ns:doctype"));
    }

    @Test
    public void loadBasicFields() throws Exception {
        final Document document = new Document();
        final Locale locale = new Locale("en");
        final DocumentTypeSpec docType = new DocumentTypeSpec();
        final Node handle = createMock(Node.class);
        final Node draft = rootNode.addNode("test", "test");
        final Session session = createMock(Session.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        FieldTypeSpec field = new FieldTypeSpec();
        field.setId("present-string-field");
        field.setType(FieldTypeSpec.Type.STRING);
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("absent-string-field");
        field.setType(FieldTypeSpec.Type.STRING);
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("present-multiline-string-field");
        field.setType(FieldTypeSpec.Type.MULTILINE_STRING);
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("present-multiple-string-field");
        field.setType(FieldTypeSpec.Type.STRING);
        field.setStoredAsMultiValueProperty(true);
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("empty-multiple-string-field");
        field.setType(FieldTypeSpec.Type.STRING);
        field.setStoredAsMultiValueProperty(true);
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("absent-multiple-string-field");
        field.setType(FieldTypeSpec.Type.STRING);
        field.setStoredAsMultiValueProperty(true);
        docType.addField(field);

        draft.setProperty("present-string-field", "Present String Field");
        draft.setProperty("present-multiline-string-field", "Present Multiline Sting Field");
        draft.setProperty("present-multiple-string-field", new String[] { "one", "two", "three" });
        draft.setProperty("empty-multiple-string-field", new String[] { });

        documentsService = createMockBuilder(DocumentsServiceImpl.class)
                .addMockedMethod("getOrMakeDraftNode")
                .addMockedMethod("getDocumentType")
                .createMock();
        expect(documentsService.getOrMakeDraftNode(workflow, handle)).andReturn(draft);
        expect(documentsService.getDocumentType(document, session, locale)).andReturn(docType);
        expect(handle.getSession()).andReturn(session);
        replay(documentsService, handle);

        documentsService.determineDocumentFields(document, handle, workflow, locale);

        final Map<String, Object> fields = document.getFields();
        assertThat(fields.get("present-string-field"), equalTo("Present String Field"));
        assertThat("absent string field is not present", !fields.containsKey("absent-string-field"));
        assertThat(fields.get("present-multiline-string-field"), equalTo("Present Multiline Sting Field"));
        assertThat(((List<String>)fields.get("present-multiple-string-field")).size(), equalTo(3));
        assertThat(((List<String>)fields.get("present-multiple-string-field")).get(0), equalTo("one"));
        assertThat(((List<String>)fields.get("present-multiple-string-field")).get(1), equalTo("two"));
        assertThat("empty multiple string field is not present", !fields.containsKey("empty-multiple-string-field"));
        assertThat("absent multiple string field is not present", !fields.containsKey("absent-multiple-string-field"));
    }
    */
}
