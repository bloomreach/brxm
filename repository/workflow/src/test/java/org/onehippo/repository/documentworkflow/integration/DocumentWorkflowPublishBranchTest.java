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

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.WorkflowUtils.getDocumentVariantNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

public class DocumentWorkflowPublishBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void publish_branch_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("publishBranch"));
        // Publish is enabled when there is a preview
        assertTrue((Boolean) workflow.hints().get("publishBranch"));

        // create a branch
        workflow.branch("foo", "Foo");

        assertTrue((Boolean) workflow.hints().get("publishBranch"));

        // when document is being edited, you can still publish
        workflow.obtainEditableInstance();
        assertTrue((Boolean) workflow.hints().get("publishBranch"));
        assertFalse((Boolean) workflow.hints().get("publish"));

        // when there is only a live version, publishing branch is not possible
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        toBecomeLive.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        toBecomeLive.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeLive.removeMixin(MIX_VERSIONABLE);
        session.save();
        assertFalse((Boolean) workflow.hints().get("publishBranch"));

    }

    @Test
    public void publish_branch_assertions() throws Exception {
        assertFalse(getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).isPresent());

        final Node preview = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        // The actual branching!
        workflow.branch("foo", "Foo");

        assertTrue(workflow.hints().containsKey("publish"));
        assertFalse("Normal Publication should be disabled for branches", (Boolean)workflow.hints().get("publish"));

        workflow.publishBranch("foo");

        assertTrue(getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).isPresent());
        final Node live = getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertEquals("foo", live.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // now branch the preview to bar.
        workflow.branch("bar", "Bar");

        workflow.publishBranch("bar");

        // since there was already live, for 'bar' we only expect a new version in version history. Published variant
        // should still remain for foo
        assertEquals("foo", live.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        // preview is bar
        assertEquals("bar", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        assertEquals(5, versionHistory.getVersionLabels().length);
        assertTrue(versionHistory.hasVersionLabel("master-UNPUBLISHED"));
        assertTrue(versionHistory.hasVersionLabel("foo-UNPUBLISHED"));
        assertTrue(versionHistory.hasVersionLabel("foo-PUBLISHED"));
        assertTrue(versionHistory.hasVersionLabel("bar-UNPUBLISHED"));
        assertTrue(versionHistory.hasVersionLabel("bar-PUBLISHED"));

        // let's now remove the published version, create a branch 'lux1' and then create a branch 'lux2'. Then
        // start editing 'lux2' and after that, publish branch 'lux1' : We expect the unpublished to be replaced by 'lux1'
        // and the published to have become 'lux1' and the draft still for 'lux2' : After committing the editable instance
        // we expect the unpublished to become 'lux2'

        getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get().remove();
        session.save();

        workflow.branch("lux1", "Lux1");
        {
            final Node draft = workflow.obtainEditableInstance().getNode(session);
            draft.setProperty("title", "Lux 1");
            session.save();
        }
        workflow.commitEditableInstance();

        workflow.branch("lux2", "Lux2");
        {
            final Node draft = workflow.obtainEditableInstance().getNode(session);
            draft.setProperty("title", "Lux 2");
            session.save();
        }

        assertTrue(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("lux2", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // we do not commit the editable instance yet! Instead we try to publish the branch 'lux1' which as a result
        // should replace the preview AND create a live!

        workflow.publishBranch("lux1");
        // preview is expected to be replaced
        assertEquals("lux1", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Lux 1", preview.getProperty("title").getString());
        final Node live2 = getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertEquals("lux1", live2.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Lux 1", live2.getProperty("title").getString());

        final Node draft = getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        assertEquals("lux2", draft.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // now commit the editable instance (the draft which is still for 'lux2'
        workflow.commitEditableInstance();

        assertEquals("lux2", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Lux 2", preview.getProperty("title").getString());

        assertEquals(8, versionHistory.getVersionLabels().length);

        assertTrue(versionHistory.hasVersionLabel("master-UNPUBLISHED"));
        assertTrue(versionHistory.hasVersionLabel("foo-UNPUBLISHED"));
        assertTrue(versionHistory.hasVersionLabel("foo-PUBLISHED"));
        assertTrue(versionHistory.hasVersionLabel("bar-UNPUBLISHED"));
        assertTrue(versionHistory.hasVersionLabel("bar-PUBLISHED"));
        assertTrue(versionHistory.hasVersionLabel("lux1-UNPUBLISHED"));
        assertTrue(versionHistory.hasVersionLabel("lux1-PUBLISHED"));
        // because lux2 is being edited WHILE a publish branch of lux1 happened, there must have been created a version
        // of lux2 SINCE if the editor would discard the draft changes, she otherwise might have lost the lux2 branch
        // for good if there was not checked in version yet!
        assertTrue(versionHistory.hasVersionLabel("lux2-UNPUBLISHED"));
        assertFalse(versionHistory.hasVersionLabel("lux2-PUBLISHED"));

        // Expected 'Lux 1' for "lux2-UNPUBLISHED" : This might be confusing but the lux2 draft was not yet committed
        // when the publish branch happened, hence the preview as it was got pushed to version history.
        assertEquals("Lux 1",
                versionHistory.getVersionByLabel("lux2-UNPUBLISHED").getFrozenNode().getProperty("title").getString());

        // checkoutBranch lux1 triggers lux2 preview to be versioned with the committed changes
        workflow.checkoutBranch("lux1");
        assertEquals("Lux 2",
                versionHistory.getVersionByLabel("lux2-UNPUBLISHED").getFrozenNode().getProperty("title").getString());

        // now publish lux2 : Since there is already a live version (lux1), we only expect 'lux2-PUBLISHED' label in
        // version history to have been added and point to the same version as lux2-UNPUBLISHED

        workflow.publishBranch("lux2");

        // live should not have changed and still for lux1
        assertEquals("lux1", getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED)
                .get().getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // version history now also should have lux2-PUBLISHED

        assertEquals(9, versionHistory.getVersionLabels().length);
        assertTrue(versionHistory.hasVersionLabel("lux2-UNPUBLISHED"));
        assertTrue(versionHistory.hasVersionLabel("lux2-PUBLISHED"));
        assertTrue(versionHistory.getVersionByLabel("lux2-UNPUBLISHED").isSame(versionHistory.getVersionByLabel("lux2-PUBLISHED")));
    }

    @Test
    public void publish_non_existing_branch_results_in_workflow_exception() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.publishBranch("foo");
            fail("Branch 'foo' does not exist so publishing should not be possible");
        } catch (WorkflowException e) {
            assertEquals("Branch 'foo' cannot be published because it doesn't exist", e.getMessage());
        }

    }

}
