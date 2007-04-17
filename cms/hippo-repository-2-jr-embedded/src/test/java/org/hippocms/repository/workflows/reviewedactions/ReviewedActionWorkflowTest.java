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
import org.easymock.MockControl;
import org.hippocms.repository.model.CurrentUsernameSource;
import org.hippocms.repository.model.Document;
import org.hippocms.repository.model.DocumentTemplate;
import org.hippocms.repository.model.PublicationServiceProvider;

public class ReviewedActionWorkflowTest extends TestCase {
    private static final long SECONDS_IN_A_DAY = 86400000;

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

    public void testPublicationClearsPendingPublicationRequest() {
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
        workflow.publish(null, null);

        assertNull("Publication must clear pending publication request", workflow.getPendingPublicationRequest());
    }

    public void testCanRequestDeletionWithNoPendingRequests() {
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
            workflow.requestDeletion();
        } catch (IllegalStateException e) {
            fail("It must be allowed to request deletion when there are no pending requests");
        }
    }

    public void testCannotRequestDeletionWithPendingRequests() {
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
            workflow.requestDeletion();
            workflow.requestDeletion();
            fail("It must not be allowed to request deletion when there are pending requests");
        } catch (IllegalStateException e) {
        }
    }

    public void testCanHaveMultipleDisapprovedDeletionRequests() {
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
        DeletionRequest firstDeletionRequest = workflow.getPendingDeletionRequest();
        firstDeletionRequest.disapprove("First disapproval.");

        workflow.requestDeletion();
        DeletionRequest secondDeletionRequest = workflow.getPendingDeletionRequest();
        secondDeletionRequest.disapprove("Second disapproval.");

        assertEquals("Workflow must support multiple disapproved deletion requests", 2, workflow
                .getNumberOfDisapprovedDeletionRequests());
    }

    public void testDeletionRequestHasCorrectRequestor() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        Document doc = docTemplate.create("Lorem ipsum");

        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        String deletionRequestorName = "Jane Doe";
        currentUsernameSource.setCurrentUsername(deletionRequestorName);
        workflow.requestDeletion();
        DeletionRequest deletionRequest = workflow.getPendingDeletionRequest();

        assertEquals("Deletion requestor of deletion request must be correct", deletionRequestorName, deletionRequest
                .getRequestor());
    }

    public void testDeletionClearsPendingDeletionRequest() {
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
        workflow.delete();

        assertNull("Deletion must clear pending deletion request", workflow.getPendingDeletionRequest());
    }

    public void testPublicationDoesNotClearPendingDeletionRequest() {
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
        workflow.publish(null, null);

        assertNotNull("Publication must not clear pending deletion request", workflow.getPendingDeletionRequest());
    }

    public void testPublicationSendsContentToPublicationSps() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        MockControl spMockControl = MockControl.createControl(PublicationServiceProvider.class);
        PublicationServiceProvider mockSp = (PublicationServiceProvider) spMockControl.getMock();
        String name = "Lorem ipsum";
        String content = "Foo bar baz qux quux.";
        mockSp.publish(name, content);
        spMockControl.replay();
        docTemplate.addPublicationServiceProvider(mockSp);

        Document doc = docTemplate.create(name);
        doc.setContent(content);
        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.publish(null, null);

        spMockControl.verify();
    }

    public void testDeletionDoesNotRemoveUnpublishedDocumentFromPublicationSps() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        MockControl spMockControl = MockControl.createControl(PublicationServiceProvider.class);
        PublicationServiceProvider mockSp = (PublicationServiceProvider) spMockControl.getMock();
        spMockControl.replay();
        docTemplate.addPublicationServiceProvider(mockSp);

        Document doc = docTemplate.create("Lorem ipsum");
        doc.setContent("Foo bar baz qux quux.");
        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.delete();

        spMockControl.verify();
    }

    public void testDeletionRemovesPublishedDocumentFromPublicationSps() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        MockControl spMockControl = MockControl.createControl(PublicationServiceProvider.class);
        PublicationServiceProvider mockSp = (PublicationServiceProvider) spMockControl.getMock();
        String name = "Lorem ipsum";
        String content = "Foo bar baz qux quux.";
        mockSp.publish(name, content);
        mockSp.remove(name);
        spMockControl.replay();
        docTemplate.addPublicationServiceProvider(mockSp);

        Document doc = docTemplate.create(name);
        doc.setContent(content);
        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.publish(null, null);
        workflow.delete();

        spMockControl.verify();
    }

    public void testUnpublicationRemovesDocumentFromPublicationSps() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        MockControl spMockControl = MockControl.createControl(PublicationServiceProvider.class);
        PublicationServiceProvider mockSp = (PublicationServiceProvider) spMockControl.getMock();
        String name = "Lorem ipsum";
        String content = "Foo bar baz qux quux.";
        mockSp.publish(name, content);
        mockSp.remove(name);
        spMockControl.replay();
        docTemplate.addPublicationServiceProvider(mockSp);

        Document doc = docTemplate.create(name);
        doc.setContent(content);
        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.publish(null, null);
        workflow.unpublish();

        spMockControl.verify();
    }

    public void testCannotUnpublishAnUnpublishedDocument() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        MockControl spMockControl = MockControl.createControl(PublicationServiceProvider.class);
        PublicationServiceProvider mockSp = (PublicationServiceProvider) spMockControl.getMock();
        String name = "Lorem ipsum";
        String content = "Foo bar baz qux quux.";
        mockSp.publish(name, content);
        mockSp.remove(name);
        spMockControl.replay();
        docTemplate.addPublicationServiceProvider(mockSp);

        Document doc = docTemplate.create(name);
        doc.setContent(content);
        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        workflow.publish(null, null);
        workflow.unpublish();
        try {
            workflow.unpublish();
            fail("Cannot unpublish a document that has not been published");
        } catch (IllegalStateException e) {
        }
    }

    public void testScheduledPublicationDoesNotSendDocumentToPublicationSpsImmediatly() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        MockControl spMockControl = MockControl.createControl(PublicationServiceProvider.class);
        PublicationServiceProvider mockSp = (PublicationServiceProvider) spMockControl.getMock();
        spMockControl.replay();
        docTemplate.addPublicationServiceProvider(mockSp);

        Document doc = docTemplate.create("Lorem ipsum");
        doc.setContent("Foo bar baz qux quux.");
        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        Date publicationDate = new Date(System.currentTimeMillis() + SECONDS_IN_A_DAY);
        workflow.publish(publicationDate, null);

        spMockControl.verify();
    }

    public void testScheduledPublicationSendsDocumentToPublicationSpsImmediatlyIfPublicationDateInThePast() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        MockControl spMockControl = MockControl.createControl(PublicationServiceProvider.class);
        PublicationServiceProvider mockSp = (PublicationServiceProvider) spMockControl.getMock();
        String name = "Lorem ipsum";
        String content = "Foo bar baz qux quux.";
        mockSp.publish(name, content);
        spMockControl.replay();
        docTemplate.addPublicationServiceProvider(mockSp);

        Document doc = docTemplate.create(name);
        doc.setContent(content);
        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        Date publicationDate = new Date(System.currentTimeMillis() - SECONDS_IN_A_DAY);
        workflow.publish(publicationDate, null);

        spMockControl.verify();
    }

    public void testScheduledPublicationDoesNotSendDocumentToPublicationSpsIfUnpublicationBeforePublication() {
        DocumentTemplate docTemplate = new DocumentTemplate();

        CurrentUsernameSource currentUsernameSource = new CurrentUsernameSource();
        currentUsernameSource.setCurrentUsername("John Doe");
        docTemplate.setCurrentUsernameSource(currentUsernameSource);
        ReviewedActionsWorkflowFactory workflowFactory = new ReviewedActionsWorkflowFactory();
        workflowFactory.setCurrentUsernameSource(currentUsernameSource);
        docTemplate.setWorkflowFactory(workflowFactory);
        MockControl spMockControl = MockControl.createControl(PublicationServiceProvider.class);
        PublicationServiceProvider mockSp = (PublicationServiceProvider) spMockControl.getMock();
        spMockControl.replay();
        docTemplate.addPublicationServiceProvider(mockSp);

        Document doc = docTemplate.create("Lorem ipsum");
        doc.setContent("Foo bar baz qux quux.");
        ReviewedActionsWorkflow workflow = (ReviewedActionsWorkflow) doc.getWorkflow();
        Date publicationDate = new Date(System.currentTimeMillis());
        Date unpublicationDate = new Date(System.currentTimeMillis() - SECONDS_IN_A_DAY);
        workflow.publish(publicationDate, unpublicationDate);

        spMockControl.verify();
    }
}
