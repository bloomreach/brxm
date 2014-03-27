/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.content.beans.manager.workflow;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.junit.Assert.assertEquals;

public class TestBaseWorkflowCallbackHandler {

    @Test
    public void testWorkflowType() {
        assertEquals(Workflow.class, new BaseWorkflowCallbackHandler() {
            public void processWorkflow(final Workflow workflow) throws Exception {}
        }.getWorkflowType());
        assertEquals(Workflow.class, new BaseWorkflowCallbackHandler<Workflow>() {
            public void processWorkflow(final Workflow workflow) throws Exception {}
        }.getWorkflowType());
        assertEquals(EditableWorkflow.class, new BaseWorkflowCallbackHandler<EditableWorkflow>() {
            public void processWorkflow(final EditableWorkflow workflow) throws Exception {}
        }.getWorkflowType());
        assertEquals(FullRequestWorkflow.class, new BaseWorkflowCallbackHandler<FullRequestWorkflow>() {
            public void processWorkflow(final FullRequestWorkflow workflow) throws Exception {}
        }.getWorkflowType());
        assertEquals(FullReviewedActionsWorkflow.class, new BaseWorkflowCallbackHandler<FullReviewedActionsWorkflow>() {
            public void processWorkflow(final FullReviewedActionsWorkflow workflow) throws Exception {}
        }.getWorkflowType());
        assertEquals(DocumentWorkflow.class, new BaseWorkflowCallbackHandler<DocumentWorkflow>() {
            public void processWorkflow(final DocumentWorkflow workflow) throws Exception {}
        }.getWorkflowType());
    }
}
