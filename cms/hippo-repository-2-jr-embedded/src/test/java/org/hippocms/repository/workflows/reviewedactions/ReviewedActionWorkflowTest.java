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

import java.util.Date;
import junit.framework.TestCase;
import org.hippocms.repository.model.CurrentUsernameSource;
import org.hippocms.repository.model.Document;
import org.hippocms.repository.model.DocumentTemplate;

public class ReviewedActionWorkflowTest extends TestCase {
    public ReviewedActionWorkflowTest() {
        super();
    }

    public ReviewedActionWorkflowTest(String name) {
        super(name);
    }

    public void testCanChangeContentWithNoPendingRequests() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(new ReviewedActionsWorkflowFactory());
        Document doc = docTemplate.create("Lorem ipsum");

        try {
            doc.setContent("Quux qux baz bar foo.");
        } catch (IllegalStateException e) {
            fail("It must be allowed to set the content of documents when there are no pending requests");
        }
    }

    public void testCanRequestPublicationWithNoPendingRequests() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        try {
            ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
            workflow.requestPublication(null, null);
        } catch (IllegalStateException e) {
            fail("It must be allowed to request publication when there are no pending requests");
        }
    }

    public void testCannotRequestPublicationWithPendingRequests() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        try {
            ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
            workflow.requestPublication(null, null);
            workflow.requestPublication(null, null);
            fail("It must not be allowed to request publication when there are pending requests");
        } catch (IllegalStateException e) {
        }
    }

    public void testRequestedPublicationDatesAreCopiedToPublicationRequest() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        long now = System.currentTimeMillis();
        Date publicationDate = new Date(now + 1000);
        Date unpublicationDate = new Date(now + 2000);
        workflow.requestPublication(publicationDate, unpublicationDate);
        PublicationRequest publicationRequest = workflow.getPendingPublicationRequest();

        assertEquals("Publication date in publication request must match requested publication date", publicationDate,
                publicationRequest.getRequestedPublicationDate());
        assertEquals("Unpublication date in unpublication request must match requested unpublication date",
                unpublicationDate, publicationRequest.getRequestedUnpublicationDate());
    }

    public void testPublicationRequestHasCorrectRequestor() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        String publicationRequestorName = "Jane Doe";
        currentUsernameSource.setCurrentUsername(publicationRequestorName);
        workflow.requestPublication(null, null);
        PublicationRequest publicationRequest = workflow.getPendingPublicationRequest();

        assertEquals("Publication requestor of publication request must be correct", publicationRequestorName,
                publicationRequest.getRequestor());
    }

    public void testCanHaveMultipleDisapprovedPublicationRequests() {
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
        PublicationRequest firstPublicationRequest = workflow.getPendingPublicationRequest();
        firstPublicationRequest.disapprove("First disapproval.");

        workflow.requestPublication(null, null);
        PublicationRequest secondPublicationRequest = workflow.getPendingPublicationRequest();
        secondPublicationRequest.disapprove("Second disapproval.");

        assertEquals("Workflow must support multiple disapproved publication requests", 2, workflow
                .getNumberOfDisapprovedPublicationRequests());
    }
}
