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
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.documentworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

public class DocumentWorkflowRemoveBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void branch_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("removeBranch"));
        assertTrue((Boolean) workflow.hints().get("removeBranch"));

        // when document is being edited, you cannot remove branch
        workflow.obtainEditableInstance();
        assertFalse((Boolean) workflow.hints().get("removeBranch"));

        workflow.commitEditableInstance();
        assertTrue((Boolean) workflow.hints().get("removeBranch"));

        // when there is only a live version, removing branch is not possible
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        toBecomeLive.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        toBecomeLive.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeLive.removeMixin(MIX_VERSIONABLE);
        session.save();
        assertFalse((Boolean) workflow.hints().get("removeBranch"));

    }

    @Test
    public void remove_branch_removes_branch_info_from_all_variants_and_handle_if_the_branch_matches() throws Exception {

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        // The actual branching!
        workflow.branch("foo", "Foo");

        assertTrue(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        assertHandleBranchesProperty(handle, new String[]{MASTER_BRANCH_ID, "foo"});

        workflow.publishBranch("foo");

        final Node live = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertTrue(live.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo", live.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // now branch the preview to bar. When we now remove the branch 'foo' we expect it only to be removed from the
        // live version
        workflow.branch("bar", "Bar");

        assertHandleBranchesProperty(handle, new String[]{MASTER_BRANCH_ID, "foo", "bar"});

        assertTrue(live.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertTrue(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        // live is still foo
        assertEquals("foo", live.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        // preview is bar
        assertEquals("bar", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        assertHandleBranchesProperty(handle, new String[]{MASTER_BRANCH_ID, "foo", "bar"});

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());

        assertTrue(versionHistory.hasVersionLabel("foo-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("foo-published"));

        workflow.removeBranch("foo");

        assertHandleBranchesProperty(handle, new String[]{MASTER_BRANCH_ID, "bar"});

        // branch should have been removed from live
        assertFalse(live.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        // preview was for branch bar which should not have been removed
        assertTrue(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        // preview is bar
        assertEquals("bar", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // version history labels should not have 'foo-unpublished' or 'foo-published' any more
        assertFalse(versionHistory.hasVersionLabel("foo-unpublished"));
        assertFalse(versionHistory.hasVersionLabel("foo-published"));

        assertHandleBranchesProperty(handle, new String[]{MASTER_BRANCH_ID, "bar"});

        // publish branch bar
        workflow.obtainEditableInstance();
        workflow.commitEditableInstance();


        workflow.publishBranch("bar");

        // since there was already live, for 'bar' we only expect a new version in version history. Published variant
        // should still remain for core
        assertFalse(live.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        // preview is bar
        assertEquals("bar", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        assertTrue(versionHistory.hasVersionLabel("bar-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("bar-published"));

        workflow.removeBranch("bar");

        assertHandleBranchesProperty(handle, new String[]{MASTER_BRANCH_ID});

        assertFalse(live.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertFalse(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        assertFalse(versionHistory.hasVersionLabel("bar-unpublished"));
        assertFalse(versionHistory.hasVersionLabel("bar-published"));

    }

    private void assertHandleBranchesProperty(final Node handle, final String[] expected) throws RepositoryException {
        assertArrayEquals(expected, getMultipleStringProperty(handle, HippoNodeType.HIPPO_BRANCHES_PROPERTY, null));
    }

    @Test
    public void remove_master_is_not_allowed() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.removeBranch(MASTER_BRANCH_ID);
        } catch (WorkflowException e) {
            assertEquals("Cannot remove 'master' branch.", e.getMessage());
        }
    }
}
