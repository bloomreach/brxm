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

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_HOLDER;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

public class DocumentWorkflowCheckoutBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void checkout_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("checkoutBranch"));
        assertTrue((Boolean)workflow.hints().get("checkoutBranch"));

        // when there is only a live version, checkout is NOT possible
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        toBecomeLive.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        toBecomeLive.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeLive.removeMixin(MIX_VERSIONABLE);
        session.save();
        assertFalse((Boolean)workflow.hints().get("checkoutBranch"));

        // when editing, checkoutbranch is not supported
        workflow.obtainEditableInstance();
        assertFalse((Boolean)workflow.hints().get("checkoutBranch"));

        workflow.commitEditableInstance();
        assertTrue((Boolean)workflow.hints().get("checkoutBranch"));
    }

    @Test
    public void checkout_existing_branch() throws Exception {
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        assertFalse("No draft yet",
                WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).isPresent());

        // below triggers the core-preview to be versioned
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("foo", "Foo");
        }

        assertTrue(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());

        assertTrue(versionHistory.hasVersionLabel("core-preview"));

        final long numberOfVersions = versionHistory.getAllVersions().getSize();

        // now we should be able to checkout core
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.checkoutBranch("core");
        }

        assertFalse(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertFalse(preview.hasProperty(HIPPO_PROPERTY_BRANCH_ID));

        // as a result of the checkout we expect also 'foo' to be checkin into version history
        assertEquals(numberOfVersions + 1, versionHistory.getAllVersions().getSize());

        assertTrue(versionHistory.hasVersionLabel("foo-preview"));

        // checkout foo again
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.checkoutBranch("foo");
        }
        assertTrue(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // as a result of the checkout of 'foo' we exect a checkin of core
        assertEquals(numberOfVersions + 2, versionHistory.getAllVersions().getSize());
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.obtainEditableInstance();
        }
        final Optional<Node> draftVariant = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT);
        assertTrue(draftVariant.isPresent());

        final Node draft = draftVariant.get();
        assertTrue(draftVariant.get().isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo",draftVariant.get().getProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID).getString());

        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
                workflow.checkoutBranch("core");
                fail("Checkout expected to fail because of editing state of preview");
            } catch (WorkflowException e) {
                assertEquals("Cannot invoke workflow documentworkflow action checkoutBranch: action not allowed or undefined",
                        e.getMessage());
            }
        }

        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            draft.setProperty("title", "Foo title");
            session.save();
            assertEquals("admin", draft.getProperty(HIPPOSTD_HOLDER).getString());
            workflow.commitEditableInstance();
            assertFalse(draft.hasProperty(HIPPOSTD_HOLDER));
        }

        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            // after commitEditableInstance, we are not editing any more and should be able to checkout the core again.
            workflow.checkoutBranch("core");
        }
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.obtainEditableInstance();
        }

        final Node draft2 = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        assertFalse(draft2.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
    }

    @Test
    public void checkout_non_existing_branch_results_in_workflow_exception() throws Exception {
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        final long numberOfVersions = versionHistory.getAllVersions().getSize();

        // below triggers the core-preview to be versioned
        {
            try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
                final DocumentWorkflow workflow = getDocumentWorkflow(handle);
                workflow.checkoutBranch("foo");
            } catch (WorkflowException e) {
                assertEquals("Branch 'foo' cannot be checked out because it doesn't exist",
                        e.getMessage());
                assertEquals("In case of a failed checkout, no new version should be created",
                        numberOfVersions, versionHistory.getAllVersions().getSize());
            }
        }
    }

    @Test
    public void checkout_of_branch_which_is_already_preview_is_NOOP() throws Exception {
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        // below triggers the core-preview to be versioned
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("foo", "Foo");
        }
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(preview.getPath());
        final long numberOfVersions = versionHistory.getAllVersions().getSize();

        // checkout branch which is already checked out should be NOOP
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            final Document document = workflow.checkoutBranch("foo");
            assertTrue("Even NOOP should return a Document wrapping the preview node",
                    document.getNode(session).isSame(preview));
        }
        assertEquals("In case the preview is already checked out, no new version should have been created",
                numberOfVersions, versionHistory.getAllVersions().getSize());
    }
}
