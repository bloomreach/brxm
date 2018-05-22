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

import java.io.Serializable;
import java.util.Map;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.documentworkflow.DocumentVariant.MASTER_BRANCH_ID;

public class DocumentWorkflowPublicationBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void publication_branch_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Map<String, Serializable> hints = workflow.hints();
        assertTrue((Boolean)workflow.hints().get("publish"));
        assertTrue((Boolean)workflow.hints().get("requestPublication"));
        assertFalse((Boolean)workflow.hints().get("requestDepublication"));
        assertFalse((Boolean)workflow.hints().get("depublish"));

        workflow.branch("foo", "Foo");

        // since unpublished is now for branch, (de)publish and request(de)publication should be false
        assertFalse((Boolean)workflow.hints().get("publish"));
        assertFalse((Boolean)workflow.hints().get("requestPublication"));
        assertFalse((Boolean)workflow.hints().get("depublish"));
        assertFalse((Boolean)workflow.hints().get("requestDepublication"));

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.publish();
            fail("Expected workflow exception");
        } catch (WorkflowException e) {
            System.out.println(e.getMessage());
        }

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        assertTrue((Boolean)workflow.hints().get("publish"));
        assertTrue((Boolean)workflow.hints().get("requestPublication"));
        assertFalse((Boolean)workflow.hints().get("requestDepublication"));
        assertFalse((Boolean)workflow.hints().get("depublish"));

        workflow.publish();

        assertFalse((Boolean)workflow.hints().get("publish"));
        assertFalse((Boolean)workflow.hints().get("requestPublication"));
        assertTrue((Boolean)workflow.hints().get("requestDepublication"));
        assertTrue((Boolean)workflow.hints().get("depublish"));

        workflow.checkoutBranch("foo");

        assertFalse((Boolean)workflow.hints().get("publish"));
        assertFalse((Boolean)workflow.hints().get("requestPublication"));
        assertFalse((Boolean)workflow.hints().get("depublish"));
        assertFalse((Boolean)workflow.hints().get("requestDepublication"));


        workflow.checkoutBranch(MASTER_BRANCH_ID);

        // delete the unpublished manually
        WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get().remove();
        session.save();

        assertFalse((Boolean)workflow.hints().get("publish"));
        assertFalse((Boolean)workflow.hints().get("requestPublication"));
        assertTrue((Boolean)workflow.hints().get("depublish"));
        assertTrue((Boolean)workflow.hints().get("requestDepublication"));

    }

}
