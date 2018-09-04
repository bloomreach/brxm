/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.documentworkflow.integration;

import java.rmi.RemoteException;
import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_LABEL_UNPUBLISHED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DocumentWorkflowRestoreVersionToBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void restore_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("restoreVersionToBranch"));
        assertTrue((Boolean) workflow.hints().get("restoreVersionToBranch"));
    }

    @Test
    public void restore_version_to_NON_existing_branch_results_in_workflow_exception() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");
        final Node unpublished = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final Version version = session.getWorkspace().getVersionManager()
                .getVersionHistory(unpublished.getPath()).getVersionByLabel(MASTER_BRANCH_LABEL_UNPUBLISHED);

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            try {
                workflow.restoreVersionToBranch(version, "non-existing-branch");
                fail("Expected workflow exception");
            } catch (WorkflowException e) {
                assertEquals("Cannot restore version to branch 'non-existing-branch' since it doesn't exist", e.getMessage());
            }
        }

    }


    @Test
    public void restore_master_version_to_master_unpublished_without_other_existing_branches() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.version();

        final Node unpublishedMaster = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        unpublishedMaster.setProperty("title", "title Master");
        session.save();

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublishedMaster.getPath());
        final Version version = versionHistory
                .getVersionByLabel(MASTER_BRANCH_LABEL_UNPUBLISHED);

        workflow.restoreVersionToBranch(version, MASTER_BRANCH_ID);

        final Version versionAgain = versionHistory
                .getVersionByLabel(MASTER_BRANCH_LABEL_UNPUBLISHED);

        assertFalse(version.isSame(versionAgain));
        assertEquals("title Master", versionAgain.getFrozenNode().getProperty("title").getString());
    }

    @Test
    public void restore_master_version_to_master_unpublished() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node unpublishedMaster = createBranchFooAndModifyMasterUnpublished(workflow);

        unpublishedMaster.setProperty("title", "title Master again");
        session.save();

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublishedMaster.getPath());
        final Version version = versionHistory
                .getVersionByLabel(MASTER_BRANCH_LABEL_UNPUBLISHED);

        workflow.restoreVersionToBranch(version, MASTER_BRANCH_ID);

        assertTrue(unpublishedMaster.hasProperty("title"));
        assertEquals("title Master", unpublishedMaster.getProperty("title").getString());

        assertTrue(versionHistory.hasVersionLabel(MASTER_BRANCH_LABEL_UNPUBLISHED));

        assertEquals("Expected that before restore the unpublished got versioned",
                "title Master again", versionHistory.getVersionByLabel(MASTER_BRANCH_LABEL_UNPUBLISHED).getFrozenNode()
                        .getProperty("title").getString());

    }

    private Node createBranchFooAndModifyMasterUnpublished(final DocumentWorkflow workflow) throws WorkflowException, RepositoryException, RemoteException {
        workflow.branch("foo", "Foo");

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        workflow.obtainEditableInstance();
        final Node unpublishedMaster = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        unpublishedMaster.setProperty("title", "title Master");
        session.save();
        workflow.commitEditableInstance();
        // create a master unpublished version which contains the above 'title' property
        workflow.version();

        return WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
    }

    @Test
    public void restore_branch_version_to_master_unpublished() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node unpublishedMaster = createBranchFooAndModifyMasterUnpublished(workflow);

        // restore the 'foo' version now to'master unpubished'. Expected is that 'title' from master unpublished is gone,
        // but also expected that before the restore, the unpublished gets versioned to not loose any data! Also expected
        // is that since version 'foo' gets restored to 'master', the mixin HIPPO_MIXIN_BRANCH_INFO gets removed on
        // unpublished master

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublishedMaster.getPath());
        final Version version = versionHistory.getVersionByLabel("foo-unpublished");


        final String[] mixins = JcrUtils.getMultipleStringProperty(version.getFrozenNode(), "jcr:frozenMixinTypes", new String[]{});

        assertTrue("branch info mixin expected to be present on the version to be restored over 'master'",
                Arrays.stream(mixins).anyMatch(mixin -> HIPPO_MIXIN_BRANCH_INFO.equals(mixin)));

        workflow.restoreVersionToBranch(version, MASTER_BRANCH_ID);

        assertFalse("Since restore was to master, expected was that the branch info mixin got removed",
                unpublishedMaster.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        assertFalse(unpublishedMaster.hasProperty("title"));

        assertTrue(versionHistory.hasVersionLabel(MASTER_BRANCH_LABEL_UNPUBLISHED));

        assertEquals("Expected that before restore the unpublished got versioned",
                "title Master", versionHistory.getVersionByLabel(MASTER_BRANCH_LABEL_UNPUBLISHED).getFrozenNode()
                        .getProperty("title").getString());

    }

    @Test
    public void restore_master_version_to_branch_unpublished() throws Exception {

        assertions_restoring_master(true);
    }

    @Test
    public void restore_master_version_to_branch_in_version_history() throws Exception {
        assertions_restoring_master(false);
    }

    private void assertions_restoring_master(final boolean checkoutFooFirst) throws RepositoryException, WorkflowException, RemoteException {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node unpublished = createBranchFooAndModifyMasterUnpublished(workflow);

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());
        final Version version = versionHistory.getVersionByLabel("foo-unpublished");

        assertFalse(version.getFrozenNode().hasProperty("title"));

        if (checkoutFooFirst) {
            workflow.checkoutBranch("foo");
        } else {
            assertFalse("master is expected to be the unpublished", unpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        }

        final Version masterVersion = versionHistory.getVersionByLabel(MASTER_BRANCH_LABEL_UNPUBLISHED);

        workflow.restoreVersionToBranch(masterVersion, "foo");

        // regardless whether 'foo' got checked out above with 'checkoutFooFirst' or not, we expect after the
        // restore the unpublished to be for 'foo'
        assertTrue("since master unpublished gets restored to 'foo', title property is expected to be present",
                unpublished.hasProperty("title"));

        assertTrue(unpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo", unpublished.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
    }



    @Test
    public void restore_branch_foo_version_to_branch_foo_unpublished() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node unpublished = createBranchFooAndModifyMasterUnpublished(workflow);
        workflow.checkoutBranch("foo");
        // branch foo is now the unpublished

        unpublished.setProperty("title", "title Foo");
        session.save();


        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());
        final Version fooVersion = versionHistory.getVersionByLabel("foo-unpublished");

        workflow.restoreVersionToBranch(fooVersion, "foo");

        // now we expect that the previously version of 'foo' with the title is versioned, and the current unpublished
        // has been replaced with the 'foo' version from version history that did not yet have the property 'title'

        assertFalse("since foo version without title got restored it shouldn't be present",
                unpublished.hasProperty("title"));
        assertTrue(unpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        final Version fooVersionNew = versionHistory.getVersionByLabel("foo-unpublished");

        assertFalse("Expected a new version of 'foo' branch because of the restore", fooVersionNew.isSame(fooVersion));

        assertTrue("The versioned foo should have the property 'title'",fooVersionNew.getFrozenNode().hasProperty("title"));
    }

    @Test
    public void restore_branch_bar_version_over_branch_foo_unpublished() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node unpublished = createBranchFooAndModifyMasterUnpublished(workflow);
        workflow.branch("bar", "Bar");

        unpublished.setProperty("title", "title Bar");
        session.save();
        workflow.checkoutBranch("foo");

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());
        final Version barVersion = versionHistory.getVersionByLabel("bar-unpublished");

        assertEquals("title Bar", barVersion.getFrozenNode().getProperty("title").getString());

        workflow.restoreVersionToBranch(barVersion, "foo");

        assertTrue(unpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo",unpublished.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("title Bar",unpublished.getProperty("title").getString());
    }

    @Test
    public void restore_branch_bar_version_over_branch_foo_when_master_is_unpublished() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node unpublished = createBranchFooAndModifyMasterUnpublished(workflow);
        workflow.branch("bar", "Bar");

        unpublished.setProperty("title", "title Bar");
        session.save();

        workflow.checkoutBranch("master");
        unpublished.setProperty("title", "title Master");
        session.save();

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());
        final Version barVersion = versionHistory.getVersionByLabel("bar-unpublished");

        assertEquals("title Bar", barVersion.getFrozenNode().getProperty("title").getString());

        workflow.restoreVersionToBranch(barVersion, "foo");

        // as a result of the restore to 'foo', 'foo' will have become the unpublished and 'master' will have been versioned
        assertTrue(unpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo",unpublished.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("title Bar",unpublished.getProperty("title").getString());


        final Version masterVersion = versionHistory.getVersionByLabel(MASTER_BRANCH_LABEL_UNPUBLISHED);
        assertEquals("title Master", masterVersion.getFrozenNode().getProperty("title").getString());
    }

    @Test
    public void restore_branch_after_document_has_been_renamed() throws Exception {
        DocumentWorkflow workflow = getDocumentWorkflow(handle);

        assertEquals("document", handle.getName());
        for (Node variant : new NodeIterable(handle.getNodes())) {
            assertEquals("document", variant.getName());
        }

        workflow.branch("foo", "Foo");

        workflow.checkoutBranch(MASTER_BRANCH_ID);
        // branch foo is now the unpublished

        workflow.rename("doc");

        assertEquals("doc", handle.getName());
        for (Node variant : new NodeIterable(handle.getNodes())) {
            assertEquals("doc", variant.getName());
        }

        try {
            workflow.hints();
            fail("After rename, workflow is expected to be terminated");
        } catch (WorkflowException e) {
            assertEquals("Workflow documentworkflow already terminated", e.getMessage());
        }

        workflow = getDocumentWorkflow(handle);

        workflow.checkoutBranch("foo");

        assertEquals("doc", handle.getName());
        for (Node variant : new NodeIterable(handle.getNodes())) {
            assertEquals("doc", variant.getName());
        }

        workflow.rename("doc2");

        workflow = getDocumentWorkflow(handle);
        workflow.checkoutBranch(MASTER_BRANCH_ID);

        final Node unpublished = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());

        final Version fooVersion = versionHistory.getVersionByLabel("foo-unpublished");

        workflow.restoreVersionToBranch(fooVersion, "foo");

        assertEquals("doc2", handle.getName());
        for (Node variant : new NodeIterable(handle.getNodes())) {
            assertEquals("doc2", variant.getName());
        }
    }
}
