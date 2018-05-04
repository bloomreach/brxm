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

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.documentworkflow.task.ListBranchesTask.CORE_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

public class DocumentWorkflowListBranchesTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void listBranches_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("listBranches"));
        assertTrue((Boolean)workflow.hints().get("listBranches"));

        // when there is only a live version, list branches is still possible (and will create a preview first)
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        toBecomeLive.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        toBecomeLive.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeLive.removeMixin(MIX_VERSIONABLE);
        session.save();
        assertTrue((Boolean)workflow.hints().get("listBranches"));

        // when document is being edited, listBranches is still availabel
        workflow.obtainEditableInstance();
        assertTrue((Boolean)workflow.hints().get("listBranches"));
    }

    @Test
    public void listBranches_various_use_cases() throws Exception {

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            final Set<String> branches = workflow.listBranches();
            assertEquals(1, branches.size());
            assertTrue(branches.contains(CORE_BRANCH_ID));
            assertFalse(versionHistory.hasVersionLabel("core-preview"));
        }

        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.version();
            final Set<String> branches = workflow.listBranches();
            assertEquals(1, branches.size());
            assertTrue(branches.contains(CORE_BRANCH_ID));
            assertTrue(versionHistory.hasVersionLabel("core-preview"));
        }

        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("foo", "Foo");
            final Set<String> branches = workflow.listBranches();
            assertEquals(2, branches.size());
            assertTrue(branches.contains(CORE_BRANCH_ID));
            assertTrue(branches.contains("foo"));

            // even though 'foo' is a branch, it is not yet part of version history: Just the preview variant is for branch
            // 'foo'
            assertFalse(versionHistory.hasVersionLabel("foo-preview"));
        }

        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("bar", "Bar");
            final Set<String> branches = workflow.listBranches();
            assertEquals(3, branches.size());
            assertTrue(branches.contains(CORE_BRANCH_ID));
            assertTrue(branches.contains("foo"));
            assertTrue(branches.contains("bar"));
        }

        // check out does not result in extra branch
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.checkoutBranch("foo");
            final Set<String> branches = workflow.listBranches();
            assertEquals(3, branches.size());
        }
    }

    @Test
    public void listBranches_when_there_is_only_live() throws Exception {
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        toBecomeLive.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        toBecomeLive.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeLive.removeMixin(MIX_VERSIONABLE);
        session.save();
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            final Set<String> branches = workflow.listBranches();
            assertEquals(1, branches.size());
            assertTrue(branches.contains(CORE_BRANCH_ID));
        }
    }

    @Test
    public void listBranches_when_there_is_only_draft() throws Exception {
        final Node toBecomeDraft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        toBecomeDraft.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.DRAFT);
        toBecomeDraft.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeDraft.removeMixin(MIX_VERSIONABLE);
        session.save();
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            final Set<String> branches = workflow.listBranches();
            assertEquals(1, branches.size());
            assertTrue(branches.contains(CORE_BRANCH_ID));
        }
    }

    @Test
    public void listBranches_only_returns_branches_which_are_real_branches() throws Exception {
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        final Version v1 = versionManager.checkpoint(preview.getPath());
        versionManager.getVersionHistory(preview.getPath()).addVersionLabel(v1.getName(), "noRealBranch-preview", true);

        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            final Set<String> branches = workflow.listBranches();
            assertEquals(1, branches.size());
            assertTrue(branches.contains(CORE_BRANCH_ID));
            assertFalse(branches.contains("noRealBranch"));
        }
    }
}
