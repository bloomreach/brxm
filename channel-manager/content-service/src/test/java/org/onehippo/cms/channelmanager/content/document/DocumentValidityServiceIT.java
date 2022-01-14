/*
 * Copyright 2022 Bloomreach (http://www.bloomreach.com)
 */

package org.onehippo.cms.channelmanager.content.document;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.assertj.core.api.Assertions;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.Utilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.TestUserContext;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_DOCBASE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROTOTYPE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROTOTYPES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getDocumentWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

public class DocumentValidityServiceIT extends RepositoryTestCase {

    private static final String TEST_DOC = "/docvalidation/documents/test/doc";
    private static final String TEST_DOC_DRAFT = TEST_DOC + "/doc[1]";
    private static final String TEST_DOC_UNPUBLISHED = TEST_DOC + "/doc[2]";
    private static final String TEST_DOC_BACKUP = "/backup-document";

    private static final String TEST_DOCTYPE = "/hippo:namespaces/test/document";
    private static final String TEST_DOCTYPE_BACKUP = "/backup-doctype";

    private DocumentValidityServiceImpl documentValidityService;
    private DocumentWorkflow workflow;
    private Node documentHandle;
    private Node workflowSessionHandle;

    @Before
    public void setup() throws Exception {
        documentValidityService = new DocumentValidityServiceImpl();

        // make a backup of the test document which will be modified
        JcrUtils.copy(session, TEST_DOC, TEST_DOC_BACKUP);
        JcrUtils.copy(session, TEST_DOCTYPE, TEST_DOCTYPE_BACKUP);
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        session.getNode(TEST_DOC).remove();
        JcrUtils.copy(session, TEST_DOC_BACKUP, TEST_DOC);
        session.getNode(TEST_DOC_BACKUP).remove();

        session.getNode(TEST_DOCTYPE).remove();
        JcrUtils.copy(session, TEST_DOCTYPE_BACKUP, TEST_DOCTYPE);
        session.getNode(TEST_DOCTYPE_BACKUP).remove();

        session.save();

        super.tearDown();
    }

    @Before
    public void beforeEach() throws RepositoryException {
        documentHandle = session.getNode(TEST_DOC);

        final String uuid = documentHandle.getIdentifier();
        workflow = getDocumentWorkflow(getHandle(uuid, session));

        final Session internalWorkflowSession = workflow.getWorkflowContext().getInternalWorkflowSession();
        workflowSessionHandle = getHandle(uuid, internalWorkflowSession);

        // ensure the DocumentType is fresh for every test run
        DocumentTypesService.get().invalidateCache();
    }

