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

import java.util.Arrays;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.hippoecm.repository.util.WorkflowUtils.getDocumentVariantNode;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

public class DocumentWorkflowDepublishBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void publish_branch_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("depublishBranch"));
        // depublish is disabled when there is no live variant
        assertFalse((Boolean) workflow.hints().get("depublishBranch"));

        workflow.publish();

        // depublish is enabled when there is a live variant
        assertTrue((Boolean) workflow.hints().get("depublishBranch"));

        // when document is being edited, you can still depublish
        workflow.obtainEditableInstance();
        assertTrue((Boolean) workflow.hints().get("depublishBranch"));
        assertFalse((Boolean) workflow.hints().get("depublish"));

        // when there is only a live version, depublishing branch is still possible
        WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get().remove();
        session.save();
        assertTrue((Boolean) workflow.hints().get("depublishBranch"));

    }

    @Test
    public void depublish_branch_when_the_published_version_is_master_assertions() throws Exception {

        final Node preview = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.publish();

        workflow.branch("foo", "Foo");
        workflow.publishBranch("foo");

        assertTrue(versionHistory.hasVersionLabel("foo-published"));

        workflow.depublishBranch("foo");

        assertFalse(versionHistory.hasVersionLabel("foo-published"));
        assertTrue(versionHistory.hasVersionLabel("foo-unpublished"));
    }

    @Test
    public void depublish_branch_when_the_published_version_is_for_branch_and_NO_other_published_versions_present_assertions() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");
        workflow.publishBranch("foo");

        Node published = getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertEquals("foo", published.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        workflow.depublishBranch("foo");

        assertArrayEquals(new String[]{}, JcrUtils.getMultipleStringProperty(published, HIPPO_AVAILABILITY, null));

        // there were no other live branches, so we expect no other version to have become live
        final Node unpublished = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());

        assertFalse(versionHistory.hasVersionLabel("foo-published"));
        assertTrue(versionHistory.hasVersionLabel("foo-unpublished"));
    }

    @Test
    public void depublish_branch_when_the_published_version_is_for_branch_and_master_published_exists() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.branch("foo", "Foo");
        workflow.publishBranch("foo");

        // also publish 'bar'
        workflow.checkoutBranch(MASTER_BRANCH_ID);

        workflow.branch("bar", "Bar");
        workflow.publishBranch("bar");

        // we cannot just publish master to get the DESIRED fixture for this integration test since that replaces the
        // live variant, hence we manually mark master-published to be live in version history
        workflow.checkoutBranch(MASTER_BRANCH_ID);
        final Node unpublished = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());
        versionHistory.addVersionLabel(versionHistory.getVersionByLabel("master-unpublished").getName(), "master-published", false);

        workflow.depublishBranch("foo");

        Node published = getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertArrayEquals(new String[]{"live"}, JcrUtils.getMultipleStringProperty(published, HIPPO_AVAILABILITY, null));

        // expected master to have become live and not bar!
        assertFalse(published.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        assertTrue(versionHistory.hasVersionLabel("master-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("master-published"));
        assertTrue(versionHistory.hasVersionLabel("bar-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("bar-published"));
        assertTrue(versionHistory.hasVersionLabel("foo-unpublished"));
        assertFalse(versionHistory.hasVersionLabel("foo-published"));

    }

    @Test
    public void depublish_branch_when_the_published_version_is_for_branch_and_OTHER_published_versions_present_assertions() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.branch("foo", "Foo");
        workflow.publishBranch("foo");

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        workflow.branch("bar", "Bar");
        workflow.publishBranch("bar");

        // there is no 'master' published : foo has the live variant. Depublishing foo should result in a bar having the
        // live variant

        workflow.depublishBranch("foo");

        final Node unpublished = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());
        versionHistory.addVersionLabel(versionHistory.getVersionByLabel("master-unpublished").getName(), "master-published", false);

        assertTrue(versionHistory.hasVersionLabel("bar-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("bar-published"));
        assertTrue(versionHistory.hasVersionLabel("foo-unpublished"));
        assertFalse(versionHistory.hasVersionLabel("foo-published"));

        final Node published = getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertTrue(published.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("bar", published.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
    }

    @Test
    public void depublish_branch_result_in_oldest_published_version_live_if_there_is_no_master() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.branch("foo", "Foo");
        workflow.publishBranch("foo");

        int oldestPublished = -1;
        final Random random = new Random();
        for (int i = 1; i < 10; i++) {
            workflow.checkoutBranch(MASTER_BRANCH_ID);
            workflow.branch("bar" + i, "Bar" + i);
            if (random.nextBoolean()) {
                workflow.publishBranch("bar" + i);
                if (oldestPublished == -1) {
                    oldestPublished = i;
                }
            }
            Thread.sleep(100);
        }

        if (oldestPublished == -1) {
            return;
        }

        workflow.depublishBranch("foo");

        final Node published = getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertTrue(published.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        // expected oldest version published to be restored
        assertEquals("bar" + oldestPublished, published.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // assert that after publication of barx, bar9 is restored as preview!

        final Node unpublished = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        assertEquals("bar9", unpublished.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
    }

    @Test
    public void depublish_branch_when_the_published_version_for_master_assertions() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.branch("foo", "Foo");
        workflow.publishBranch("foo");

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        // trigger a change in master to be able to publish
        workflow.obtainEditableInstance();
        workflow.commitEditableInstance();

        workflow.publish();

        assertTrue((Boolean)workflow.hints().get("depublishBranch"));
        assertTrue((Boolean)workflow.hints("master").get("depublishBranch"));
        assertTrue((Boolean)workflow.hints("foo").get("depublishBranch"));

        // as a result, the published version should have become MASTER, and version history still contains "foo-published"

        final Node published = getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertFalse(published.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        final Node unpublished = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());

        assertTrue(versionHistory.hasVersionLabel("master-published"));
        assertTrue(versionHistory.hasVersionLabel("foo-published"));

        workflow.depublishBranch("foo");

        assertTrue(versionHistory.hasVersionLabel("master-published"));
        assertFalse(versionHistory.hasVersionLabel("foo-published"));

        // live variant should still be there for master
        assertNotNull(getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get());

    }

    @Test
    public void depublish_branch_when_the_published_version_is_for_branch_and_MASTER_published_version_is_present_and_other_published_branches_assertions() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.branch("foo", "Foo");
        workflow.publishBranch("foo");

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        workflow.branch("bar", "Bar");
        workflow.publishBranch("bar");

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        // trigger a change to be able to publish
        workflow.obtainEditableInstance();
        workflow.commitEditableInstance();

        workflow.publish();

        // as a result, the published version should have become MASTER, and version history still contains "foo-published"

        final Node published = getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertFalse(published.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        final Node unpublished = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());

        assertTrue(versionHistory.hasVersionLabel("master-published"));
        assertTrue(versionHistory.hasVersionLabel("foo-published"));
        assertTrue(versionHistory.hasVersionLabel("bar-published"));

        workflow.depublishBranch("foo");

        assertTrue(versionHistory.hasVersionLabel("master-published"));
        assertFalse(versionHistory.hasVersionLabel("foo-published"));
        assertTrue(versionHistory.hasVersionLabel("bar-published"));
    }

    @Test
    public void depublish_branch_when_there_is_no_unpublished_for_other_branch_assertions() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");
        workflow.publishBranch("foo");

        Node preview = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        preview.remove();
        session.save();

        final Node published = getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        published.setProperty(HIPPO_AVAILABILITY, new String[]{"live", "preview"});
        session.save();

        assertEquals("foo", published.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.depublishBranch("bar");
            fail("Branch bar should not exist and throw exception when tried to be depublished");
        } catch (WorkflowException e) {
            assertEquals("Cannot invoke workflow documentworkflow action depublishBranch: action not allowed or undefined", e.getMessage());
        }

        workflow.depublishBranch("foo");

        assertTrue("Depublish keeps the published node!", getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).isPresent());
        assertTrue(published.isSame(getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get()));
        assertArrayEquals(new String[]{}, JcrUtils.getMultipleStringProperty(published, HIPPO_AVAILABILITY, null));

        // not a problem that the previously live variant is still for 'foo'
        assertEquals("foo", published.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        assertEquals("foo",
                getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get().getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // since we removed the 'preview' we do not have master any more. Manually restore preview to master
        preview = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).remove();
        preview.removeMixin(HIPPO_MIXIN_BRANCH_INFO);
        session.save();

        workflow.publish();
        assertFalse(published.hasProperty(HIPPO_PROPERTY_BRANCH_ID));
    }

    @Test
    public void depublish_non_existing_published_version_results_in_workflow_exception() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.depublishBranch("foo");
            fail("Published version does not exist so depublishing should not be possible");
        } catch (WorkflowException e) {
            assertEquals("Cannot invoke workflow documentworkflow action depublishBranch: action not allowed or undefined", e.getMessage());
        }
    }

    @Test
    public void depublish_master_published_version() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.publish();

        final Node unpublished = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());

        assertTrue(versionHistory.hasVersionLabel(MASTER_BRANCH_ID + "-published"));

        workflow.depublishBranch(MASTER_BRANCH_ID);

        final Node publishedVariant = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertTrue(Arrays.equals(new String[]{},
                JcrUtils.getMultipleStringProperty(publishedVariant, HippoNodeType.HIPPO_AVAILABILITY, new String[]{})));


        assertFalse(versionHistory.hasVersionLabel(MASTER_BRANCH_ID + "-published"));
    }

    @Test
    public void depublish_non_existing_branch_results_in_workflow_exception() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.publish();

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.depublishBranch("foo");
            fail("Branch 'foo' does not exist so depublishing should not be possible");
        } catch (WorkflowException e) {
            assertEquals("Cannot invoke workflow documentworkflow action depublishBranch: action not allowed or undefined", e.getMessage());
        }
    }

    @Test
    public void depublish_branch_foo_while_bar_published_exists_and_bar_unpublished_has_changes() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "foo");
        workflow.publishBranch("foo");

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        workflow.branch("bar", "Bar");
        workflow.publishBranch("bar");

        // make changes to unpublished variant
        workflow.obtainEditableInstance("bar");
        final Node draft = getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        draft.setProperty("title", "bar title");
        session.save();
        workflow.commitEditableInstance();

        final Node unpublished = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());
        assertEquals(5, versionHistory.getVersionLabels().length);
        assertTrue(versionHistory.hasVersionLabel("foo-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("foo-published"));
        assertTrue(versionHistory.hasVersionLabel("bar-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("bar-published"));
        assertTrue(versionHistory.hasVersionLabel("master-unpublished"));

        workflow.depublishBranch("foo");

        // as a result, we expect the 'bar' live version to be live. The 'bar' unpublished version is a different version
        // and is expected to be restored

        final Node published = getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();


        assertEquals("bar", published.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        // assert that NOT the unpublished from bar got published since it got changes
        assertFalse(published.hasProperty("title"));

        assertEquals("bar", unpublished.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // assert for bar unpublished and published point to a different version
        assertFalse(versionHistory.getVersionByLabel("bar-unpublished").isSame(versionHistory.getVersionByLabel("bar-published")));


        assertEquals(4, versionHistory.getVersionLabels().length);
        assertTrue(versionHistory.hasVersionLabel("foo-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("bar-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("bar-published"));
        assertTrue(versionHistory.hasVersionLabel("master-unpublished"));

    }

    @Test
    public void depublish_branch_foo_while_bar_published_exists_and_bar_unpublished_has_changes_and_being_edited() throws Exception {


        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "foo");
        workflow.publishBranch("foo");

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        workflow.branch("bar", "Bar");
        workflow.publishBranch("bar");

        // make changes to unpublished variant
        workflow.obtainEditableInstance("bar");
        final Node draft = getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        draft.setProperty("title", "bar title");
        session.save();
        workflow.commitEditableInstance();


        workflow.obtainEditableInstance("foo");
        draft.setProperty("title", "title in editing");
        draft.setProperty("summary", "summary in editing");

        final Node unpublished = getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());
        assertEquals(5, versionHistory.getVersionLabels().length);
        assertTrue(versionHistory.hasVersionLabel("foo-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("foo-published"));
        assertTrue(versionHistory.hasVersionLabel("bar-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("bar-published"));
        assertTrue(versionHistory.hasVersionLabel("master-unpublished"));

        workflow.depublishBranch("foo");
        session.save();

        // as a result, we expect the 'bar' live version to be live. The 'bar' unpublished version is a different version
        // and is expected to be restored

        final Node published = getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();


        assertEquals("bar", published.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        // assert that NOT the unpublished from bar got published since it got changes
        assertFalse(published.hasProperty("title"));

        // although bar has been published via 'unpublished' as a result of workflow.depublishBranch("foo");, we still
        // expect the unpublished to be 'foo' (since that should be restored)
        assertEquals("foo", unpublished.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // assert the unpublished doesn't have the 'title' property because *AFTER* the published 'bar' from version history
        // was restored and published, the unpublished 'foo' version should be restored

        assertFalse(unpublished.hasProperty("title"));

        // assert for bar unpublished and published point to a different version
        assertFalse(versionHistory.getVersionByLabel("bar-unpublished").isSame(versionHistory.getVersionByLabel("bar-published")));


        assertEquals(4, versionHistory.getVersionLabels().length);
        assertTrue(versionHistory.hasVersionLabel("foo-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("bar-unpublished"));
        assertTrue(versionHistory.hasVersionLabel("bar-published"));
        assertTrue(versionHistory.hasVersionLabel("master-unpublished"));

        assertTrue(draft.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("title in editing", draft.getProperty("title").getString());
        assertEquals("summary in editing", draft.getProperty("summary").getString());
    }

}
