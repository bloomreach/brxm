/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response.Status;

import org.hippoecm.hst.platform.api.ChannelService;
import org.hippoecm.hst.platform.api.PlatformServices;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.DocumentWorkflowAction;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.standardworkflow.JcrTemplateNode;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.hippoecm.repository.util.WorkflowUtils.Variant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.TestUserContext;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.DocumentInfo;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.model.NewDocumentInfo;
import org.onehippo.cms.channelmanager.content.document.model.PublicationState;
import org.onehippo.cms.channelmanager.content.document.util.BranchingService;
import org.onehippo.cms.channelmanager.content.document.util.DocumentLocaleUtils;
import org.onehippo.cms.channelmanager.content.document.util.DocumentNameUtils;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.document.util.FolderUtils;
import org.onehippo.cms.channelmanager.content.document.util.HintsInspector;
import org.onehippo.cms.channelmanager.content.document.util.PublicationStateUtils;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.documenttype.validation.ValidationUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ConflictException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.MethodNotAllowed;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
        "com.sun.org.apache.xalan.*",
        "com.sun.org.apache.xerces.*",
        "javax.activation.*",
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.xml.*",
        "org.apache.logging.log4j.*",
        "org.w3c.dom.*",
        "org.xml.*",
})
@PrepareForTest({
        DocumentLocaleUtils.class,
        DocumentNameUtils.class,
        DocumentTypesService.class,
        DocumentUtils.class,
        EditingUtils.class,
        FieldTypeUtils.class,
        FolderUtils.class,
        JcrUtils.class,
        PublicationStateUtils.class,
        ValidationUtils.class,
        WorkflowUtils.class,
})
public class DocumentsServiceImplTest {

    private Session session;
    private UserContext userContext;
    private NewDocumentInfo info;
    private DocumentsServiceImpl documentsService;
    private HintsInspector hintsInspector;
    private BranchingService branchingService;
    private PlatformServices platformServices;

    @Before
    public void setup() {
        userContext = new TestUserContext();
        session = userContext.getSession();
        hintsInspector = createMock(HintsInspector.class);
        documentsService = new DocumentsServiceImpl(){
            @Override
            SaveDraftDocumentService getJcrSaveDraftDocumentService(final String uuid, final String branchId
                    , final UserContext userContext) {
                return new SaveDraftDocumentService(){

                    @Override
                    public boolean canEditDraft() {
                        return false;
                    }

                    @Override
                    public boolean shouldSaveDraft(final Document document) {
                        return false;
                    }

                    @Override
                    public Document editDraft() {
                        return null;
                    }

                    @Override
                    public DocumentInfo addDocumentInfo(final Document document) {
                        return null;
                    }

                    @Override
                    public Document saveDraft(final Document document) {
                        return null;
                    }
                };
            }

        };
        documentsService.setHintsInspector(hintsInspector);
        branchingService = createMock(BranchingService.class);
        documentsService.setBranchingService(branchingService);

        platformServices = createMock(PlatformServices.class);
        HippoServiceRegistry.register(platformServices, PlatformServices.class);

        PowerMock.mockStatic(DocumentNameUtils.class);
        PowerMock.mockStatic(DocumentLocaleUtils.class);
        PowerMock.mockStatic(PublicationStateUtils.class);
        PowerMock.mockStatic(DocumentTypesService.class);
        PowerMock.mockStatic(DocumentUtils.class);
        PowerMock.mockStatic(EditingUtils.class);
        PowerMock.mockStatic(FieldTypeUtils.class);
        PowerMock.mockStatic(FolderUtils.class);
        PowerMock.mockStatic(JcrUtils.class);
        PowerMock.mockStatic(ValidationUtils.class);
        PowerMock.mockStatic(WorkflowUtils.class);

        info = new NewDocumentInfo();
        info.setName("Breaking News"); // the name needs to be display-name-encoded by the backend
        info.setSlug("breaking news"); // the slug needs to be URI-encoded by the backend, e.g. to "breaking-news"
        info.setDocumentTemplateQuery("new-news-document");
        info.setDocumentTypeId("project:newsdocument");
        info.setRootPath("/content/documents/channel/news");
    }

    @After
    public void tearDown() {
        HippoServiceRegistry.unregister(platformServices, PlatformServices.class);
    }

    @Test
    public void obtainEditableDocumentNotAHandle() {
        final String uuid = "uuid";
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());
        replayAll();

