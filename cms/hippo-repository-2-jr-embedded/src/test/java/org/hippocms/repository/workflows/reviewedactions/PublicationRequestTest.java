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

public class PublicationRequestTest extends TestCase {
    public PublicationRequestTest() {
        super();
    }

    public PublicationRequestTest(String name) {
        super(name);
    }

    public void testPublicationCancellationClearsPendingRequest() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.requestPublication(null, null);
        PublicationRequest publicationRequest = workflow.getPendingPublicationRequest();
        publicationRequest.cancel();

        assertNull("Cancelling publication request must clear pending request", workflow.getPendingPublicationRequest());
    }

    public void testPublicationDisapprovalClearsPendingRequest() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.requestPublication(null, null);
        PublicationRequest publicationRequest = workflow.getPendingPublicationRequest();
        publicationRequest.disapprove("Spelling errors.");

        assertNull("Disapproving publication request must clear pending request", workflow
                .getPendingPublicationRequest());
    }

    public void testDisapprovedPublicationRequestHasCorrectReason() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.requestPublication(null, null);
        PublicationRequest publicationRequest = workflow.getPendingPublicationRequest();
        String disapprovalReason = "Spelling errors.";
        publicationRequest.disapprove(disapprovalReason);

        assertEquals("Disapproved publication request must have correct reason", disapprovalReason, publicationRequest
                .getDisapprovalReason());
    }

    public void testDisapprovedPublicationRequestHasCorrectDisapprover() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.requestPublication(null, null);
        PublicationRequest publicationRequest = workflow.getPendingPublicationRequest();
        String disapproverName = "Jane Doe";
        currentUsernameSource.setCurrentUsername(disapproverName);
        publicationRequest.disapprove("Spelling errors.");

        assertEquals("Disapproved publication request must have correct disapprover", disapproverName,
                publicationRequest.getDisapprover());
    }

    public void testCannotCancelDisapprovedPublicationRequest() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.requestPublication(null, null);
        PublicationRequest publicationRequest = workflow.getPendingPublicationRequest();
        publicationRequest.disapprove("Spelling errors.");

        try {
            publicationRequest.cancel();
            fail("Cannot cancel a disapproved publication request");
        } catch (IllegalStateException e) {
        }
    }

    public void testCannotDisapproveCancelledPublicationRequest() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.requestPublication(null, null);
        PublicationRequest publicationRequest = workflow.getPendingPublicationRequest();
        publicationRequest.cancel();

        try {
            publicationRequest.disapprove("Spelling errors.");
            fail("Cannot disapprove a cancelled publication request");
        } catch (IllegalStateException e) {
        }
    }
}
