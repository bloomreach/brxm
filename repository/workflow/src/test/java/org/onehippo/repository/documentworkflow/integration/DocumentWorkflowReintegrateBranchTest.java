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
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.apache.commons.lang3.ArrayUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_HOLDER;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.documentworkflow.DocumentVariant.CORE_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

public class DocumentWorkflowReintegrateBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void reintegrate_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("reintegrateBranch"));
        // Reintegrate is enabled when there is a preview
        assertTrue((Boolean) workflow.hints().get("reintegrateBranch"));

        // create a branch
        workflow.branch("foo", "Foo");

        assertTrue((Boolean) workflow.hints().get("reintegrateBranch"));

        // when document is being edited, you can still reintegrate
        workflow.obtainEditableInstance();
        assertTrue((Boolean) workflow.hints().get("reintegrateBranch"));

        // when there is only a live version, reintegrating branch is not possible
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        toBecomeLive.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        toBecomeLive.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeLive.removeMixin(MIX_VERSIONABLE);
        session.save();
        assertFalse((Boolean) workflow.hints().get("reintegrateBranch"));

    }

    @Test
    public void reintegrate_core_is_not_allowed() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        // create a branch
        workflow.branch("foo", "Foo");

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.reintegrateBranch(DocumentVariant.CORE_BRANCH_ID, true);
        } catch (WorkflowException e) {

                assertEquals("Branch 'core' cannot be reintegrated", e.getMessage());
        }
    }

    @Test
    public void reintegrate_non_existing_branch() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.reintegrateBranch("foo", true);
        } catch (WorkflowException e) {
            assertEquals("Branch 'foo' cannot be reintegrated because it doesn't exist",
                    e.getMessage());
        }
    }

    /**
     * Standard scenario where the current preview is for branch 'foo' and foo gets reintegrated (and published)
     */
    @Test
    public void standard_reintegration_scenario_1_assertions() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");

        workflow.obtainEditableInstance();
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        // set a property for 'core' version
        draft.setProperty("title", "Foo title");
        session.save();
        workflow.commitEditableInstance();

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        assertEquals("Foo title",
                preview.getProperty("title").getString());

        assertEquals(2, workflow.listBranches().size());
        assertTrue(workflow.listBranches().contains(CORE_BRANCH_ID));
        assertTrue(workflow.listBranches().contains("foo"));

        workflow.reintegrateBranch("foo", true);

        afterReintegrationAssertions(workflow, preview);

    }

    private void afterReintegrationAssertions(final DocumentWorkflow workflow, final Node preview) throws WorkflowException, RepositoryException {
        assertEquals(1, workflow.listBranches().size());
        assertTrue(workflow.listBranches().contains(CORE_BRANCH_ID));

        // assert live has changed as a result of workflow.reintegrateBranch("foo", true);
        assertEquals("Foo title",
                WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get().getProperty("title").getString());

        // assert version history does contain core-preview and core-live labels and not foo-preview and foo-live

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        assertFalse("Core was not live before", versionHistory.hasVersionLabel("pre-reintegrate-core-live-1"));
        assertTrue(versionHistory.hasVersionLabel("pre-reintegrate-core-preview-1"));
        assertTrue(versionHistory.hasVersionLabel("core-live"));
        assertTrue(versionHistory.hasVersionLabel("core-preview"));
        assertFalse(versionHistory.hasVersionLabel("foo-live"));
        assertFalse(versionHistory.hasVersionLabel("foo-preview"));

        final Version version = versionHistory.getVersionByLabel("pre-reintegrate-core-preview-1");
        final Version version1 = versionHistory.getVersionByLabel("core-live");
        final Version version2 = versionHistory.getVersionByLabel("core-preview");

        assertFalse(version.isSame(version1));
        assertTrue(version1.isSame(version2));

        assertFalse(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
    }

    /**
     * Standard scenario where the current preview is for branch 'core' and foo gets reintegrated (and published)
     * As a result, the old preview core will be versioned with label pre-reintegrate-core-preview-1
     */
    @Test
    public void standard_reintegration_scenario_2_assertions() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.branch("foo", "Foo");
        workflow.obtainEditableInstance();
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        // set a property for 'core' version
        draft.setProperty("title", "Foo title");
        session.save();
        workflow.commitEditableInstance();

        workflow.checkoutBranch("core");

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        assertFalse(preview.hasProperty("title"));

        workflow.reintegrateBranch("foo", true);

        afterReintegrationAssertions(workflow, preview);

    }

    /**
     * Standard scenario where the current preview is for branch 'bar' and foo gets reintegrated (and published)
     * As a result, the old preview core will be versioned with label pre-reintegrate-core-preview-1
     */
    @Test
    public void standard_reintegration_scenario_3_assertions() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.branch("foo", "Foo");
        workflow.obtainEditableInstance();
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        // set a property for 'core' version
        draft.setProperty("title", "Foo title");
        session.save();
        workflow.commitEditableInstance();

        workflow.checkoutBranch("core");
        workflow.branch("bar", "Bar");

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        assertFalse(preview.hasProperty("title"));

        workflow.reintegrateBranch("foo", true);

        // to have the same assertions, remove the 'bar' branch again
        workflow.removeBranch("bar");
        afterReintegrationAssertions(workflow, preview);
    }

    @Test
    public void reintegrate_when_there_is_no_core_makes_the_reintegratee_core() throws Exception {
        createFooBranchWithMissingCoreBranch();
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        {
            final String[] versionLabels = versionHistory.getVersionLabels();
            assertEquals(0, versionLabels.length);
            assertFalse(ArrayUtils.contains(versionLabels, "core-preview"));
            assertFalse(ArrayUtils.contains(versionLabels, "core-live"));
        }

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.reintegrateBranch("foo", true);
        {
            final String[] versionLabels = versionHistory.getVersionLabels();
            assertEquals(2, versionLabels.length);
            assertTrue(ArrayUtils.contains(versionLabels, "core-preview"));
            assertTrue(ArrayUtils.contains(versionLabels, "core-live"));
            // since there was NO core before the reintegrate, there is no "pre-reintegrate-core-preview-1" label
            assertFalse(ArrayUtils.contains(versionLabels, "pre-reintegrate-core-preview-1"));
        }
    }

    private void createFooBranchWithMissingCoreBranch() throws RepositoryException {
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        // set a property for 'core' version
        preview.setProperty("title", "Core title");
        session.save();

        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        // don't use workflow version to avoid core-preview to be added
        versionManager.checkpoint(preview.getPath());
        final VersionHistory versionHistory = versionManager.getVersionHistory(preview.getPath());
        assertFalse(versionHistory.hasVersionLabel("core-preview"));

        // now hardcode via JCR the preview to belong to 'foo' (not via workflow to create a right fixture (thus not using
        // workflow branch)
        preview.setProperty("title", "Foo title");
        preview.addMixin(HIPPO_MIXIN_BRANCH_INFO);
        preview.setProperty(HIPPO_PROPERTY_BRANCH_ID, "foo");
        preview.setProperty(HIPPO_PROPERTY_BRANCH_NAME, "Foo");
        handle.addMixin(HippoNodeType.NT_HIPPO_VERSION_INFO);
        handle.setProperty(HippoNodeType.HIPPO_BRANCHES_PROPERTY, new String[]{"foo"});
        session.save();
    }

    /**
     * A reintegrate should not conflict with a document being edited. Also after saving the draft, the preview will be
     * correctly replaced again by the draft.
     */
    @Test
    public void reintegrate_is_even_allowed_when_document_is_being_edited() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.branch("foo", "Foo");
        workflow.obtainEditableInstance();
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        // set a property for 'core' version
        draft.setProperty("title", "Foo title");
        session.save();
        workflow.commitEditableInstance();

        workflow.checkoutBranch("core");
        assertFalse(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        // the draft can still have the 'foo' mixin since on next edit it will be replaced
        assertTrue(draft.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo", draft.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        workflow.branch("bar", "Bar");

        assertTrue(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        // obtain editable instance for branch 'bar'
        workflow.obtainEditableInstance();
        assertTrue(draft.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        assertFalse(preview.hasProperty("title"));
        assertFalse(draft.hasProperty("title"));

        draft.setProperty("summary", "Bar summary");
        session.save();

        workflow.reintegrateBranch("foo", true);

        // the preview has been reintegrated
        assertFalse(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("Foo title", preview.getProperty("title").getString());
        final Node live = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertFalse(live.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("Foo title", preview.getProperty("title").getString());

        // draft is still for branch 'bar' and there is still a holder!!!
        assertTrue(draft.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("bar", draft.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("admin", draft.getProperty(HIPPOSTD_HOLDER).getString());
        assertFalse(draft.hasProperty("title"));

        // commit the draft
        workflow.commitEditableInstance();
        assertTrue(draft.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertTrue(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("bar", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Bar summary", preview.getProperty("summary").getString());
        assertFalse(preview.hasProperty("title"));

        workflow.reintegrateBranch("bar", true);

        // reintegrate removed the 'bar' branch as well
        assertFalse(live.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertFalse(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertFalse(draft.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        workflow.obtainEditableInstance();
        assertFalse(draft.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        // since initially there was no core live, there never will be a 'pre-reintegrate-core-live-1' label
        assertFalse("Core was not live before", versionHistory.hasVersionLabel("pre-reintegrate-core-live-1"));
        assertTrue(versionHistory.hasVersionLabel("pre-reintegrate-core-preview-1"));
        assertTrue(versionHistory.hasVersionLabel("pre-reintegrate-core-live-2"));
        assertTrue(versionHistory.hasVersionLabel("pre-reintegrate-core-preview-2"));
        assertTrue(versionHistory.hasVersionLabel("core-live"));
        assertTrue(versionHistory.hasVersionLabel("core-preview"));
        assertFalse(versionHistory.hasVersionLabel("foo-live"));
        assertFalse(versionHistory.hasVersionLabel("foo-preview"));
        assertFalse(versionHistory.hasVersionLabel("bar-live"));
        assertFalse(versionHistory.hasVersionLabel("bar-preview"));

    }

}
