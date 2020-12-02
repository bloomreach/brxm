/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.documentworkflow.task.CopyDocumentTask;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_BRANCHES_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.hippoecm.repository.util.WorkflowUtils.getDocumentVariantNode;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DocumentWorkflowCopyTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void copy_hints() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");

        assertThat((Boolean)workflow.hints().get("copy")).isTrue();
        assertThat((Boolean)workflow.hints("foo").get("copy")).isTrue();
        assertThat(workflow.hints("bar").containsKey("copy"))
                .as("Branch bar does not exist so not copy action expected")
                .isFalse();
    }

    @Test
    public void copy_document_only_live_results_in_unpublished_new_doc() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node folder = handle.getParent();
        workflow.copy(new Document(folder), "newDoc");

        assertThat(folder.hasNode("newDoc")).isTrue();
        assertThat(getMultipleStringProperty(folder.getNode("newDoc").getNode("newDoc"), HIPPO_AVAILABILITY, null))
                .as("Expected new document to be preview")
                .containsOnlyOnce("preview") ;
    }

    @Test
    public void copy_document_preview_exists_copies_the_preview() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.obtainEditableInstance();
        final Node draft = getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        draft.setProperty("title", "my title");
        session.save();
        workflow.commitEditableInstance();

        final Node folder = handle.getParent();
        workflow.copy(new Document(folder), "newDoc");

        assertThat(folder.hasNode("newDoc")).isTrue();
        final Node newHandle = folder.getNode("newDoc");
        final Node newDocUnpublished = newHandle.getNode("newDoc");
        assertThat(getMultipleStringProperty(newDocUnpublished, HIPPO_AVAILABILITY, null))
                .as("Expected new document to be preview")
                .containsOnlyOnce("preview");

        assertThat(newDocUnpublished.isSame(getDocumentVariantNode(newHandle, WorkflowUtils.Variant.UNPUBLISHED).get())).isTrue();
        assertThat(newDocUnpublished.hasProperty("title")).isTrue();
    }

    @Test
    public void copy_non_existing_branch_results_in_action_not_allowed() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node folder = handle.getParent();
        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.copy(new Document(folder), "newDoc", "foo");
            fail("Expected copy of non existing branch to fail");
        } catch (WorkflowException e) {
            assertThat(e.getMessage()).isEqualTo("Cannot invoke workflow documentworkflow action copy: action not allowed or undefined");
        }

    }

    @Test
    public void copy_document_master_when_other_branch_is_the_unpublished_copies_master_from_history() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");
        workflow.obtainEditableInstance("foo");
        final Node draft = getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        draft.setProperty("title", "foo title");
        session.save();
        workflow.commitEditableInstance();

        final Node oldDocUnpublished = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        assertThat(oldDocUnpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO))
                .isTrue();

        final Node folder = handle.getParent();
        // below should copy master. Since unpublished is for 'foo' branch, we first expect a checkout
        workflow.copy(new Document(folder), "newDoc");

        final Node newHandle = folder.getNode("newDoc");
        final Node newDocUnpublished = newHandle.getNode("newDoc");

        assertThat(newDocUnpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO))
                .as("Expected 'master' to have been copied")
                .isFalse();

        assertThat(newDocUnpublished.hasProperty("title")).isFalse();

        assertThat(oldDocUnpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO))
                .as("Master is expected to be checked out")
                .isFalse();

        // expected is that the 'foo' unpublished got checked in as result of the copy of master
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(oldDocUnpublished.getPath());

        final Version version = versionHistory.getVersionByLabel("foo-" + UNPUBLISHED);
        assertThat(version.getFrozenNode().hasProperty("title")).isTrue();
        assertThat(version.getFrozenNode().getProperty("title").getString()).isEqualTo("foo title");
    }

    /**
     * TODO once CMS-14276 has been addressed, rename this test to
     * copy_document_foo_when_other_branch_is_the_unpublished_copies_foo_from_history_and_sets_branch_info_on_target
     */
    @Test
    public void copy_document_foo_when_other_branch_is_the_unpublished_copies_foo_from_history_and_sets_it_to_core()
            throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");
        workflow.obtainEditableInstance("foo");
        final Node draft = getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        draft.setProperty("title", "foo title");
        session.save();
        workflow.commitEditableInstance();

        workflow.obtainEditableInstance();
        draft.setProperty("title", "master title");
        session.save();

        final Node folder = handle.getParent();
        // copy 'foo' which is not the current unpublished
        workflow.copy(new Document(folder), "newDoc", "foo");

        final Node newHandle = folder.getNode("newDoc");
        final Node newDocUnpublished = newHandle.getNode("newDoc");


        // EXPECTATIONS FOR THE TARGET NODE
        // TODO CMS-14276
        if (CopyDocumentTask.COPY_BRANCH_TO_BRANCH_SUPPORTED) {
            assertThat(newHandle.isNodeType(HippoNodeType.NT_HIPPO_VERSION_INFO))
                    .isTrue();
            assertThat(newDocUnpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO))
                    .as("Expected 'foo' branch to have been copied")
                    .isTrue();
            assertThat(newDocUnpublished.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString()).isEqualTo("foo");
            assertThat(newDocUnpublished.getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString()).isEqualTo("Foo");
            assertThat(JcrUtils.getMultipleStringProperty(newHandle, HIPPO_BRANCHES_PROPERTY, null))
                    .containsExactly("foo");
        } else {
            assertThat(newDocUnpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO))
                    .as("As long as CMS-14276 is not done, we copy always to a 'master' version")
                    .isFalse();
            assertThat(newHandle.isNodeType(HippoNodeType.NT_HIPPO_VERSION_INFO))
                    .as("As long as CMS-14276 is not done, we copy always to a 'master' version")
                    .isFalse();
        }

        assertThat(newDocUnpublished.hasProperty("title")).isTrue();
        assertThat(newDocUnpublished.getProperty("title").getString()).isEqualTo("foo title");


        final VersionHistory newDocVersionHistory = session.getWorkspace().getVersionManager().getVersionHistory(newDocUnpublished.getPath());
        assertThat(newDocVersionHistory.getVersionLabels())
                .as("There should not be any version history for the new document")
                .isEmpty();

        assertThat(newDocVersionHistory.getAllVersions().getSize())
                .as("Only root version expected")
                .isEqualTo(1);


        // EXPECTATIONS FOR THE SOURCE NODE
        final Node oldDocUnpublished = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        assertThat(oldDocUnpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO))
                .as("'foo' branch is expected to be checked out")
                .isTrue();

        // expected is that the 'master' unpublished got checked in as result of the copy of master
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(oldDocUnpublished.getPath());

        final Version version = versionHistory.getVersionByLabel("foo-" + UNPUBLISHED);
        assertThat(version.getFrozenNode().hasProperty("title")).isTrue();
        assertThat(version.getFrozenNode().getProperty("title").getString()).isEqualTo("foo title");

    }

    @Test
    public void copy_document_only_transferable_draft_variant() throws Exception{
        document.setProperty(HippoStdNodeType.HIPPOSTD_TRANSFERABLE, true);
        document.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.DRAFT);
        session.save();

        final Node folder = handle.getParent();
        getDocumentWorkflow(handle).copy(new Document(folder), "newDoc");
        assertThat(folder.hasNode("newDoc")).isTrue();
        assertThat(folder.getNode("newDoc").getNode("newDoc").getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString())
                .as("Expected document to be a draft")
                .isEqualTo(HippoStdNodeType.DRAFT);
    }

    @Test( expected=WorkflowException.class)
    public void copy_document_only_not_transferable_draft_variant() throws Exception{
        document.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.DRAFT);
        session.save();

        final Node folder = handle.getParent();
        getDocumentWorkflow(handle).copy(new Document(folder), "newDoc");
    }

}
