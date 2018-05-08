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

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.util.JcrConstants;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

public class DocumentWorkflowBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void branch_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("branch"));
        assertTrue((Boolean)workflow.hints().get("branch"));

        // when there is only a live version, branching is still possible (and will create a preview first)
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
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
    }

    @Test
    public void branch_when_there_is_a_preview() throws Exception {

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        assertTrue("Expected preview only below handle",preview.isSame(handle.getNode(handle.getName())));

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        assertEquals("Only 'jcr:rootVersion' version expected", 1L, versionHistory.getAllVersions().getSize());

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        // The actual branching!
        final Document branch = workflow.branch("foo bar", "Foo Bar");

        branchAssertions(preview, branch);

        workflow.publish();

        final Node live = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();

        assertTrue("live variant is expected to have branch info after publish.", live.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo bar", live.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Foo Bar", live.getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString());

        // publication is expected to result in a revision
        assertTrue(versionHistory.hasVersionLabel("foo bar-preview"));
        assertTrue(versionHistory.hasVersionLabel("foo bar-live"));

        // when now editing, we expect the draft to also contain the branch info mixin
        workflow.obtainEditableInstance();
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

        assertEquals("Expected label 'core-preview' to be present", 1, versionHistory.getVersionLabels().length);

        final Version coreVersion = versionHistory.getVersionByLabel("core-preview");

        assertFalse("Core version does not have a branch id", coreVersion.getFrozenNode().hasProperty(HIPPO_PROPERTY_BRANCH_ID));

        assertTrue("preview variant is expected to have branch info.", preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo bar", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Foo Bar", preview.getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString());

        assertTrue(handle.isNodeType(NT_HIPPO_VERSION_INFO));
        assertEquals("Expected handle version property to point to version history of preview node",
                handle.getProperty(HIPPO_VERSION_HISTORY_PROPERTY).getString(),
                preview.getProperty(JcrConstants.JCR_VERSION_HISTORY).getNode().getIdentifier());

        assertArrayEquals(new String[]{"core", "foo bar"}, getMultipleStringProperty(handle, HippoNodeType.HIPPO_BRANCHES_PROPERTY, null));

    }

    @Test
    public void branch_when_there_is_only_live_creates_preview_then_versions_it_and_then_creates_branch() throws Exception {
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        toBecomeLive.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        toBecomeLive.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeLive.removeMixin(MIX_VERSIONABLE);
        session.save();
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        // The actual branching!
        final Document branch = workflow.branch("foo bar", "Foo Bar");

        branchAssertions(WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get(), branch);
    }

    @Test
    public void branch_document_to_other_branch_results_in_extra_version() throws Exception {
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("foo bar", "Foo Bar");
        }
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("bar lux", "Bar Lux");
        }

        assertEquals("bar lux", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Bar Lux", preview.getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString());

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        assertEquals("After two times branching, we expect 3 versions",
                3L, versionHistory.getAllVersions().getSize());

        assertEquals("We expect the 'foo bar-preview' to be added as label since 'bar lux' branch was branched " +
                        "after 'foo bar",2, versionHistory.getVersionLabels().length);

        assertTrue(versionHistory.hasVersionLabel("foo bar-preview"));
        final Version fooBar = versionHistory.getVersionByLabel("foo bar-preview");

        assertEquals("foo bar", fooBar.getFrozenNode().getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        assertEquals("Foo Bar", fooBar.getFrozenNode().getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString());

        assertTrue(handle.isNodeType(NT_HIPPO_VERSION_INFO));
        assertEquals("Expected handle version property to point to version history of preview node",
                handle.getProperty(HIPPO_VERSION_HISTORY_PROPERTY).getString(),
                preview.getProperty(JcrConstants.JCR_VERSION_HISTORY).getNode().getIdentifier());

        assertArrayEquals(new String[]{"core", "foo bar", "bar lux"}, getMultipleStringProperty(handle, HippoNodeType.HIPPO_BRANCHES_PROPERTY, null));

    }

    @Test
    public void branch_document_to_existing_branch_is_not_allowed() throws Exception {
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            final Document branch = workflow.branch("foo bar", "Foo Bar");
            assertTrue(preview.isSame(branch.getNode(session)));
        }
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        assertFalse(versionHistory.hasVersionLabel("foo bar-preview"));
        {


            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
                workflow.branch("foo bar", "Foo Bar");
                fail("Branch already exists so exception expected");
            } catch (WorkflowException e) {
                assertEquals("Branch 'foo bar' already exists",
                        e.getMessage());
                assertEquals("Branching to branch for which the preview already exists should not result in an extra version",
                        2L, versionHistory.getAllVersions().getSize());

                assertFalse(versionHistory.hasVersionLabel("foo bar-preview"));
            }
        }
    }

    @Test
    public void branch_to_core_is_in_general_not_needed() throws Exception {

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        assertFalse("no core version yet", versionHistory.hasVersionLabel("preview-core"));

        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
                workflow.branch("core", null);
                fail("Branching core is in general not needed and should not be possible when preview is for core");
            } catch (WorkflowException e) {
                assertEquals("Branch 'core' already exists",
                        e.getMessage());
                assertEquals("Branching to core is not allowed and should not have created version",
                        1L, versionHistory.getAllVersions().getSize());
                assertFalse(versionHistory.hasVersionLabel("core-preview"));
            }
        }
    }

    @Test
    public void missing_core_branch_in_version_history_is_possible()
            throws Exception {

        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        // set a property for 'core' version
        preview.setProperty("title", "Core title");
        session.save();

        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        // don't use workflow version to avoid core-preview to be added
        versionManager.checkpoint(preview.getPath());
        final VersionHistory versionHistory = versionManager.getVersionHistory(preview.getPath());

        assertEquals(2L, versionHistory.getAllVersions().getSize());
        assertFalse(versionHistory.hasVersionLabel("core-preview"));

        // now hardcode via JCR the preview to belong to 'foo' (not via workflow to create a right fixture (thus not using
        // workflow branch)
        preview.setProperty("title", "Foo title");

        preview.addMixin(HIPPO_MIXIN_BRANCH_INFO);
        preview.setProperty(HIPPO_PROPERTY_BRANCH_ID, "foo");
        preview.setProperty(HIPPO_PROPERTY_BRANCH_NAME, "Foo");
        session.save();

        // now branch to bar
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("bar", "Bar");
        }

        //  assert the handle does NOT have 'core' now as available since there never was a core! Also 'foo' was never
        // really a branch so also not present
        assertTrue(handle.isNodeType(NT_HIPPO_VERSION_INFO));
        assertArrayEquals(new String[]{"bar"}, getMultipleStringProperty(handle, HippoNodeType.HIPPO_BRANCHES_PROPERTY, null));


        assertFalse("Version history does not have core-preview.",
                versionHistory.hasVersionLabel("core-preview"));
        assertTrue("Branching should had added version label for foo-preview to existing version.",
                versionHistory.hasVersionLabel("foo-preview"));

        // Now branch to core
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("core", null);
        }

        assertFalse("for core the mixin should had been removed.",preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        assertTrue(versionHistory.hasVersionLabel("foo-preview"));
        assertTrue(versionHistory.hasVersionLabel("bar-preview"));
        assertFalse(versionHistory.hasVersionLabel("core-preview"));

        // after branching to 'lux' we expect a core-preview label
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("lux", "Lux");
        }

        assertTrue(versionHistory.hasVersionLabel("core-preview"));
        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("core", null);
            fail("Branching core should now not be possible");
        } catch (WorkflowException e) {
            assertTrue(versionHistory.hasVersionLabel("core-preview"));
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
}
