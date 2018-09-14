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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.assertj.core.api.Assertions;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.documentworkflow.BranchHandleImpl;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.util.JcrConstants;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_LABEL_UNPUBLISHED;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

public class DocumentWorkflowBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void branch_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("branch"));
        assertTrue((Boolean)workflow.hints().get("branch"));

        // when there is only a live version, branching is still possible (and will create a preview first)
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get();
        toBecomeLive.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        toBecomeLive.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeLive.removeMixin(MIX_VERSIONABLE);
        session.save();
        assertTrue((Boolean)workflow.hints().get("branch"));

        // when document is being edited, you cannot branch
        workflow.obtainEditableInstance();
        assertFalse((Boolean)workflow.hints().get("branch"));

        workflow.commitEditableInstance();
        assertTrue((Boolean)workflow.hints().get("branch"));

        workflow.branch("foo", "Foo");

        assertTrue((Boolean)workflow.hints().get("branch"));

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        assertTrue((Boolean)workflow.hints().get("branch"));
    }

    @Test
    public void branch_when_there_is_a_preview() throws Exception {

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get();
        assertTrue("Expected preview only below handle",preview.isSame(handle.getNode(handle.getName())));

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        assertEquals("Only 'jcr:rootVersion' version expected", 1L, versionHistory.getAllVersions().getSize());

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        // The actual branching!
        final Document branch = workflow.branch("foo bar", "Foo Bar");

        branchAssertions(preview, branch);

        workflow.publishBranch("foo bar");

        final Node live = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();

        assertTrue("live variant is expected to have branch info after publish since there was no live variant " +
                "before (otherwise only a live version in version history was expected).", live.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo bar", live.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Foo Bar", live.getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString());

        // publication is expected to result in a revision
        assertTrue(versionHistory.hasVersionLabel("foo bar-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("foo bar-published"));

        // when now editing, we expect the draft to also contain the branch info mixin
        workflow.obtainEditableInstance("foo bar");
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();

        assertTrue("draft variant is expected to have branch info after publish.", draft.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo bar", draft.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Foo Bar", draft.getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString());

    }

    private void branchAssertions(final Node preview, final Document branch) throws RepositoryException {
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        assertTrue(preview.isSame(branch.getNode(session)));
        assertEquals("After branch, we expect the 'master' to be versioned",
                2L, versionHistory.getAllVersions().getSize());

        assertEquals("Expected label 'master-unpublished' to be present", 1, versionHistory.getVersionLabels().length);

        final Version masterVersion = versionHistory.getVersionByLabel(MASTER_BRANCH_LABEL_UNPUBLISHED);

        assertFalse("Master version does not have a branch id", masterVersion.getFrozenNode().hasProperty(HIPPO_PROPERTY_BRANCH_ID));

        assertTrue("preview variant is expected to have branch info.", preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo bar", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Foo Bar", preview.getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString());

        assertTrue(handle.isNodeType(NT_HIPPO_VERSION_INFO));
        assertEquals("Expected handle version property to point to version history of preview node",
                handle.getProperty(HIPPO_VERSION_HISTORY_PROPERTY).getString(),
                preview.getProperty(JcrConstants.JCR_VERSION_HISTORY).getNode().getIdentifier());

        assertArrayEquals(new String[]{MASTER_BRANCH_ID, "foo bar"}, getMultipleStringProperty(handle, HippoNodeType.HIPPO_BRANCHES_PROPERTY, null));

    }

    @Test
    public void branch_when_there_is_only_live_creates_preview_then_versions_it_and_then_creates_branch() throws Exception {
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get();
        toBecomeLive.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        toBecomeLive.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeLive.removeMixin(MIX_VERSIONABLE);
        session.save();
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        // The actual branching!
        final Document branch = workflow.branch("foo bar", "Foo Bar");

        branchAssertions(WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get(), branch);
    }

    @Test
    public void branch_document_to_other_branch_results_in_extra_version() throws Exception {
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());

        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("foo bar", "Foo Bar");

        }

        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            // first checkout master to allow branching
            workflow.checkoutBranch(MASTER_BRANCH_ID);
            workflow.branch("bar lux", "Bar Lux");
        }

        assertEquals("bar lux", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Bar Lux", preview.getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString());

        assertEquals("After two times branching, we expect 3 versions",
                3L, versionHistory.getAllVersions().getSize());

        assertEquals("We expect the 'foo bar-unpublished' to be added as label since 'bar lux' branch was branched " +
                        "after 'foo bar",2, versionHistory.getVersionLabels().length);

        assertTrue(versionHistory.hasVersionLabel("foo bar-unpublished"));

        final BranchHandle branchHandle = new BranchHandleImpl("foo bar", handle);
        final DocumentVariant unpublishedFooBar = new DocumentVariant(branchHandle.getUnpublished());

        assertEquals("foo bar", unpublishedFooBar.getBranchId());
        assertEquals("Foo Bar", unpublishedFooBar.getBranchName());

        assertTrue(handle.isNodeType(NT_HIPPO_VERSION_INFO));
        assertEquals("Expected handle version property to point to version history of preview node",
                handle.getProperty(HIPPO_VERSION_HISTORY_PROPERTY).getString(),
                preview.getProperty(JcrConstants.JCR_VERSION_HISTORY).getNode().getIdentifier());

        assertArrayEquals(new String[]{MASTER_BRANCH_ID, "foo bar", "bar lux"}, getMultipleStringProperty(handle, HippoNodeType.HIPPO_BRANCHES_PROPERTY, null));

    }

    @Test
    public void branch_to_existing_branch_results_in_existing_branch_returned_and_placed_in_unpublished() throws Exception {
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        assertEquals("no master version yet",
                1L, versionHistory.getAllVersions().getSize());

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Document branch = workflow.branch("foo", "Foo");
        final Node unpublished = branch.getNode(session);
        Assertions.assertThat(unpublished.isSame(WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get()))
                .isTrue();

        Assertions.assertThat(unpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO))
                .isTrue();

        assertEquals("master version expected",
                2L, versionHistory.getAllVersions().getSize());

        final Document branchAgain = workflow.branch("foo", "Foo");

        final Node unpublishedAgain = branchAgain.getNode(session);
        Assertions.assertThat(unpublishedAgain.isSame(WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get()))
                .isTrue();

        assertEquals("No extra version expected since 'foo' was already the unpublished variant",
                2L, versionHistory.getAllVersions().getSize());

        // branch to 'bar'
        workflow.branch("bar", "Bar");

        assertEquals("Foo version expected",
                3L, versionHistory.getAllVersions().getSize());

        final Document fooBranch = workflow.branch("foo", "Foo");

        assertEquals("Extra version expected although 'foo' already existed but 'bar' should be versioned",
                4L, versionHistory.getAllVersions().getSize());

        Assertions.assertThat(fooBranch.getNode(session).getProperty(HIPPO_PROPERTY_BRANCH_ID).getString())
                .as("Expected 'foo' has become the unpublished")
                .isEqualTo("foo");

        workflow.branch("bar", "Bar");
        assertEquals("No extra version expected since 'bar' and 'foo' already exist and there were no changes",
                4L, versionHistory.getAllVersions().getSize());

        workflow.branch("foo", "Foo");
        assertEquals("No extra version expected since 'bar' and 'foo' already exist and there were no changes",
                4L, versionHistory.getAllVersions().getSize());

        // now modify 'foo' : Then a branch to 'bar' results in a checkout of 'bar' which should version the changed 'foo'
        workflow.obtainEditableInstance("foo");
        WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get().setProperty("title", "bar");
        session.save();
        workflow.commitEditableInstance();

        final Document barBranch = workflow.branch("bar", "bar");
        assertEquals("Extra version expected since 'foo' had changes and got replaced by 'bar'",
                5L, versionHistory.getAllVersions().getSize());

        Assertions.assertThat(barBranch.getNode(session).getProperty(HIPPO_PROPERTY_BRANCH_ID).getString())
                .as("Expected 'bar' has become the unpublished")
                .isEqualTo("bar");

    }

    @Test
    public void branch_to_id_that_already_exists_does_not_result_in_a_change() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");
        workflow.branch("foo", "FooAgain");
        Assertions.assertThat(WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get().getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString())
                .as("Name expected still to be Foo since branch already existed")
                .isEqualTo("Foo");
    }


    @Test
    public void branch_to_master_if_unpublished_is_already_master() throws Exception {

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        assertFalse("no master version yet", versionHistory.hasVersionLabel(MASTER_BRANCH_LABEL_UNPUBLISHED));

        assertEquals("no master version yet",
                1L, versionHistory.getAllVersions().getSize());
        assertFalse(versionHistory.hasVersionLabel(MASTER_BRANCH_LABEL_UNPUBLISHED));

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Document branch = workflow.branch(MASTER_BRANCH_ID, null);
        assertTrue(branch.getNode(session).isSame(WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get()));

        assertEquals("Branching to master if unpublished is already master should be NOOP",
                1L, versionHistory.getAllVersions().getSize());
        assertFalse(versionHistory.hasVersionLabel(MASTER_BRANCH_LABEL_UNPUBLISHED));

    }

    @Test
    public void missing_master_branch_in_version_history_is_possible_and_makes_further_branching_impossible()
            throws Exception {

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get();
        // set a property for 'master' version
        preview.setProperty("title", "Master title");
        session.save();

        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        // don't use workflow version to avoid master-unpublished to be added
        versionManager.checkpoint(preview.getPath());
        final VersionHistory versionHistory = versionManager.getVersionHistory(preview.getPath());

        assertEquals(2L, versionHistory.getAllVersions().getSize());
        assertFalse(versionHistory.hasVersionLabel(MASTER_BRANCH_LABEL_UNPUBLISHED));

        // now hardcode via JCR the preview to belong to 'foo' (not via workflow to create a right fixture (thus not using
        // workflow branch)
        preview.setProperty("title", "Foo title");

        preview.addMixin(HIPPO_MIXIN_BRANCH_INFO);
        preview.setProperty(HIPPO_PROPERTY_BRANCH_ID, "foo");
        preview.setProperty(HIPPO_PROPERTY_BRANCH_NAME, "Foo");
        session.save();



        // now try to branch to bar
        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            final Map<String, Serializable> hints = workflow.hints();
            assertFalse((Boolean) hints.get("branch"));
            workflow.branch("bar", "Bar");
            fail("Expected branching to be not possible since no Master branch");
        } catch (WorkflowException e) {
            assertEquals("Cannot invoke workflow documentworkflow action branch: action not allowed or undefined", e.getMessage());
        }
        // now try to checkout master
        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.checkoutBranch(MASTER_BRANCH_ID);
            fail("Expected checkout to Master not possible since no Master branch");
        } catch (WorkflowException e) {
            assertEquals("Cannot invoke workflow documentworkflow action checkoutBranch: action not allowed or undefined", e.getMessage());
        }

    }

    @Test
    public void branch_document_disabled_when_being_edited() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("branch"));

        assertTrue((Boolean)workflow.hints().get("branch"));

        workflow.obtainEditableInstance();

        assertFalse((Boolean)workflow.hints().get("branch"));

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.branch("foo", "foo");
        } catch (WorkflowException e) {
            assertEquals("Cannot invoke workflow documentworkflow action branch: action not allowed or undefined", e.getMessage());
        }
    }

    @Test
    public void branch_document_is_always_done_from_master_and_works_even_if_master_is_in_version_history() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.branch("foo", "Foo");
        workflow.obtainEditableInstance("foo");
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        draft.setProperty("title", "foo title");
        session.save();
        workflow.commitEditableInstance();

        final Node unpublished = WorkflowUtils.getDocumentVariantNode(handle, UNPUBLISHED).get();

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());

        assertTrue(Arrays.equals(new String[]{"master-unpublished"}, versionHistory.getVersionLabels()));

        // foo is unpublished and there is no version yet. Now branching 'bar' should branch from master, resulting in a
        // new version for 'foo' in version history

        workflow.branch("bar", "Bar");

        assertFalse("Expected branch from master which does not have the property title",
                unpublished.hasProperty("title"));

        assertTrue("Expected 'foo' to be versioned",
                Arrays.equals(new String[]{"master-unpublished", "foo-unpublished"}, versionHistory.getVersionLabels()));

    }

}
