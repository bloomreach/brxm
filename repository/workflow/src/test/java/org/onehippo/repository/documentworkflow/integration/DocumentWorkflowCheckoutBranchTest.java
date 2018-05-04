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

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;

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

        // below triggers the core-preview to be versioned
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.branch("foo", "Foo");
        }

        assertTrue(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        // now we should be able to checkout core
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.checkoutBranch("core");
        }

        assertFalse(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertFalse(preview.hasProperty(HIPPO_PROPERTY_BRANCH_ID));

        // checkout foo again
        {
            final DocumentWorkflow workflow = getDocumentWorkflow(handle);
            workflow.checkoutBranch("foo");
        }
        assertTrue(preview.isNodeType(HIPPO_MIXIN_BRANCH_INFO));
        assertEquals("foo", preview.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.obtainEditableInstance();

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.checkoutBranch("core");
            fail("Checkout expected to fail because of editing state of preview");
        } catch (WorkflowException e) {
            assertEquals("Cannot invoke workflow documentworkflow action checkoutBranch: action not allowed or undefined",
                    e.getMessage());
        }
    }


    @Test
    public void checkout_non_existing_branch_results_in_workflow_exception() throws Exception {
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        // below triggers the core-preview to be versioned
        {
            try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
                final DocumentWorkflow workflow = getDocumentWorkflow(handle);
                workflow.checkoutBranch("foo");
            } catch (WorkflowException e) {
                assertEquals("version label 'foo-preview' does not exist in version history so cannot checkout branch 'foo'",
                        e.getMessage());
            }
        }
    }
}
