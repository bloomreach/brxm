/*
 * Copyright 2007 Hippo.
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
package org.hippocms.repository.workflows.reviewedactions;

import junit.framework.TestCase;
import org.hippocms.repository.model.CurrentUsernameSource;
import org.hippocms.repository.model.Document;
import org.hippocms.repository.model.DocumentTemplate;

public class DeletionRequestTest extends TestCase {
    public DeletionRequestTest() {
        super();
    }

    public DeletionRequestTest(String name) {
        super(name);
    }

    public void testDeletionCancellationClearsPendingRequest() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.requestDeletion();
        DeletionRequest deletionRequest = workflow.getPendingDeletionRequest();
        deletionRequest.cancel();

        assertNull("Cancelling deletion request must clear pending request", workflow.getPendingDeletionRequest());
    }

    public void testDeletionDisapprovalClearsPendingRequest() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.requestDeletion();
        DeletionRequest deletionRequest = workflow.getPendingDeletionRequest();
        deletionRequest.disapprove("Too important.");

        assertNull("Disapproving deletion request must clear pending request", workflow.getPendingDeletionRequest());
    }

    public void testDisapprovedDeletionRequestHasCorrectReason() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.requestDeletion();
        DeletionRequest deletionRequest = workflow.getPendingDeletionRequest();
        String disapprovalReason = "Too important.";
        deletionRequest.disapprove(disapprovalReason);

        assertEquals("Disapproved deletion request must have correct reason", disapprovalReason, deletionRequest
                .getDisapprovalReason());
    }

    public void testDisapprovedDeletionRequestHasCorrectDisapprover() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.requestDeletion();
        DeletionRequest deletionRequest = workflow.getPendingDeletionRequest();
        String disapproverName = "Jane Doe";
        currentUsernameSource.setCurrentUsername(disapproverName);
        deletionRequest.disapprove("Too important.");

        assertEquals("Disapproved deletion request must have correct disapprover", disapproverName, deletionRequest
                .getDisapprover());
    }

    public void testCannotCancelDisapprovedDeletionRequest() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.requestDeletion();
        DeletionRequest deletionRequest = workflow.getPendingDeletionRequest();
        deletionRequest.disapprove("Too important.");

        try {
            deletionRequest.cancel();
            fail("Cannot cancel a disapproved deletion request");
        } catch (IllegalStateException e) {
        }
    }
}