    @Test
    public void logs_error_and_returns_if_draft_is_null() {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(DocumentValidityServiceImpl.class).build()) {

            documentValidityService.handleDocumentTypeChanges(session, MASTER_BRANCH_ID, workflowSessionHandle, null);

            Assertions.assertThat(interceptor.messages())
                    .containsExactly("Could not find 'DRAFT' variant for document /docvalidation/documents/test/doc");
        }
    }

    @Test
    public void logs_a_warning_if_the_document_has_no_prototype_node() throws Exception {
        // remove the prototype nodes from the doc-type
        final Node doctype = session.getNode(TEST_DOCTYPE);
        doctype.getNode(HIPPO_PROTOTYPES).remove();

        // create draft
        EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, session);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(DocumentValidityServiceImpl.class).build()) {
            final DocumentType documentType = getDocumentType("test:document");
            documentValidityService.handleDocumentTypeChanges(session, MASTER_BRANCH_ID, workflowSessionHandle, documentType);

            Assertions.assertThat(interceptor.messages())
                    .containsExactly("Unable to find prototype 'test:document' for branch 'master', skipping handling of document type changes");
        }
    }

    @Test
    public void adds_a_missing_field_node_to_the_document() throws Exception {
        final DocumentType documentType = getDocumentType("test:document");
        EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, session);

        final Node draft = session.getNode(TEST_DOC_DRAFT);
        final Node unpublished = session.getNode(TEST_DOC_UNPUBLISHED);

        assertFalse(draft.hasNode("test:link"));
        assertFalse(unpublished.hasNode("test:link"));

        documentValidityService.handleDocumentTypeChanges(session, MASTER_BRANCH_ID, workflowSessionHandle, documentType);

        assertLinkExists("docbase-from-document-prototype", 1, draft, unpublished);
    }

    @Test
    public void adds_a_missing_field_node_from_content_type_if_not_in_document_prototype() throws Exception {
        // delete test:link prototype from the published document prototype
        session.getNode(TEST_DOCTYPE + "/" + HIPPO_PROTOTYPES + "/" + HIPPO_PROTOTYPE + "/test:link").remove();

        final DocumentType documentType = getDocumentType("test:document");
        EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, session);

        documentValidityService.handleDocumentTypeChanges(session, MASTER_BRANCH_ID, workflowSessionHandle, documentType);

        final Node draft = session.getNode(TEST_DOC_DRAFT);
        final Node unpublished = session.getNode(TEST_DOC_UNPUBLISHED);

        assertLinkExists("cafebabe-cafe-babe-cafe-babecafebabe", 1, draft, unpublished);
    }

    @Test
    public void adds_multiple_missing_field_nodes_to_the_document() throws Exception {
        final DocumentType documentType = getDocumentType("test:document");
        documentType.getFields().stream()
                .filter(fieldType -> fieldType.getId().equals("test:link"))
                .findFirst()
                .ifPresent(fieldType -> fieldType.setMinValues(3));

        EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, session);

        documentValidityService.handleDocumentTypeChanges(session, MASTER_BRANCH_ID, workflowSessionHandle, documentType);

        final Node draft = session.getNode(TEST_DOC_DRAFT);
        final Node unpublished = session.getNode(TEST_DOC_UNPUBLISHED);

        assertLinkExists("docbase-from-document-prototype", 1, draft, unpublished);
        assertLinkExists("docbase-from-document-prototype", 2, draft, unpublished);
        assertLinkExists("docbase-from-document-prototype", 3, draft, unpublished);
        assertFalse(draft.hasNode("test:link[4]"));
        assertFalse(unpublished.hasNode("test:link[4]"));
    }

    @Test
    public void updates_missing_field_nodes_in_the_document() throws Exception {
        final DocumentType documentType = getDocumentType("test:document");
        EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, session);

        documentValidityService.handleDocumentTypeChanges(session, MASTER_BRANCH_ID, workflowSessionHandle, documentType);

        final Node draft = session.getNode(TEST_DOC_DRAFT);
        final Node unpublished = session.getNode(TEST_DOC_UNPUBLISHED);

        assertLinkExists("docbase-from-document-prototype", 1, draft, unpublished);
        assertFalse(draft.hasNode("test:link[2]"));
        assertFalse(unpublished.hasNode("test:link[2]"));

        documentType.getFields().stream()
                .filter(fieldType -> fieldType.getId().equals("test:link"))
                .findFirst()
                .ifPresent(fieldType -> fieldType.setMinValues(2));

        documentValidityService.handleDocumentTypeChanges(session, MASTER_BRANCH_ID, workflowSessionHandle, documentType);

        assertLinkExists("docbase-from-document-prototype", 2, draft, unpublished);
    }

    // If a field has a min-values setting that is higher than the number of available prototype nodes, it will copy
    // them in a cyclic order; let's assume the field has a min-values of 5 and only two prototype nodes (A & B). The
    // end result will be a document that contains 5 new nodes in the following order: A B A B A
    @Test
    public void copies_prototype_nodes_in_cyclic_order() throws Exception {
        // add a second prototype in the published document prototype for field "test:link"
        final String linkPath = TEST_DOCTYPE + "/" + HIPPO_PROTOTYPES + "/" + HIPPO_PROTOTYPE + "/test:link";
        JcrUtils.copy(session, linkPath, linkPath).setProperty("hippo:docbase", "second-docbase");

        EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, session);

        final DocumentType documentType = getDocumentType("test:document");
        documentType.getFields().stream()
                .filter(fieldType -> fieldType.getId().equals("test:link"))
                .findFirst()
                .ifPresent(fieldType -> fieldType.setMinValues(4));

        documentValidityService.handleDocumentTypeChanges(session, MASTER_BRANCH_ID, workflowSessionHandle, documentType);

        final Node draft = session.getNode(TEST_DOC_DRAFT);
        final Node unpublished = session.getNode(TEST_DOC_UNPUBLISHED);
        assertLinkExists("docbase-from-document-prototype", 1, draft, unpublished);
        assertLinkExists("second-docbase", 2, draft, unpublished);
        assertLinkExists("docbase-from-document-prototype", 3, draft, unpublished);
        assertLinkExists("second-docbase", 4, draft, unpublished);
    }

    @Test
    public void does_not_change_document_if_no_missing_fields() throws Exception {
        final DocumentType documentType = getDocumentType("test:document");
        EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, session);

        documentValidityService.handleDocumentTypeChanges(session, MASTER_BRANCH_ID, workflowSessionHandle, documentType);
        final String handleBefore = snapshot(documentHandle);

        documentValidityService.handleDocumentTypeChanges(session, MASTER_BRANCH_ID, workflowSessionHandle, documentType);
        final String handleAfter = snapshot(documentHandle);

        assertEquals(handleBefore, handleAfter);
    }

    private DocumentType getDocumentType(final String type) {
        final UserContext userContext = new TestUserContext(session);
        return DocumentTypesService.get().getDocumentType(type, userContext);
    }

    private static void assertLinkExists(final String expected, final int position, final Node... variants) throws RepositoryException {
        final String nodeName = "test:link" + (position > 1 ? "[" + position + "]" : "");
        for (final Node variant : variants) {
            assertTrue(variant.hasNode(nodeName));
            assertEquals(expected, variant.getNode(nodeName).getProperty(HIPPO_DOCBASE).getString());
        }
    }

    private static String snapshot(final Node node) throws RepositoryException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utilities.dump(new PrintStream(out), node);
        return out.toString();
    }

}
