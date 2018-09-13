/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.documentworkflow.integration;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;

import org.assertj.core.api.Assertions;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.Utilities;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_LABEL_UNPUBLISHED;

public class DocumentWorkflowVersioningTest extends AbstractDocumentWorkflowIntegrationTest {

    private static final int NO_VERSIONS = 3;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        document.setProperty("counter", 0l);
        session.save();
    }

    @Test
    public void testHints() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow();

        final Map<String, Serializable> hints = workflow.hints();
        assertNotNull(hints.get("version"));
        assertTrue((Boolean) hints.get("version"));

        assertNotNull(hints.get("listVersions"));
        assertTrue((Boolean) hints.get("listVersions"));

        assertNotNull(hints.get("restoreVersion"));
        assertTrue((Boolean) hints.get("restoreVersion"));

        assertNotNull(hints.get("versionRestoreTo"));
        assertTrue((Boolean) hints.get("versionRestoreTo"));

        assertNotNull(hints.get("retrieveVersion"));
        assertTrue((Boolean) hints.get("retrieveVersion"));
    }

    @Test
    public void testVersionDocumentAndRetrieve() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow();
        for (int i = 0; i < NO_VERSIONS; i++) {
            edit();
            workflow.version();
        }
        final Map<Calendar, Set<String>> history = workflow.listVersions();
        assertEquals(NO_VERSIONS, history.size());
        long counter = 0;
        for (Map.Entry<Calendar, Set<String>> entry : history.entrySet()) {
            final Document version = workflow.retrieveVersion(entry.getKey());
            assertEquals(++counter, version.getNode(session).getProperty("counter").getLong());
        }
    }

    @Test
    public void version_document_results_in_master_preview_label() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow();
        workflow.version();

        final Map<Calendar, Set<String>> history = workflow.listVersions();
        assertEquals(1, history.size());
        assertTrue("In the labels we expect 'master-unpublished' after version",
                history.values().iterator().next().contains(MASTER_BRANCH_LABEL_UNPUBLISHED));

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        versionHistory.hasVersionLabel(MASTER_BRANCH_LABEL_UNPUBLISHED);
    }

    @Test
    public void testVersionDocumentAndRestore() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow();
        for (int i = 0; i < NO_VERSIONS; i++) {
            edit();
            workflow.version();
        }
        final Map<Calendar, Set<String>> history = workflow.listVersions();
        final Calendar first = history.keySet().iterator().next();
        workflow.restoreVersion(first);

        assertEquals(1l, document.getProperty("counter").getLong());
    }

    private Long edit() throws Exception {
        JcrUtils.ensureIsCheckedOut(document);
        long counter = document.getProperty("counter").getLong();
        document.setProperty("counter", ++counter);
        session.save();
        return counter;
    }

    private DocumentWorkflow getDocumentWorkflow() throws RepositoryException {
        return (DocumentWorkflow) getWorkflow(handle, "default");
    }

    private Workflow getWorkflow(Node node, String category) throws RepositoryException {
        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        return workflowManager.getWorkflow(category, node);
    }


    @Test
    public void workflow_only_creates_an_extra_version_when_needed() throws Exception {
        DocumentWorkflow workflow = getDocumentWorkflow(handle);

        final Node unpublished = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .isEqualTo(1);

        workflow.branch("foo", "Foo");

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Branching is expected to create an extra version for master since there was no live for 'master'")
                .isEqualTo(2);

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Checkout is expected to create an extra version since there was not yet a version for 'foo'")
                .isEqualTo(3);


        workflow.checkoutBranch("foo");

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Checkout of 'foo' should not result in a new version for 'master' since 'master' did not change")
                .isEqualTo(3);

        // publish of 'master' should only create one extra version, even though 'foo' was unpublished variant: The reason
        // is that 'foo' didn't change
        workflow.publish();

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Publishing is expected to create an extra version since there was no live for 'master'")
                .isEqualTo(4);

        workflow.branch("bar", "Bar");

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Since master was published and no changes, we do not expect an extra version")
                .isEqualTo(4);


        workflow.checkoutBranch("foo");

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Since 'bar' does not have a revision yet, a new revision is expected")
                .isEqualTo(5);


        workflow.checkoutBranch("bar");

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Since 'bar' does not have a revision yet, a new revision is expected")
                .isEqualTo(5);

        // modify bar
        workflow.obtainEditableInstance("bar");
        workflow.commitEditableInstance();

        workflow.checkoutBranch("foo");

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Since 'bar' got changed, a new revision is expected")
                .isEqualTo(6);

        // depublish 'master' again
        workflow.depublish();

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Since 'foo' did not change, and neither did master, no new versions expected")
                .isEqualTo(6);


        workflow.publish();

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Publication should always result in a new version for synchronous version history, even " +
                        "if the to be published unpublished has a same version in version history already")
                .isEqualTo(7);

        // bar is not the current unpublished
        workflow.publishBranch("bar");
        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Bar unpublished did not have any changes, so expected no new version but " +
                        "only the label bar-published being added to point to the same version as bar-unpublished")
                .isEqualTo(7);

        workflow.depublishBranch("bar");

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("As a result of unpublished 'bar' no new version is expected")
                .isEqualTo(7);

        // now publish bar when 'bar' is the unpublished variant : this still should not result in a new version
        workflow.checkoutBranch("bar");
        workflow.publishBranch("bar");
        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Bar unpublished did not have any changes, so expected no new version but " +
                        "only the label bar-published being added to point to the same version as bar-unpublished")
                .isEqualTo(7);

        // start working on 'foo'
        workflow.obtainEditableInstance("foo");

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Bar unpublished did not have any changes, so obtain editable instance for 'foo' is not " +
                        "expected to result in a new version")
                .isEqualTo(7);

        workflow.commitEditableInstance();

        workflow.publishBranch("foo");
        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Foo unpublished did have changes, so publication is expected to result in a new version.")
                .isEqualTo(8);

        workflow.depublishBranch("bar");
        workflow.publishBranch("bar");
        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Bar did not change so no new version")
                .isEqualTo(8);

        workflow.restoreVersionToBranch(versionHistory.getVersionByLabel("bar-unpublished"), "foo");

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Since unpublished 'foo' did not have any changes, we expect 1 new version instead of 2")
                .isEqualTo(9);

        workflow.checkoutBranch("bar");

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Since unpublished 'foo' has no changes, we do not expect a new version")
                .isEqualTo(9);

        // now modify 'foo'
        workflow.obtainEditableInstance("foo");
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        draft.setProperty("Title", "foo");
        session.save();
        workflow.commitEditableInstance();

        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .isEqualTo(9);

        // now again restore 'bar' to 'foo'. Since 'foo' has changes in unpublished, we expect an extra version as a
        // result of restore
        workflow.restoreVersionToBranch(versionHistory.getVersionByLabel("bar-unpublished"), "foo");
        Assertions.assertThat(versionHistory.getAllVersions().getSize())
                .as("Since unpublished 'foo' did have changes, we do expect 2 new versions when restoring " +
                        "from 'bar' to 'foo'")
                .isEqualTo(11);
    }

}
