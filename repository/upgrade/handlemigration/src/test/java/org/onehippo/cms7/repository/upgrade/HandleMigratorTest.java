/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.repository.upgrade;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.util.JcrConstants;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class HandleMigratorTest extends RepositoryTestCase {

    private static int NO_OF_DOCS = 1;
    private static int NO_OF_VERSIONS = 4;

    private Node documents;
    private Node attic;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        documents = session.getNode("/content/documents");
        attic = session.getNode("/content/attic");
        createTestDocuments(NO_OF_DOCS);
        session.save();
    }

    @Override
    public void tearDown() throws Exception {
        removeDocuments();
        super.tearDown();
    }

    @Test
    public void testHandleMigration() throws Exception {
        editAndPublishTestDocuments();
        migrate();
//        session.exportSystemView("/content/documents/document0", System.out, true, false);
        checkDocumentHistory();
    }

    @Test
    public void testDeletedHandleMigration() throws Exception {
        editAndPublishTestDocuments();
        deleteDocuments();
        migrate();
        checkAtticDocumentHistory();
    }

    private void deleteDocuments() throws Exception {
        for (Node handle : new NodeIterable(documents.getNodes())) {
            if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                final Node document = handle.getNode(handle.getName());
                deleteTestDocument(document);
            }
        }
    }

    private void deleteTestDocument(final Node document) throws Exception {
        final FullReviewedActionsWorkflow workflow = getFullReviewedActionsWorkflow(document);
        workflow.depublish();
        workflow.delete();
    }

    private void checkAtticDocumentHistory() throws RepositoryException {
        final List<Node> handles = new ArrayList<Node>();
        attic.accept(new ItemVisitor() {
            @Override
            public void visit(final Property property) throws RepositoryException {
            }
            @Override
            public void visit(final Node node) throws RepositoryException {
                if (JcrUtils.isVirtual(node)) {
                    return;
                }
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    handles.add(node);
                    return;
                }
                for (Node child : new NodeIterable(node.getNodes())) {
                    visit(child);
                }
            }
        });
        for (Node handle : handles) {
            checkAtticHandle(handle);
        }
    }

    private void checkAtticHandle(final Node handle) throws RepositoryException {
        assertTrue("No hippo:deleted node under attic handle", handle.hasNode(handle.getName()));
        final Node deleted = handle.getNode(handle.getName());
        assertTrue(deleted.isNodeType(HippoNodeType.NT_DELETED));
        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        final String documentPath = deleted.getPath();
        final VersionHistory versionHistory = versionManager.getVersionHistory(documentPath);
        final VersionIterator versions = versionHistory.getAllVersions();
        assertEquals("Unexpected number of versions", 7, versions.getSize());
    }

    private void migrate() throws RepositoryException {
        final HandleMigrator handleMigrator = new HandleMigrator(session);
        handleMigrator.init();
        for (Node handle : new NodeIterable(documents.getNodes())) {
            if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                handleMigrator.migrate(handle);
            }
        }
    }

    private void checkDocumentHistory() throws Exception {
        for (int i = 0; i < NO_OF_DOCS; i++) {
            checkDocumentHistory(getPreview(i));
        }
    }

    private void checkDocumentHistory(Node document) throws Exception {
        assertNotNull("No preview available", document);
        assertFalse("Preview still has harddocument mixin", document.isNodeType(HippoNodeType.NT_HARDDOCUMENT));
        assertTrue("Document is not versionable", document.isNodeType(JcrConstants.MIX_VERSIONABLE));
        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        final String documentPath = document.getPath();
        final String documentIdentifier = document.getIdentifier();
        final VersionHistory versionHistory = versionManager.getVersionHistory(documentPath);
        final VersionIterator versions = versionHistory.getAllVersions();
        assertEquals("Unexpected number of versions", NO_OF_VERSIONS+1, versions.getSize());
        versionManager.restore(documentPath, "1.2", true);
        document = session.getNodeByIdentifier(documentIdentifier);
        assertEquals("Unexpected property value", "bar2", JcrUtils.getStringProperty(document, "foo", null));
        assertFalse("Preview still has harddocument mixin", document.isNodeType(HippoNodeType.NT_HARDDOCUMENT));
    }

    private void editAndPublishTestDocuments() throws Exception {
        for (Node handle : new NodeIterable(documents.getNodes())) {
            if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                continue;
            }
            for (int i = 0; i < NO_OF_VERSIONS; i++) {
                final Node document = handle.getNode(handle.getName());
                publishTestDocument(editTestDocument(document, i));
            }
        }
    }

    private Node editTestDocument(final Node document, int i) throws Exception {
        final Node draft = getFullReviewedActionsWorkflow(document).obtainEditableInstance().getNode();
        draft.setProperty("foo", "bar" + i);
        draft.getSession().save();
        return getFullReviewedActionsWorkflow(draft).commitEditableInstance().getNode();
    }

    private void createTestDocuments(final int count) throws Exception {
        for (int i = 0; i < count; i++) {
            String path = createTestDocument(i);
            Node node = session.getNode(path).getParent();
            node.addMixin(HippoNodeType.NT_HARDHANDLE);
        }
    }

    private void removeDocuments() throws RepositoryException {
        for (Node document : new NodeIterable(documents.getNodes())) {
            document.remove();
        }
        session.save();
    }

    private void publishTestDocument(final Node document) throws Exception {
        getFullReviewedActionsWorkflow(document).publish();
    }

    private String createTestDocument(int index) throws Exception {
        return getFolderWorkflow(documents).add("legacy-document", "testcontent:news", "document" + index);
    }

    private Node getPreview(final int index) throws RepositoryException {
        final NodeIterator documents = session.getNode("/content/documents/document" + index).getNodes("document" + index);
        while (documents.hasNext()) {
            final Node variant = documents.nextNode();
            if (HippoStdNodeType.UNPUBLISHED.equals(JcrUtils.getStringProperty(variant, HippoStdNodeType.HIPPOSTD_STATE, null))) {
                return variant;
            }
        }
        return null;
    }

    private FullReviewedActionsWorkflow getFullReviewedActionsWorkflow(final Node document) throws RepositoryException {
        return (FullReviewedActionsWorkflow) getWorkflow("deprecated", document);
    }

    private FolderWorkflow getFolderWorkflow(final Node folder) throws RepositoryException {
        return (FolderWorkflow) getWorkflow("internal", folder);
    }

    private Workflow getWorkflow(final String category, final Node node) throws RepositoryException {
        final WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        return workflowManager.getWorkflow(category, node);
    }

}