        try {
            documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST);
        }

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentNoNodeType() {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST);
        }

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentDeleted() {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("hippo:deleted"));

        replayAll();

        try {
            documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST);
        }

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentNoWorkflow() {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Display Name"));
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            assertErrorStatusAndReason(e, Status.METHOD_NOT_ALLOWED, Reason.NOT_A_DOCUMENT,
                    Collections.singletonMap("displayName", "Display Name"));
        }

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentNotEditable() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canObtainEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(MASTER_BRANCH_ID, emptyMap(), session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertErrorStatusAndReason(e, Status.FORBIDDEN, Reason.SERVER_ERROR);
        }

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentOtherHolder() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(PublicationStateUtils.getPublicationStateFromHandle(handle)).andReturn(PublicationState.CHANGED);
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andStubReturn(emptyMap());
        expect(hintsInspector.canObtainEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(MASTER_BRANCH_ID, emptyMap(), session))
                .andReturn(Optional.of(new ErrorInfo(Reason.OTHER_HOLDER)));

        replayAll();

        try {
            documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertErrorStatusAndReason(e, Status.FORBIDDEN, Reason.OTHER_HOLDER,
                    Collections.singletonMap("publicationState", PublicationState.CHANGED));
        }

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentNoDocumentNodeType() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:nodetype"));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap());
        expect(hintsInspector.canObtainEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);

        replayAll();

        try {
            documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.DOES_NOT_EXIST);
        }

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentNoDocumentType() throws Exception {
        final String uuid = "uuid";
        final String variantType = "project:newsdocument";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final DocumentTypesService documentTypesService = createMock(DocumentTypesService.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of(variantType)).anyTimes();
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap());
        expect(hintsInspector.canObtainEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        expect(DocumentTypesService.get()).andReturn(documentTypesService);
        expect(JcrUtils.getNodePathQuietly(handle)).andReturn("/bla");

        expect(documentTypesService.getDocumentType(variantType, userContext)).andThrow(new NotFoundException());

        replayAll();

        try {
            documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR);
        }

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentUnknownValidator() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Display Name"));
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap());
        expect(hintsInspector.canObtainEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);

        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(true);

        replayAll();

        try {
            documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertErrorStatusAndReason(e, Status.FORBIDDEN, Reason.CREATE_WITH_UNSUPPORTED_VALIDATOR,
                    Collections.singletonMap("displayName", "Display Name"));
        }

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentFailed() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canObtainEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        expect(EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, userContext.getSession())).andReturn(Optional.empty());

        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);

        replayAll();

        try {
            documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertErrorStatusAndReason(e, Status.FORBIDDEN, Reason.SERVER_ERROR);
        }

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentSuccess() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final Node unpublished = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final List<FieldType> fields = Collections.emptyList();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Display Name"));
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, userContext.getSession())).andReturn(Optional.of(draft));
        expect(PublicationStateUtils.getPublicationStateFromVariant(draft)).andReturn(PublicationState.NEW);
        expect(DocumentLocaleUtils.getDocumentLocale(draft)).andReturn("en");
        expect(workflow.hints(anyString())).andReturn(emptyMap());
        expect(hintsInspector.canObtainEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);

        FieldTypeUtils.readFieldValues(eq(draft), eq(fields), isA(Map.class));
        expectLastCall();

        expect(docType.getId()).andReturn("document:type");
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields).anyTimes();

        expect(WorkflowUtils.getDocumentVariantNode(eq(handle), eq(Variant.UNPUBLISHED))).andReturn(Optional.of(unpublished));
        expect(JcrUtils.getNodeNameQuietly(eq(handle))).andReturn("url-name");
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/test/url-name");

        FieldTypeUtils.readFieldValues(eq(unpublished), eq(fields), isA(Map.class));
        expectLastCall();

        expect(EditingUtils.isHintActionTrue(emptyMap(), EditingUtils.HINT_PUBLISH)).andReturn(false);
        expect(EditingUtils.isHintActionTrue(emptyMap(), EditingUtils.HINT_REQUEST_PUBLICATION)).andReturn(false);

        expect(draft.getIdentifier()).andReturn("draftUuid").anyTimes();
        expect(JcrUtils.getStringProperty(eq(draft), anyString(), eq(null))).andReturn("draft");
        expect(JcrUtils.getStringProperty(eq(draft), eq(HIPPO_PROPERTY_BRANCH_ID), eq(MASTER_BRANCH_ID))).andReturn(MASTER_BRANCH_ID);

        replayAll(draft);

        final Document document = documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
        assertThat(document.getId(), equalTo(uuid));
        assertThat(document.getVariantId(), equalTo("draftUuid"));
        assertThat(document.getUrlName(), equalTo("url-name"));
        assertThat(document.getDisplayName(), equalTo("Display Name"));
        assertThat(document.getRepositoryPath(), equalTo("/content/documents/test/url-name"));
        assertThat(document.getInfo().getType().getId(), equalTo("document:type"));
        assertThat(document.getInfo().isDirty(), equalTo(false));
        assertThat(document.getInfo().isCanPublish(), equalTo(false));
        assertThat(document.getInfo().isCanRequestPublication(), equalTo(false));
        assertThat(document.getInfo().getPublicationState(), equalTo(PublicationState.NEW));
        assertThat(document.getInfo().getLocale(), equalTo("en"));

        verifyAll();
    }

    @Test
    public void obtainEditableDocumentCanPublish() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final Node unpublished = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final List<FieldType> fields = Collections.emptyList();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Display Name"));
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, userContext.getSession())).andReturn(Optional.of(draft));
        expect(PublicationStateUtils.getPublicationStateFromVariant(draft)).andReturn(PublicationState.NEW);
        expect(DocumentLocaleUtils.getDocumentLocale(draft)).andReturn("en");
        expect(workflow.hints(anyString())).andReturn(emptyMap());
        expect(hintsInspector.canObtainEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        FieldTypeUtils.readFieldValues(eq(draft), eq(fields), isA(Map.class));
        expectLastCall();

        expect(docType.getId()).andReturn("document:type");
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields).anyTimes();

        expect(WorkflowUtils.getDocumentVariantNode(eq(handle), eq(Variant.UNPUBLISHED))).andReturn(Optional.of(unpublished));
        expect(JcrUtils.getNodeNameQuietly(eq(handle))).andReturn("url-name");
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/test/url-name");
        FieldTypeUtils.readFieldValues(eq(unpublished), eq(fields), isA(Map.class));
        expect(EditingUtils.isHintActionTrue(emptyMap(), EditingUtils.HINT_PUBLISH)).andReturn(true);
        expect(EditingUtils.isHintActionTrue(emptyMap(), EditingUtils.HINT_REQUEST_PUBLICATION)).andReturn(true);
        expectLastCall();

        expect(draft.getIdentifier()).andReturn("draftUuid").anyTimes();
        expect(JcrUtils.getStringProperty(eq(draft), anyString(), eq(null))).andReturn("draft");
        expect(JcrUtils.getStringProperty(eq(draft), eq(HIPPO_PROPERTY_BRANCH_ID), eq(MASTER_BRANCH_ID))).andReturn(MASTER_BRANCH_ID);
        replayAll(draft);

        final Document document = documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
        assertThat(document.getId(), equalTo(uuid));
        assertThat(document.getVariantId(), equalTo("draftUuid"));
        assertThat(document.getUrlName(), equalTo("url-name"));
        assertThat(document.getDisplayName(), equalTo("Display Name"));
        assertThat(document.getRepositoryPath(), equalTo("/content/documents/test/url-name"));
        assertThat(document.getInfo().getType().getId(), equalTo("document:type"));
        assertThat(document.getInfo().isDirty(), equalTo(false));
        assertThat(document.getInfo().isCanPublish(), equalTo(true));
        assertThat(document.getInfo().isCanRequestPublication(), equalTo(true));
        assertThat(document.getInfo().getPublicationState(), equalTo(PublicationState.NEW));
        assertThat(document.getInfo().getLocale(), equalTo("en"));

        verifyAll();
    }

    @Test
    public void obtainDirtyEditableDocument() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final Node unpublished = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final List<FieldType> fields = Collections.emptyList();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));

        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Display Name"));
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.of(workflow));
        expect(EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, userContext.getSession())).andReturn(Optional.of(draft));
        expect(PublicationStateUtils.getPublicationStateFromVariant(draft)).andReturn(PublicationState.NEW);
        expect(DocumentLocaleUtils.getDocumentLocale(draft)).andReturn("en");
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canObtainEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        FieldTypeUtils.readFieldValues(eq(draft), eq(fields), isA(Map.class));
        expectLastCall();

        expect(docType.getId()).andReturn("document:type");
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields);

        expect(WorkflowUtils.getDocumentVariantNode(eq(handle), eq(Variant.UNPUBLISHED))).andReturn(Optional.of(unpublished));
        expect(JcrUtils.getNodeNameQuietly(eq(handle))).andReturn("url-name");
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/test/url-name");
        expect(docType.getFields()).andReturn(fields);

        FieldTypeUtils.readFieldValues(eq(unpublished), eq(fields), isA(Map.class));
        expectLastCall().andAnswer(() -> ((Map) getCurrentArguments()[2]).put("extraField", new FieldValue("value")));

        expect(EditingUtils.isHintActionTrue(emptyMap(), EditingUtils.HINT_PUBLISH)).andReturn(false);
        expect(EditingUtils.isHintActionTrue(emptyMap(), EditingUtils.HINT_REQUEST_PUBLICATION)).andReturn(false);

        expect(draft.getIdentifier()).andReturn("draftUuid").anyTimes();
        expect(JcrUtils.getStringProperty(eq(draft), anyString(), eq(null))).andReturn("draft");
        expect(JcrUtils.getStringProperty(eq(draft), eq(HIPPO_PROPERTY_BRANCH_ID), eq(MASTER_BRANCH_ID))).andReturn(MASTER_BRANCH_ID);
        replayAll(draft);

        final Document document = documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
        assertThat(document.getId(), equalTo(uuid));
        assertThat(document.getVariantId(), equalTo("draftUuid"));
        assertThat(document.getUrlName(), equalTo("url-name"));
        assertThat(document.getDisplayName(), equalTo("Display Name"));
        assertThat(document.getRepositoryPath(), equalTo("/content/documents/test/url-name"));
        assertThat(document.getInfo().getType().getId(), equalTo("document:type"));
        assertThat(document.getInfo().isDirty(), equalTo(true));
        assertThat(document.getInfo().getPublicationState(), equalTo(PublicationState.NEW));
        assertThat(document.getInfo().getLocale(), equalTo("en"));

        verifyAll();
    }

    @Test
    public void updateEditableDocumentNotAHandle() {
        final Document document = new Document();
        final String uuid = "uuid";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateEditableDocument(uuid, document, userContext);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST);
        }

        verifyAll();
    }

    @Test
    public void updateEditableDocumentNoWorkflow() {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateEditableDocument(uuid, document, userContext);
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            assertErrorStatusAndReason(e, Status.METHOD_NOT_ALLOWED, Reason.NOT_A_DOCUMENT);
        }

        verifyAll();
    }

    @Test
    public void updateEditableDocumentDraftVariantNotFound() {
        final Document document = new Document();
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateEditableDocument(uuid, document, userContext);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST);
        }

        verifyAll();
    }

    @Test
    public void updateEditableDocumentNotEditing() throws Exception {
        final Document document = new Document();
        document.setBranchId(MASTER_BRANCH_ID);
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(MASTER_BRANCH_ID, emptyMap(), session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateEditableDocument(uuid, document, userContext);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertErrorStatusAndReason(e, Status.FORBIDDEN, Reason.NO_HOLDER);
        }

        verifyAll();
    }

    @Test
    public void updateEditableDocumentOtherHolder() throws Exception {
        final Document document = new Document();
        document.setBranchId(MASTER_BRANCH_ID);
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(MASTER_BRANCH_ID, emptyMap(), session))
                .andReturn(Optional.of(new ErrorInfo(Reason.OTHER_HOLDER)));

        replayAll();

        try {
            documentsService.updateEditableDocument(uuid, document, userContext);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertErrorStatusAndReason(e, Status.FORBIDDEN, Reason.OTHER_HOLDER);
        }

        verifyAll();
    }

    @Test
    public void updateEditableDocumentNoDocumentType() throws Exception {
        final Document document = new Document();
        document.setBranchId(MASTER_BRANCH_ID);
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap());
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);

        replayAll();

        try {
            documentsService.updateEditableDocument(uuid, document, userContext);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.DOES_NOT_EXIST);
        }

        verifyAll();
    }

    @Test
    public void updateEditableDocumentUnknownValidator() throws Exception {
        final Document document = new Document();
        document.setBranchId(MASTER_BRANCH_ID);
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);

        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(true);

        replayAll();

        try {
            documentsService.updateEditableDocument(uuid, document, userContext);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertErrorStatusAndReason(e, Status.FORBIDDEN, Reason.SAVE_WITH_UNSUPPORTED_VALIDATOR);
        }

        verifyAll();
    }

    @Test
    public void updateEditableDocumentWriteFailure() throws Exception {
        final Document document = new Document();
        document.setBranchId(MASTER_BRANCH_ID);
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final BadRequestException badRequest = new BadRequestException();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall().andThrow(badRequest);

        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);

        expect(docType.getFields()).andReturn(Collections.emptyList());

        replayAll();

        try {
            documentsService.updateEditableDocument(uuid, document, userContext);
            fail("No Exception");
        } catch (final BadRequestException e) {
            assertThat(e, equalTo(badRequest));
        }

        verifyAll();
    }

    @Test
    public void updateEditableDocumentSaveFailure() throws Exception {
        final Document document = new Document();
        document.setBranchId(MASTER_BRANCH_ID);
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();

        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());
        session.save();
        expectLastCall().andThrow(new RepositoryException());

        replayAll();

        try {
            documentsService.updateEditableDocument(uuid, document, userContext);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR);
        }

        verifyAll();
    }

    @Test
    public void updateEditableDocumentValidationFailure() throws Exception {
        final Document document = new Document();
        document.setBranchId(MASTER_BRANCH_ID);
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);

        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        expect(ValidationUtils.validateDocument(eq(document), eq(docType), eq(draft), eq(userContext))).andReturn(false);

        replayAll();

        try {
            documentsService.updateEditableDocument(uuid, document, userContext);
            fail("No Exception");
        } catch (final BadRequestException e) {
            assertThat(e.getStatus(), is(Status.BAD_REQUEST));
            assertThat(e.getPayload(), is(document));
        }

        verifyAll();
    }

    @Test
    public void updateEditableDocumentCopyToPreviewFailure() throws Exception {
        final Document document = new Document();
        document.setBranchId(MASTER_BRANCH_ID);
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        workflow.commitEditableInstance();
        expectLastCall().andThrow(new WorkflowException("bla"));

        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        expect(hintsInspector.determineEditingFailure(MASTER_BRANCH_ID, emptyMap(), session)).andReturn(Optional.empty());
        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();

        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();
        expect(ValidationUtils.validateDocument(eq(document), eq(docType), eq(draft), eq(userContext))).andReturn(true);

        replayAll();

        try {
            documentsService.updateEditableDocument(uuid, document, userContext);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.NO_HOLDER);
        }

        verifyAll();
    }

    @Test
    public void updateEditableDocumentSuccess() throws Exception {
        final Document document = new Document();
        document.setBranchId(MASTER_BRANCH_ID);
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow)).times(1);
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.of(workflow)).times(1);
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        expect(workflow.commitEditableInstance()).andReturn(null);

        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();

        expect(EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, userContext.getSession())).andReturn(Optional.of(draft));
        expect(PublicationStateUtils.getPublicationStateFromVariant(draft)).andReturn(PublicationState.CHANGED);
        expect(docType.getFields()).andReturn(Collections.emptyList());

        FieldTypeUtils.readFieldValues(draft, Collections.emptyList(), document.getFields());
        expectLastCall();

        expect(EditingUtils.isHintActionTrue(emptyMap(), EditingUtils.HINT_PUBLISH)).andReturn(false);
        expect(EditingUtils.isHintActionTrue(emptyMap(), EditingUtils.HINT_REQUEST_PUBLICATION)).andReturn(false);

        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        expect(ValidationUtils.validateDocument(eq(document), eq(docType), eq(draft), eq(userContext))).andReturn(true);

        replayAll();

        final Document persistedDocument = documentsService.updateEditableDocument(uuid, document, userContext);

        assertThat(persistedDocument, equalTo(document));
        assertThat(persistedDocument.getInfo().getPublicationState(), equalTo(PublicationState.CHANGED));

        verifyAll();
    }

    @Test
    public void updateDirtyEditableDocumentSuccess() throws Exception {
        final Document document = new Document();
        document.setBranchId(MASTER_BRANCH_ID);
        document.getInfo().setDirty(true);

        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow)).times(1);
        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andReturn(Optional.of(workflow)).times(1);
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        expect(workflow.commitEditableInstance()).andReturn(null);

        FieldTypeUtils.writeFieldValues(document.getFields(), Collections.emptyList(), draft);
        expectLastCall();
        expect(EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, userContext.getSession())).andReturn(Optional.of(draft));
        expect(PublicationStateUtils.getPublicationStateFromVariant(draft)).andReturn(PublicationState.CHANGED);
        expect(docType.getFields()).andReturn(Collections.emptyList());

        FieldTypeUtils.readFieldValues(draft, Collections.emptyList(), document.getFields());
        expectLastCall();

        expect(EditingUtils.isHintActionTrue(emptyMap(), EditingUtils.HINT_PUBLISH)).andReturn(false);
        expect(EditingUtils.isHintActionTrue(emptyMap(), EditingUtils.HINT_REQUEST_PUBLICATION)).andReturn(false);

        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList()).anyTimes();
        session.save();
        expectLastCall();

        expect(ValidationUtils.validateDocument(eq(document), eq(docType), eq(draft), eq(userContext))).andReturn(true);

        replayAll();

        final Document persistedDocument = documentsService.updateEditableDocument(uuid, document, userContext);

        assertThat(persistedDocument.getId(), equalTo(document.getId()));
        assertThat(persistedDocument.getDisplayName(), equalTo(document.getDisplayName()));
        assertThat(persistedDocument.getFields(), equalTo(document.getFields()));
        assertThat(persistedDocument.getInfo().isDirty(), equalTo(false));
        assertThat(persistedDocument.getInfo().getPublicationState(), equalTo(PublicationState.CHANGED));

        verifyAll();
    }

    @Test
    public void updateEditableFieldNotAHandle() {
        final String uuid = "uuid";
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateEditableField(uuid, MASTER_BRANCH_ID, fieldPath, fieldValues, userContext);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST);
        }

        verifyAll();
    }

    @Test
    public void updateEditableFieldNoWorkflow() {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateEditableField(uuid, MASTER_BRANCH_ID, fieldPath, fieldValues, userContext);
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            assertErrorStatusAndReason(e, Status.METHOD_NOT_ALLOWED, Reason.NOT_A_DOCUMENT);
        }

        verifyAll();
    }

    @Test
    public void updateEditableFieldVariantNotFound() {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateEditableField(uuid, MASTER_BRANCH_ID, fieldPath, fieldValues, userContext);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST);
        }

        verifyAll();
    }

    @Test
    public void updateEditableFieldNotEditing() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(MASTER_BRANCH_ID, emptyMap(), session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.updateEditableField(uuid, MASTER_BRANCH_ID, fieldPath, fieldValues, userContext);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertErrorStatusAndReason(e, Status.FORBIDDEN, Reason.NO_HOLDER);
        }

        verifyAll();
    }

    @Test
    public void updateEditableFieldOtherHolder() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(false);
        expect(hintsInspector.determineEditingFailure(MASTER_BRANCH_ID, emptyMap(), session))
                .andReturn(Optional.of(new ErrorInfo(Reason.OTHER_HOLDER)));

        replayAll();

        try {
            documentsService.updateEditableField(uuid, MASTER_BRANCH_ID, fieldPath, fieldValues, userContext);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertErrorStatusAndReason(e, Status.FORBIDDEN, Reason.OTHER_HOLDER);
        }

        verifyAll();
    }

    @Test
    public void updateEditableFieldNoDocumentType() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);

        replayAll();

        try {
            documentsService.updateEditableField(uuid, MASTER_BRANCH_ID, fieldPath, fieldValues, userContext);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.DOES_NOT_EXIST);
        }

        verifyAll();
    }

    @Test
    public void updateEditableFieldUnknownValidator() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);

        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(true);

        replayAll();

        try {
            documentsService.updateEditableField(uuid, MASTER_BRANCH_ID, fieldPath, fieldValues, userContext);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertErrorStatusAndReason(e, Status.FORBIDDEN, Reason.SAVE_WITH_UNSUPPORTED_VALIDATOR);
        }

        verifyAll();
    }

    @Test
    public void updateEditableFieldWriteFailure() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldType> fields = Collections.emptyList();
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));
        final BadRequestException badRequest = new BadRequestException();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());

        expect(FieldTypeUtils.writeFieldValue(eq(fieldPath), eq(fieldValues), eq(fields), anyObject(CompoundContext.class))).andThrow(badRequest);

        replayAll();

        try {
            documentsService.updateEditableField(uuid, MASTER_BRANCH_ID, fieldPath, fieldValues, userContext);
            fail("No Exception");
        } catch (final BadRequestException e) {
            assertThat(e, equalTo(badRequest));
        }

        verifyAll();
    }

    @Test
    public void updateEditableFieldNotSavedWhenUnknown() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));
        final List<FieldType> fields = Collections.emptyList();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap());
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields);
        expect(FieldTypeUtils.writeFieldValue(eq(fieldPath), eq(fieldValues), eq(fields), anyObject(CompoundContext.class))).andReturn(false);

        replayAll();

        try {
            documentsService.updateEditableField(uuid, MASTER_BRANCH_ID, fieldPath, fieldValues, userContext);
            fail("Expected a BadRequestException");
        } catch (BadRequestException expected) {
            assertErrorStatusAndReason(expected, Status.BAD_REQUEST, Reason.INVALID_DATA);
            verifyAll();
        }
    }

    @Test
    public void updateEditableFieldSuccess() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));
        final List<FieldType> fields = Collections.emptyList();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(fields);
        expect(FieldTypeUtils.writeFieldValue(eq(fieldPath), eq(fieldValues), eq(fields), anyObject(CompoundContext.class))).andReturn(true);

        session.save();
        expectLastCall();

        replayAll();

        documentsService.updateEditableField(uuid, MASTER_BRANCH_ID, fieldPath, fieldValues, userContext);

        verifyAll();
    }

    @Test
    public void updateEditableFieldSaveFailure() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node draft = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final DocumentType docType = provideDocumentType(handle);
        final FieldPath fieldPath = new FieldPath("ns:field");
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("drafted value"));
        final List<FieldType> fields = Collections.emptyList();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(WorkflowUtils.getDocumentVariantNode(handle, Variant.DRAFT)).andReturn(Optional.of(draft));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canUpdateDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getFields()).andReturn(Collections.emptyList());
        expect(FieldTypeUtils.writeFieldValue(eq(fieldPath), eq(fieldValues), eq(fields), anyObject(CompoundContext.class))).andReturn(true);

        session.save();
        expectLastCall().andThrow(new RepositoryException());

        replayAll();

        try {
            documentsService.updateEditableField(uuid, MASTER_BRANCH_ID, fieldPath, fieldValues, userContext);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR);
        }

        verifyAll();
    }

    @Test
    public void discardEditableDocumentNotAHandle() {
        final String uuid = "uuid";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.discardEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST);
        }

        verifyAll();
    }

    @Test
    public void discardEditableDocumentNoWorkflow() {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.discardEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            assertErrorStatusAndReason(e, Status.METHOD_NOT_ALLOWED, Reason.NOT_A_DOCUMENT);
        }

        verifyAll();
    }

    @Test
    public void discardEditableDocumentNotDeletable() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap());
        expect(hintsInspector.canDisposeEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(false);

        replayAll();

        try {
            documentsService.discardEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final ForbiddenException e) {
            assertErrorStatusAndReason(e, Status.FORBIDDEN, Reason.ALREADY_DELETED);
        }

        verifyAll();
    }

    @Test
    public void discardEditableDocumentDisposeFailure() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canDisposeEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);

        expect(workflow.disposeEditableInstance()).andThrow(new WorkflowException("bla"));

        replayAll();

        try {
            documentsService.discardEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR);
        }

        verifyAll();
    }

    @Test
    public void discardEditableDocumentSuccess() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap()).atLeastOnce();
        expect(hintsInspector.canDisposeEditableDocument(MASTER_BRANCH_ID, emptyMap())).andReturn(true);

        expect(workflow.disposeEditableInstance()).andReturn(null);

        replayAll();

        documentsService.discardEditableDocument(uuid, MASTER_BRANCH_ID, userContext);

        verifyAll();
    }


    @Test(expected = BadRequestException.class)
    public void createDocumentWithoutName() {
        info.setName("");
        documentsService.createDocument(info, userContext);
    }

    @Test(expected = BadRequestException.class)
    public void createDocumentWithoutSlug() {
        info.setSlug("");
        documentsService.createDocument(info, userContext);
    }

    @Test(expected = BadRequestException.class)
    public void createDocumentWithoutDocumentTemplateQuery() {
        info.setDocumentTemplateQuery("");
        documentsService.createDocument(info, userContext);
    }

    @Test(expected = BadRequestException.class)
    public void createDocumentWithoutDocumentTypeId() {
        info.setDocumentTypeId("");
        documentsService.createDocument(info, userContext);
    }

    @Test(expected = BadRequestException.class)
    public void createDocumentWithoutRootPath() {
        info.setRootPath("");
        documentsService.createDocument(info, userContext);
    }

    @Test
    public void createDocumentWithExistingName() {
        final Node folderNode = createMock(Node.class);
        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(true);

        replayAll();

        try {
            documentsService.createDocument(info, userContext);
            fail("No Exception");
        } catch (final ConflictException e) {
            assertErrorStatusAndReason(e, Status.CONFLICT, Reason.NAME_ALREADY_EXISTS);
        }

        verifyAll();
    }

    @Test
    public void createDocumentWithExistingSlug() {
        final Node folderNode = createMock(Node.class);
        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(true);

        replayAll();

        try {
            documentsService.createDocument(info, userContext);
            fail("No Exception");
        } catch (final ConflictException e) {
            assertErrorStatusAndReason(e, Status.CONFLICT, Reason.SLUG_ALREADY_EXISTS);
        }

        verifyAll();
    }

    @Test
    public void createDocumentNoWorkflow() throws Exception {
        final Node folderNode = createMock(Node.class);
        final Node documentHandle = createMock(Node.class);

        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);

        final DocumentType docType = provideDocumentType(documentHandle);
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.empty());
        expect(DocumentUtils.getDisplayName(folderNode))
                .andReturn(Optional.of("News"));

        replayAll();

        try {
            documentsService.createDocument(info, userContext);
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            assertErrorStatusAndReason(e, Status.METHOD_NOT_ALLOWED, Reason.NOT_A_FOLDER,
                    Collections.singletonMap("displayName", "News"));
        }

        verifyAll();
    }

    @Test(expected = InternalServerErrorException.class)
    public void createDocumentWorkflowThrowsException() throws Exception {
        final Node folderNode = createMock(Node.class);
        final Node documentHandle = createMock(Node.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        final DocumentType docType = provideDocumentType(documentHandle);
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.of(folderWorkflow));
        expect(folderWorkflow.add(eq("new-news-document"), eq("project:newsdocument"), eq("breaking-news"), eq(null)))
                .andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(folderNode)).andReturn("/content/documents/channel/news");

        replayAll();

        documentsService.createDocument(info, userContext);
    }

    @Test
    public void createDocumentInRootPath() throws Exception {
        final Node folderNode = createMock(Node.class);
        final Node documentHandle = createMock(Node.class);
        final Node documentDraft = createMock(Node.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.of(folderWorkflow));
        expect(folderWorkflow.add(eq("new-news-document"), eq("project:newsdocument"), eq("breaking-news"), eq(null)))
                .andReturn("/content/documents/channel/news/breaking-news/breaking-news");
        expect(session.getNode(eq("/content/documents/channel/news/breaking-news/breaking-news")))
                .andReturn(documentDraft);
        expect(documentDraft.getParent()).andReturn(documentHandle);

        DocumentNameUtils.setDisplayName(eq(documentHandle), eq("Breaking News (encoded)"));
        expectLastCall();

        expect(WorkflowUtils.getDocumentVariantNode(eq(documentHandle), eq(Variant.DRAFT)))
                .andReturn(Optional.of(documentDraft));

        expect(documentHandle.getIdentifier()).andReturn("uuid");

        final DocumentType docType = provideDocumentType(documentHandle);
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getId()).andReturn("project:newsdocument");
        expect(DocumentUtils.getDisplayName(documentHandle)).andReturn(Optional.of("Breaking News (encoded)"));
        expect(JcrUtils.getNodeNameQuietly(eq(documentHandle))).andReturn("breaking-news");
        expect(JcrUtils.getNodePathQuietly(eq(documentHandle))).andReturn("/content/documents/news/breaking-news");
        expect(PublicationStateUtils.getPublicationStateFromVariant(documentDraft)).andReturn(PublicationState.LIVE);
        expect(DocumentLocaleUtils.getDocumentLocale(documentDraft)).andReturn("en");

        session.save();
        expectLastCall();

        final List<FieldType> fields = Collections.emptyList();
        expect(docType.getFields()).andReturn(fields);
        FieldTypeUtils.readFieldValues(eq(documentDraft), eq(fields), isA(Map.class));
        expectLastCall();

        expect(documentDraft.getIdentifier()).andReturn("draftUuid").anyTimes();
        expect(JcrUtils.getStringProperty(eq(documentDraft), anyString(), eq(null))).andReturn("draft");
        expect(JcrUtils.getStringProperty(eq(documentDraft), eq(HIPPO_PROPERTY_BRANCH_ID), eq(MASTER_BRANCH_ID))).andReturn(MASTER_BRANCH_ID);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        expect(WorkflowUtils.getWorkflow(eq(documentHandle), eq("default"), eq(DocumentWorkflow.class)))
                .andReturn(Optional.of(documentWorkflow));
        expect(documentWorkflow.hints()).andReturn(new HashMap<>());
        replayAll(documentDraft);

        final Document document = documentsService.createDocument(info, userContext);

        assertThat(document.getId(), equalTo("uuid"));
        assertThat(document.getVariantId(), equalTo("draftUuid"));
        assertThat(document.getUrlName(), equalTo("breaking-news"));
        assertThat(document.getDisplayName(), equalTo("Breaking News (encoded)"));
        assertThat(document.getRepositoryPath(), equalTo("/content/documents/news/breaking-news"));
        assertThat(document.getInfo().getPublicationState(), equalTo(PublicationState.LIVE));
        assertThat(document.getInfo().getLocale(), equalTo("en"));
        assertThat(document.getFields().size(), equalTo(0));

        verifyAll();
    }

    @Test
    public void createDocumentInDefaultPath() throws Exception {
        final Node rootFolderNode = createMock(Node.class);
        final Node folderNode = createMock(Node.class);
        final Node documentHandle = createMock(Node.class);
        final Node documentDraft = createMock(Node.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        info.setDefaultPath("2017/11");
        info.setFolderTemplateQuery("folderTemplateQuery");

        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(rootFolderNode);
        expect(FolderUtils.getOrCreateFolder(eq(rootFolderNode), eq("2017/11"), eq(session), eq("folderTemplateQuery")))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.of(folderWorkflow));
        expect(folderWorkflow.add(eq("new-news-document"), eq("project:newsdocument"), eq("breaking-news"), eq(null)))
                .andReturn("/content/documents/channel/news/breaking-news/breaking-news");
        expect(session.getNode(eq("/content/documents/channel/news/breaking-news/breaking-news")))
                .andReturn(documentDraft);
        expect(documentDraft.getParent()).andReturn(documentHandle);

        DocumentNameUtils.setDisplayName(eq(documentHandle), eq("Breaking News (encoded)"));
        expectLastCall();

        expect(WorkflowUtils.getDocumentVariantNode(eq(documentHandle), eq(Variant.DRAFT)))
                .andReturn(Optional.of(documentDraft));
        expect(documentHandle.getIdentifier()).andReturn("uuid");

        final DocumentType docType = provideDocumentType(documentHandle);
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getId()).andReturn("project:newsdocument");
        expect(DocumentUtils.getDisplayName(documentHandle)).andReturn(Optional.of("Breaking News (encoded)"));
        expect(JcrUtils.getNodeNameQuietly(eq(documentHandle))).andReturn("breaking-news");
        expect(JcrUtils.getNodePathQuietly(eq(documentHandle))).andReturn("/content/documents/news/breaking-news");
        expect(PublicationStateUtils.getPublicationStateFromVariant(documentDraft)).andReturn(PublicationState.NEW);
        expect(DocumentLocaleUtils.getDocumentLocale(documentDraft)).andReturn("en");

        session.save();
        expectLastCall();

        final List<FieldType> fields = Collections.emptyList();
        expect(docType.getFields()).andReturn(fields);
        FieldTypeUtils.readFieldValues(eq(documentDraft), eq(fields), isA(Map.class));
        expectLastCall();

        expect(documentDraft.getIdentifier()).andReturn("draftUuid").anyTimes();
        expect(JcrUtils.getStringProperty(eq(documentDraft), anyString(), eq(null))).andReturn("draft");
        expect(JcrUtils.getStringProperty(eq(documentDraft), eq(HIPPO_PROPERTY_BRANCH_ID), eq(MASTER_BRANCH_ID))).andReturn(MASTER_BRANCH_ID);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        expect(WorkflowUtils.getWorkflow(eq(documentHandle), eq("default"), eq(DocumentWorkflow.class)))
                .andReturn(Optional.of(documentWorkflow));
        expect(documentWorkflow.hints()).andReturn(new HashMap<>());
        replayAll(documentDraft);


        final Document document = documentsService.createDocument(info, userContext);

        assertThat(document.getId(), equalTo("uuid"));
        assertThat(document.getVariantId(), equalTo("draftUuid"));
        assertThat(document.getUrlName(), equalTo("breaking-news"));
        assertThat(document.getDisplayName(), equalTo("Breaking News (encoded)"));
        assertThat(document.getRepositoryPath(), equalTo("/content/documents/news/breaking-news"));
        assertThat(document.getFields().size(), equalTo(0));
        assertThat(document.getInfo().getPublicationState(), equalTo(PublicationState.NEW));
        assertThat(document.getInfo().getLocale(), equalTo("en"));

        verifyAll();
    }

    @Test(expected = InternalServerErrorException.class)
    public void createXPageInFolderWithoutChannelIDThrowsException() throws Exception {
        info.setLayout("xpage-layout");

        final Node folderNode = createMock(Node.class);
        final Node documentHandle = createMock(Node.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        final DocumentType docType = provideDocumentType(documentHandle);
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.of(folderWorkflow));
        expect(folderWorkflow.add(eq("new-news-document"), eq("project:newsdocument"), eq("breaking-news"), eq(null)))
                .andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(folderNode)).andReturn("/content/documents/channel/news");

        expect(JcrUtils.getStringProperty(folderNode, HippoStdNodeType.HIPPOSTD_CHANNEL_ID, null)).andReturn("");

        replayAll();

        documentsService.createDocument(info, userContext);

        verifyAll();
    }

    @Test(expected = InternalServerErrorException.class)
    public void createXPageInFolderWithUnknownLayoutThrowsException() throws Exception {
        info.setLayout("unknown-xpage-layout");

        final Node folderNode = createMock(Node.class);
        final Node documentHandle = createMock(Node.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);

        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        final DocumentType docType = provideDocumentType(documentHandle);
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.of(folderWorkflow));
        expect(folderWorkflow.add(eq("new-news-document"), eq("project:newsdocument"), eq("breaking-news"), eq(null)))
                .andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(folderNode)).andReturn("/content/documents/channel/news");

        expect(JcrUtils.getStringProperty(folderNode, HippoStdNodeType.HIPPOSTD_CHANNEL_ID, null)).andReturn("channel-id");

        final ChannelService channelService = createMock(ChannelService.class);
        expect(platformServices.getChannelService()).andReturn(channelService);
        expect(channelService.getXPageLayouts(eq("channel-id"))).andReturn(Collections.singletonMap("xpage-layout", null));

        replayAll();

        documentsService.createDocument(info, userContext);

        verifyAll();
    }

    @Test
    public void createXPage() throws Exception {
        final Node rootFolderNode = createMock(Node.class);
        final Node folderNode = createMock(Node.class);
        final Node documentHandle = createMock(Node.class);
        final Node documentDraft = createMock(Node.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        final ChannelService channelService = createMock(ChannelService.class);
        final XPageLayout layout = createMock(XPageLayout.class);
        final JcrTemplateNode layoutTemplateNode = createMock(JcrTemplateNode.class);

        info.setLayout("xpage-layout");
        info.setDefaultPath("2017/11");
        info.setFolderTemplateQuery("folderTemplateQuery");

        expect(FolderUtils.getFolder(eq("/content/documents/channel/news"), eq(session)))
                .andReturn(rootFolderNode);
        expect(FolderUtils.getOrCreateFolder(eq(rootFolderNode), eq("2017/11"), eq(session), eq("folderTemplateQuery")))
                .andReturn(folderNode);
        expect(FolderUtils.getLocale(folderNode))
                .andReturn("en_GB");
        expect(DocumentNameUtils.encodeDisplayName(eq("Breaking News"), eq("en_GB")))
                .andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folderNode), eq("Breaking News (encoded)")))
                .andReturn(false);
        expect(DocumentNameUtils.encodeUrlName(eq("breaking news"), eq("en_GB")))
                .andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folderNode), eq("breaking-news")))
                .andReturn(false);
        expect(WorkflowUtils.getWorkflow(eq(folderNode), eq("internal"), eq(FolderWorkflow.class)))
                .andReturn(Optional.of(folderWorkflow));
        expect(JcrUtils.getStringProperty(folderNode, HippoStdNodeType.HIPPOSTD_CHANNEL_ID, null))
                .andReturn("channel-id");
        expect(platformServices.getChannelService())
                .andReturn(channelService);
        expect(layout.getJcrTemplateNode())
                .andReturn(layoutTemplateNode);
        expect(channelService.getXPageLayouts(eq("channel-id")))
                .andReturn(Collections.singletonMap("xpage-layout", layout));
        expect(folderWorkflow.add(eq("new-news-document"), eq("project:newsdocument"), eq("breaking-news"), eq(layoutTemplateNode)))
                .andReturn("/content/documents/channel/news/breaking-news/breaking-news");
        expect(session.getNode(eq("/content/documents/channel/news/breaking-news/breaking-news")))
                .andReturn(documentDraft);
        expect(documentDraft.getParent())
                .andReturn(documentHandle);

        DocumentNameUtils.setDisplayName(eq(documentHandle), eq("Breaking News (encoded)"));
        expectLastCall();

        expect(WorkflowUtils.getDocumentVariantNode(eq(documentHandle), eq(Variant.DRAFT)))
                .andReturn(Optional.of(documentDraft));
        expect(documentHandle.getIdentifier()).andReturn("uuid");

        final DocumentType docType = provideDocumentType(documentHandle);
        expect(docType.isReadOnlyDueToUnsupportedValidator()).andReturn(false);
        expect(docType.getId()).andReturn("project:newsdocument");
        expect(DocumentUtils.getDisplayName(documentHandle)).andReturn(Optional.of("Breaking News (encoded)"));
        expect(JcrUtils.getNodeNameQuietly(eq(documentHandle))).andReturn("breaking-news");
        expect(JcrUtils.getNodePathQuietly(eq(documentHandle))).andReturn("/content/documents/news/breaking-news");
        expect(PublicationStateUtils.getPublicationStateFromVariant(documentDraft)).andReturn(PublicationState.NEW);
        expect(DocumentLocaleUtils.getDocumentLocale(documentDraft)).andReturn("en");

        session.save();
        expectLastCall();

        final List<FieldType> fields = Collections.emptyList();
        expect(docType.getFields()).andReturn(fields);
        FieldTypeUtils.readFieldValues(eq(documentDraft), eq(fields), isA(Map.class));
        expectLastCall();

        expect(documentDraft.getIdentifier()).andReturn("draftUuid").anyTimes();
        expect(JcrUtils.getStringProperty(eq(documentDraft), anyString(), eq(null))).andReturn("draft");
        expect(JcrUtils.getStringProperty(eq(documentDraft), eq(HIPPO_PROPERTY_BRANCH_ID), eq(MASTER_BRANCH_ID))).andReturn(MASTER_BRANCH_ID);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        expect(WorkflowUtils.getWorkflow(eq(documentHandle), eq("default"), eq(DocumentWorkflow.class)))
                .andReturn(Optional.of(documentWorkflow));
        expect(documentWorkflow.hints()).andReturn(new HashMap<>());
        replayAll(documentDraft);

        documentsService.createDocument(info, userContext);

        verifyAll();
    }


    @Test(expected = BadRequestException.class)
    public void updateDocumentNamesWithoutDisplayName() {
        final Document document = new Document();
        documentsService.updateDocumentNames("uuid", MASTER_BRANCH_ID, document, userContext);
    }

    @Test(expected = BadRequestException.class)
    public void updateDocumentNamesWithoutUrlName() {
        final Document document = new Document();
        document.setDisplayName("Breaking News");
        documentsService.updateDocumentNames("uuid", MASTER_BRANCH_ID, document, userContext);
    }

    @Test(expected = BadRequestException.class)
    public void updateDocumentNamesNotAHandle() {
        final String uuid = "uuid";
        final Document document = new Document();

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());
        replayAll();

        documentsService.updateDocumentNames(uuid, MASTER_BRANCH_ID, document, userContext);
    }

    @Test
    public void updateDocumentNamesUrlNameClashes() {
        final String uuid = "uuid";
        final String urlName = "new name";
        final String encodedUrlName = "new-name";
        final Document document = new Document();
        document.setDisplayName("New Name");
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");
        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folder), eq(encodedUrlName))).andReturn(true);

        replayAll();

        assertUpdateDocumentNamesFails(uuid, document, Reason.SLUG_ALREADY_EXISTS);

        verifyAll();
    }

    @Test
    public void updateDocumentNamesUrlNameOnly() throws Exception {
        final String uuid = "uuid";
        final String displayName = "Breaking News";
        final String encodedDisplayName = "Breaking News (encoded)";
        final String urlName = "new name";
        final String encodedUrlName = "new-name";
        final Document document = new Document();
        document.setDisplayName(displayName);
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");

        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folder), eq(encodedUrlName))).andReturn(false);

        expect(DocumentNameUtils.encodeDisplayName(eq(displayName), eq(folderLocale))).andReturn(encodedDisplayName);
        expect(DocumentNameUtils.getDisplayName(eq(handle))).andReturn(encodedDisplayName);

        DocumentNameUtils.setUrlName(eq(handle), eq(encodedUrlName), anyObject());
        expectLastCall();

        final EditableWorkflow editingWorkflow = createMock(EditableWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        expect(editingWorkflow.hints(MASTER_BRANCH_ID)).andReturn(hints);
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("editing"), eq(EditableWorkflow.class))).andReturn(Optional.of(editingWorkflow));

        replayAll();

        final Document result = documentsService.updateDocumentNames(uuid, MASTER_BRANCH_ID, document, userContext);
        assertThat(result.getDisplayName(), equalTo(displayName));
        assertThat(result.getUrlName(), equalTo(encodedUrlName));

        verifyAll();
    }

    @Test
    public void updateDocumentNamesDisplayNameOnlyAndClashes() {
        final String uuid = "uuid";
        final String displayName = "New Name";
        final String encodedDisplayName = "New Name (encoded)";
        final String urlName = "breaking news";
        final String encodedUrlName = "breaking-news";
        final Document document = new Document();
        document.setDisplayName(displayName);
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");

        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn(encodedUrlName);

        expect(DocumentNameUtils.encodeDisplayName(eq(displayName), eq(folderLocale))).andReturn(encodedDisplayName);
        expect(DocumentNameUtils.getDisplayName(eq(handle))).andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folder), eq(encodedDisplayName))).andReturn(true);

        replayAll();

        assertUpdateDocumentNamesFails(uuid, document, Reason.NAME_ALREADY_EXISTS);

        verifyAll();
    }

    @Test
    public void updateDocumentNamesDisplayNameOnly() {
        final String uuid = "uuid";
        final String displayName = "New Name";
        final String encodedDisplayName = "New Name (encoded)";
        final String urlName = "breaking news";
        final String encodedUrlName = "breaking-news";
        final Document document = new Document();
        document.setDisplayName(displayName);
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");

        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn(encodedUrlName);

        expect(DocumentNameUtils.encodeDisplayName(eq(displayName), eq(folderLocale))).andReturn(encodedDisplayName);
        expect(DocumentNameUtils.getDisplayName(eq(handle))).andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folder), eq(encodedDisplayName))).andReturn(false);

        DocumentNameUtils.setDisplayName(eq(handle), eq(encodedDisplayName));
        expectLastCall();

        replayAll();

        final Document result = documentsService.updateDocumentNames(uuid, MASTER_BRANCH_ID, document, userContext);
        assertThat(result.getDisplayName(), equalTo(encodedDisplayName));
        assertThat(result.getUrlName(), equalTo(urlName));

        verifyAll();
    }

    @Test
    public void updateDocumentNamesBoth() throws Exception {
        final String uuid = "uuid";
        final String displayName = "New Name";
        final String encodedDisplayName = "New Name (encoded)";
        final String urlName = "new name";
        final String encodedUrlName = "new-name";
        final Document document = new Document();
        document.setDisplayName(displayName);
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");

        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folder), eq(encodedUrlName))).andReturn(false);

        expect(DocumentNameUtils.encodeDisplayName(eq(displayName), eq(folderLocale))).andReturn(encodedDisplayName);
        expect(DocumentNameUtils.getDisplayName(eq(handle))).andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folder), eq(encodedDisplayName))).andReturn(false);

        DocumentNameUtils.setUrlName(eq(handle), eq(encodedUrlName), anyObject());
        expectLastCall();

        DocumentNameUtils.setDisplayName(eq(handle), eq(encodedDisplayName));
        expectLastCall();

        final EditableWorkflow editingWorkflow = createMock(EditableWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        expect(editingWorkflow.hints(MASTER_BRANCH_ID)).andReturn(hints);
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("editing"), eq(EditableWorkflow.class))).andReturn(Optional.of(editingWorkflow));

        replayAll();

        final Document result = documentsService.updateDocumentNames(uuid, MASTER_BRANCH_ID, document, userContext);
        assertThat(result.getDisplayName(), equalTo(encodedDisplayName));
        assertThat(result.getUrlName(), equalTo(encodedUrlName));

        verifyAll();
    }

    @Test
    public void updateDocumentNamesBothDisplayNameClashes() {
        final String uuid = "uuid";
        final String displayName = "New Name";
        final String encodedDisplayName = "New Name (encoded)";
        final String urlName = "new name";
        final String encodedUrlName = "new-name";
        final Document document = new Document();
        document.setDisplayName(displayName);
        document.setUrlName(urlName);
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final String folderLocale = "en";

        expect(DocumentUtils.getHandle(eq(uuid), eq(session))).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("project:newsdocument"));
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(FolderUtils.getLocale(eq(folder))).andReturn(folderLocale);
        expect(JcrUtils.getNodePathQuietly(eq(handle))).andReturn("/content/documents/channel/news/breaking-news");

        expect(DocumentNameUtils.encodeUrlName(eq(urlName), eq(folderLocale))).andReturn(encodedUrlName);
        expect(DocumentNameUtils.getUrlName(eq(handle))).andReturn("breaking-news");
        expect(FolderUtils.nodeExists(eq(folder), eq(encodedUrlName))).andReturn(false);

        expect(DocumentNameUtils.encodeDisplayName(eq(displayName), eq(folderLocale))).andReturn(encodedDisplayName);
        expect(DocumentNameUtils.getDisplayName(eq(handle))).andReturn("Breaking News (encoded)");
        expect(FolderUtils.nodeWithDisplayNameExists(eq(folder), eq(encodedDisplayName))).andReturn(true);

        replayAll();

        assertUpdateDocumentNamesFails(uuid, document, Reason.NAME_ALREADY_EXISTS);

        verifyAll();
    }

    @Test(expected = NotFoundException.class)
    public void deleteDocumentNotAHandle() {
        final String uuid = "uuid";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());
        replayAll();

        documentsService.deleteDocument(uuid, MASTER_BRANCH_ID, userContext);
    }

    @Test
    public void deleteDocumentNoWorkflow() {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.empty());
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.empty());

        replayAll();

        try {
            documentsService.deleteDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            assertErrorStatusAndReason(e, Status.METHOD_NOT_ALLOWED, Reason.NOT_A_DOCUMENT);
        }

        verifyAll();
    }

    @Test(expected = InternalServerErrorException.class)
    public void deleteArchivableDocumentWorkflowFailure() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        expect(workflow.hints(MASTER_BRANCH_ID)).andReturn(hints);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(workflow));
        expect(EditingUtils.canArchiveDocument(anyObject())).andReturn(true);

        workflow.delete();
        expectLastCall().andThrow(new WorkflowException("meh"));

        replayAll();

        try {
            hints.put(DocumentWorkflowAction.delete().getAction(), true);
            documentsService.deleteDocument(uuid, MASTER_BRANCH_ID, userContext);
        } finally {
            verifyAll();
        }
    }

    @Test
    public void deleteArchivableDocumentSuccess() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        expect(workflow.hints(MASTER_BRANCH_ID)).andReturn(hints);

        expect(WorkflowUtils.getWorkflow(handle, "default", DocumentWorkflow.class)).andStubReturn(Optional.of(workflow));
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(workflow));
        expect(EditingUtils.canArchiveDocument(anyObject())).andReturn(true);

        workflow.delete();
        expectLastCall();


        replayAll();

        documentsService.deleteDocument(uuid, MASTER_BRANCH_ID, userContext);

        verifyAll();
    }

    @Test(expected = ForbiddenException.class)
    public void deleteExistingDocumentWithPreviewVariant() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();
        expect(workflow.hints(MASTER_BRANCH_ID)).andReturn(hints);

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(workflow));
        expect(EditingUtils.canArchiveDocument(anyObject())).andReturn(false);
        expect(EditingUtils.hasPreview(anyObject())).andReturn(true);

        replayAll(workflow);

        documentsService.deleteDocument(uuid, MASTER_BRANCH_ID, userContext);

        verifyAll();
    }

    @Test
    public void deleteNewDocumentNoFolderWorkflow() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints(MASTER_BRANCH_ID)).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(workflow));
        expect(EditingUtils.canArchiveDocument(eq(hints))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(hints))).andReturn(false);
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(WorkflowUtils.getWorkflow(eq(folder), eq("internal"), eq(FolderWorkflow.class))).andReturn(Optional.empty());
        expect(DocumentUtils.getDisplayName(eq(folder))).andReturn(Optional.empty());

        replayAll(workflow);

        try {
            documentsService.deleteDocument(uuid, MASTER_BRANCH_ID, userContext);
            fail("No Exception");
        } catch (final MethodNotAllowed e) {
            assertErrorStatusAndReason(e, Status.METHOD_NOT_ALLOWED, Reason.NOT_A_FOLDER);
        }

        verifyAll();
    }

    @Test(expected = ForbiddenException.class)
    public void deleteNewDocumentNotAllowedByFolderWorkflow() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints(MASTER_BRANCH_ID)).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.canArchiveDocument(eq(hints))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(hints))).andReturn(false);
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(WorkflowUtils.getWorkflow(eq(folder), eq("internal"), eq(FolderWorkflow.class))).andReturn(Optional.of(folderWorkflow));
        expect(EditingUtils.canEraseDocument(eq(folderWorkflow))).andReturn(false);
        expect(JcrUtils.getNodeNameQuietly(handle)).andReturn("document");
        expect(JcrUtils.getNodePathQuietly(folder)).andReturn("/path/to/folder");

        replayAll(documentWorkflow, folderWorkflow);

        try {
            documentsService.deleteDocument(uuid, MASTER_BRANCH_ID, userContext);
        } finally {
            verifyAll();
        }
    }

    @Test(expected = InternalServerErrorException.class)
    public void deleteNewDocumentFolderWorkflowFailure() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints(MASTER_BRANCH_ID)).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.canArchiveDocument(eq(hints))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(hints))).andReturn(false);
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(WorkflowUtils.getWorkflow(eq(folder), eq("internal"), eq(FolderWorkflow.class))).andReturn(Optional.of(folderWorkflow));
        expect(EditingUtils.canEraseDocument(eq(folderWorkflow))).andReturn(true);

        final String handleName = "document";
        expect(handle.getName()).andReturn(handleName);

        folderWorkflow.delete(eq(handleName));
        expectLastCall().andThrow(new WorkflowException("meh"));

        replayAll(documentWorkflow, handle, folderWorkflow);

        try {
            hints.put(DocumentWorkflowAction.delete().getAction(), true);
            documentsService.deleteDocument(uuid, MASTER_BRANCH_ID, userContext);
        } finally {
            verifyAll();
        }
    }

    @Test
    public void deleteNewDocumentSuccess() throws Exception {
        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        final Node folder = createMock(Node.class);
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(documentWorkflow.hints(MASTER_BRANCH_ID)).andStubReturn(hints);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(documentWorkflow));
        expect(EditingUtils.canArchiveDocument(eq(hints))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(hints))).andReturn(false);
        expect(FolderUtils.getFolder(eq(handle))).andReturn(folder);
        expect(WorkflowUtils.getWorkflow(eq(folder), eq("internal"), eq(FolderWorkflow.class))).andReturn(Optional.of(folderWorkflow));
        expect(EditingUtils.canEraseDocument(eq(folderWorkflow))).andReturn(true);

        final String handleName = "document";
        expect(handle.getName()).andReturn(handleName);

        folderWorkflow.delete(eq(handleName));
        expectLastCall();

        replayAll(documentWorkflow, handle, folderWorkflow);

        documentsService.deleteDocument(uuid, MASTER_BRANCH_ID, userContext);

        verifyAll();
    }

    @Test
    public void cannotCreateDraftNoHintsFromWorkflow() throws ErrorWithPayloadException, RepositoryException, RemoteException, WorkflowException {

        final String uuid = "uuid";
        final Node handle = createMock(Node.class);
        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(handle));
        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of("some:documenttype"));
        expect(DocumentUtils.getDisplayName(handle)).andReturn(Optional.of("Display Name"));

        expect(PublicationStateUtils.getPublicationStateFromHandle(handle)).andReturn(PublicationState.UNKNOWN);

        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        expect(WorkflowUtils.getWorkflow(anyObject(), anyObject(), eq(DocumentWorkflow.class))).andReturn(Optional.of(workflow));
        expect(workflow.hints(anyString())).andReturn(emptyMap());

        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("some-key", "some value");
        expect(hintsInspector.canObtainEditableDocument(eq(MASTER_BRANCH_ID), anyObject())).andReturn(false);

        final Optional<ErrorInfo> errorInfo = Optional.of(new ErrorInfo(Reason.INVALID_DATA, hints));
        expect(hintsInspector.determineEditingFailure(eq(MASTER_BRANCH_ID), anyObject(), eq(session))).andReturn(errorInfo);

        replayAll();

        try {
            documentsService.obtainEditableDocument(uuid, MASTER_BRANCH_ID, userContext);
        } catch (ForbiddenException e) {
            final ErrorInfo payload = (ErrorInfo) e.getPayload();
            assertThat(payload.getReason(), is(Reason.INVALID_DATA));
            assertThat(payload.getParams(), is(hints));
            assertThat(payload.getParams().get("displayName"), equalTo("Display Name"));
            assertThat(payload.getParams().get("publicationState"), equalTo(PublicationState.UNKNOWN));
        }

        verifyAll();
    }

    private DocumentType provideDocumentType(final Node handle) throws Exception {
        final String variantType = "project:newsdocument";
        final DocumentType docType = createMock(DocumentType.class);
        final DocumentTypesService documentTypesService = createMock(DocumentTypesService.class);

        expect(DocumentUtils.getVariantNodeType(handle)).andReturn(Optional.of(variantType)).anyTimes();
        expect(DocumentTypesService.get()).andReturn(documentTypesService).anyTimes();
        expect(documentTypesService.getDocumentType(variantType, userContext)).andReturn(docType).anyTimes();
        expect(handle.getSession()).andReturn(session).anyTimes();

        return docType;
    }

    private void assertUpdateDocumentNamesFails(final String uuid, final Document document, final Reason reason) throws ErrorWithPayloadException {
        try {
            documentsService.updateDocumentNames(uuid, null, document, userContext);
            fail("No Exception");
        } catch (final ConflictException e) {
            assertErrorStatusAndReason(e, Status.CONFLICT, reason);
        }
    }

    private static void assertErrorStatusAndReason(final ErrorWithPayloadException e, final Status status, final Reason reason) {
        assertErrorStatusAndReason(e, status, reason, null);
    }

    private static void assertErrorStatusAndReason(final ErrorWithPayloadException e, final Status status, final Reason reason,
                                            final Map<String, Serializable> params) {
        assertTrue(e.getPayload() instanceof ErrorInfo);

        final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
        assertThat(e.getStatus(), is(status));
        assertThat(errorInfo.getReason(), is(reason));

        final Map<String, Serializable> exceptionParams = errorInfo.getParams();
        if (params == null) {
            assertNull(exceptionParams);
        } else {
            params.forEach((name, value) -> {
                assertThat(exceptionParams.get(name), is(value));
            });
        }
    }

}
