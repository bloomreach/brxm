/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.repository.documentworkflow.integration;

import javax.jcr.Node;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NAME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DocumentWorkflowNonMutatingIT extends AbstractDocumentWorkflowIntegrationTest {


    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void workflow_methods_annotated_with_mutates_false_do_not_save_or_refresh_workflow_session() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.branch("foo", "Foo");

        final HippoSession workflowSession = (HippoSession)workflow.getWorkflowContext().getInternalWorkflowSession();

        final Node unpublishedByWFSession = workflowSession.getNodeByIdentifier(getVariant(UNPUBLISHED).getIdentifier());

        unpublishedByWFSession.setProperty(HIPPO_NAME, "foo");

        assertTrue(workflowSession.pendingChanges(unpublishedByWFSession, null).hasNext());

        workflow.hints();

        assertTrue(workflowSession.pendingChanges(unpublishedByWFSession, null).hasNext());

        workflow.getWorkflowContext();

        assertTrue(workflowSession.pendingChanges(unpublishedByWFSession, null).hasNext());

        workflow.getNode();

        assertTrue(workflowSession.pendingChanges(unpublishedByWFSession, null).hasNext());

        workflow.listBranches();

        assertTrue(workflowSession.pendingChanges(unpublishedByWFSession, null).hasNext());

        workflow.getBranch("foo", WorkflowUtils.Variant.UNPUBLISHED);
        workflow.getBranch("master", WorkflowUtils.Variant.UNPUBLISHED);

        assertTrue(workflowSession.pendingChanges(unpublishedByWFSession, null).hasNext());

        workflow.listVersions();

        assertTrue(workflowSession.pendingChanges(unpublishedByWFSession, null).hasNext());

        workflow.obtainEditableInstance();

        assertFalse(workflowSession.pendingChanges(unpublishedByWFSession, null).hasNext());
    }

}

