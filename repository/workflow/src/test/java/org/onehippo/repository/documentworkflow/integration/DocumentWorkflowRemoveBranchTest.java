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


import javax.jcr.Node;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import com.google.common.collect.ImmutableSet;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.Utilities;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_BRANCHES_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

public class DocumentWorkflowRemoveBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void branch_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("removeBranch"));
        // there are no branches yet
        assertFalse((Boolean) workflow.hints().get("removeBranch"));

        // master can never be removed as branch
        assertFalse((Boolean) workflow.hints("master").get("removeBranch"));

        // cannot remove a non existing branch
        assertFalse((Boolean) workflow.hints("foo").get("removeBranch"));

        workflow.branch("foo", "Foo");
        assertTrue((Boolean) workflow.hints("foo").get("removeBranch"));

        // when document is being edited, you can still try to remove a branch
        workflow.obtainEditableInstance();
        assertFalse((Boolean) workflow.hints("master").get("removeBranch"));
        assertTrue((Boolean) workflow.hints("foo").get("removeBranch"));

        workflow.commitEditableInstance();
        assertTrue((Boolean) workflow.hints("foo").get("removeBranch"));

        // when there is only a live version, removing branch is not possible
        WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get().remove();
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        toBecomeLive.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        toBecomeLive.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeLive.removeMixin(MIX_VERSIONABLE);
        session.save();
        assertFalse((Boolean) workflow.hints().get("removeBranch"));
        // foo is gone because was moved to version history after workflow.obtainEditableInstance();
        assertFalse((Boolean) workflow.hints("foo").get("removeBranch"));

        // manually make the published now for 'foo
        toBecomeLive.addMixin(HIPPO_MIXIN_BRANCH_INFO);
        toBecomeLive.setProperty(HIPPO_PROPERTY_BRANCH_ID, "foo");
        toBecomeLive.setProperty(HIPPO_PROPERTY_BRANCH_NAME, "Foo");
        session.save();

        // foo is live thus cannot be removed
        assertFalse((Boolean) workflow.hints("foo").get("removeBranch"));

        // after unpublish, the branch should be possible to be removed
        workflow.depublishBranch("foo");

        assertTrue((Boolean) workflow.hints("foo").get("removeBranch"));

        workflow.removeBranch("foo");

        assertFalse(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get().isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals(1, workflow.listBranches().size());
        assertTrue(workflow.listBranches().contains(MASTER_BRANCH_ID));
    }

    @Test
    public void non_existing_branch_cannot_be_removed() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.removeBranch("non-existing");
        } catch (WorkflowException e) {
            assertEquals("Cannot invoke workflow documentworkflow action removeBranch: action not allowed or undefined", e.getMessage());
        }
    }

    @Test
    public void branch_that_is_published_variant_cannot_be_removed() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");

        assertTrue(ImmutableSet.of("master", "foo").equals(workflow.listBranches()));

        workflow.publishBranch("foo");

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.removeBranch("foo");
        } catch (WorkflowException e) {
            assertEquals("Branch 'foo' cannot be removed because it is published.", e.getMessage());
        }
        assertTrue(ImmutableSet.of("master", "foo").equals(workflow.listBranches()));
    }

    @Test
    public void branch_that_is_published_in_version_history_cannot_be_removed() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        // publish core
        workflow.publish();

        workflow.branch("foo", "Foo");

        assertTrue(ImmutableSet.of("master", "foo").equals(workflow.listBranches()));

        workflow.publishBranch("foo");

        // assert published variant is not for 'foo'
        final Node published = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertFalse(published.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.removeBranch("foo");
        } catch (WorkflowException e) {
            assertEquals("Branch 'foo' cannot be removed because it is published.", e.getMessage());
        }
        assertTrue(ImmutableSet.of("master", "foo").equals(workflow.listBranches()));

        workflow.depublishBranch("foo");
        workflow.removeBranch("foo");
        assertTrue(ImmutableSet.of("master").equals(workflow.listBranches()));
    }

    @Test
    public void branch_that_is_being_edited_cannot_be_removed() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");
        assertTrue(ImmutableSet.of("master", "foo").equals(workflow.listBranches()));
        workflow.obtainEditableInstance("foo");

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.removeBranch("foo");
        } catch (WorkflowException e) {
            assertEquals("Branch 'foo' cannot be removed because being edited.", e.getMessage());
        }

        assertTrue(ImmutableSet.of("master", "foo").equals(workflow.listBranches()));
        workflow.commitEditableInstance();
        workflow.removeBranch("foo");
        assertTrue(ImmutableSet.of("master").equals(workflow.listBranches()));
    }

    @Test
    public void if_branch_to_remove_is_unpublished_and_changed_the_unpublished_gets_versioned_and_other_branch_is_checked_out() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");

        final Node unpublished = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        assertTrue(unpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());

        final long numberVersionsBefore = versionHistory.getAllVersions().getSize();

        workflow.removeBranch("foo");

        assertEquals("One extra version expected", numberVersionsBefore + 1, versionHistory.getAllVersions().getSize());

        assertFalse("Expected core to have become preview",unpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        workflow.branch("bar", "Bar");
        workflow.checkoutBranch(MASTER_BRANCH_ID);

        // now removing branch 'bar' should not result in extra version

        final long numberVersionsBeforeRemoveBar = versionHistory.getAllVersions().getSize();

        workflow.removeBranch("bar");

        assertEquals("No extra version expected", numberVersionsBeforeRemoveBar, versionHistory.getAllVersions().getSize());

    }

    @Test
    public void branch_can_be_removed_if_other_branch_is_being_edited() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");

        assertTrue(ImmutableSet.of("master", "foo").equals(workflow.listBranches()));

        // edit master
        workflow.checkoutBranch(MASTER_BRANCH_ID);
        workflow.obtainEditableInstance();

        workflow.removeBranch("foo");

        assertTrue(ImmutableSet.of("master").equals(workflow.listBranches()));

        workflow.commitEditableInstance();

        workflow.branch("foo", "Foo");
        workflow.checkoutBranch(MASTER_BRANCH_ID);
        workflow.branch("bar", "Bar");
        workflow.obtainEditableInstance();
        workflow.removeBranch("foo");

        assertTrue(ImmutableSet.of("master", "bar").equals(workflow.listBranches()));
    }

    @Test
    public void remove_master_is_not_allowed() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.removeBranch(MASTER_BRANCH_ID);
        } catch (WorkflowException e) {
            assertEquals("Cannot invoke workflow documentworkflow action removeBranch: action not allowed or undefined", e.getMessage());
        }
    }

    @Test
    public void remove_branch_when_no_master_or_other_branches_exist_result_in_branch_version_to_become_master() throws Exception {
        // manually setup fixture to avoid an existing for MASTER
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        preview.addMixin(HIPPO_MIXIN_BRANCH_INFO);
        preview.setProperty(HIPPO_PROPERTY_BRANCH_ID, "foo");
        preview.setProperty(HIPPO_PROPERTY_BRANCH_NAME, "Foo");
        handle.addMixin(NT_HIPPO_VERSION_INFO);
        handle.setProperty(HIPPO_BRANCHES_PROPERTY, new String[]{"foo"});
        session.save();

        final Node unpublished = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.removeBranch("foo");

        assertFalse(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
    }
}
