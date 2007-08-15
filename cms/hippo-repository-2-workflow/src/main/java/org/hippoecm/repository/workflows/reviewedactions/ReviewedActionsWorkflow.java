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
package org.hippoecm.repository.workflows.reviewedactions;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.hippoecm.repository.model.CurrentUsernameSource;
import org.hippoecm.repository.model.Document;
import org.hippoecm.repository.model.Scheduler;
import org.hippoecm.repository.model.Workflow;

public class ReviewedActionsWorkflow implements Workflow {
    private Document document;
    private PublicationRequest pendingPublicationRequest;
    private DeletionRequest pendingDeletionRequest;
    private CurrentUsernameSource currentUsernameSource;
    private Scheduler scheduler;
    private Set disapprovedPublicationRequests = new HashSet();
    private Set disapprovedDeletionRequests = new HashSet();
    private String scheduledPublicationTaskId;
    private String scheduledUnpublicationTaskId;

    public ReviewedActionsWorkflow(Document document) {
        super();

        this.document = document;
    }

    public void requestPublication(Date publicationDate, Date unpublicationDate) {
        if (hasPendingRequest()) {
            throw new IllegalStateException("Cannot start a request when a request is pending");
        }
        pendingPublicationRequest = new PublicationRequest(this, publicationDate, unpublicationDate);
        pendingPublicationRequest.setRequestor(currentUsernameSource.getCurrentUsername());
        pendingPublicationRequest.setCurrentUsernameSource(currentUsernameSource);
    }

    public void publish(Date publicationDate, Date unpublicationDate) {
        pendingPublicationRequest = null;
        boolean isValidDateRange = publicationDate == null || unpublicationDate == null
                || (publicationDate.before(unpublicationDate));
        if (isValidDateRange) {
            long currentTime = System.currentTimeMillis();
            if ((publicationDate == null || currentTime >= publicationDate.getTime())
                    && (unpublicationDate == null || currentTime <= unpublicationDate.getTime())) {
                document.publish();
            } else if (publicationDate != null
                    && (unpublicationDate == null || publicationDate.before(unpublicationDate))) {
                scheduledPublicationTaskId = scheduler.schedule(publicationDate, new PublicationTask(document));
            }

            if (unpublicationDate != null && unpublicationDate.getTime() > currentTime) {
                scheduledUnpublicationTaskId = scheduler.schedule(unpublicationDate, new UnpublicationTask(document));
            }
        }
    }

    public PublicationRequest getPendingPublicationRequest() {
        return pendingPublicationRequest;
    }

    private boolean hasPendingRequest() {
        return pendingPublicationRequest != null || pendingDeletionRequest != null;
    }

    public void setCurrentUsernameSource(CurrentUsernameSource currentUsernameSource) {
        this.currentUsernameSource = currentUsernameSource;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void clearPendingPublicationRequest() {
        pendingPublicationRequest = null;
    }

    public int getNumberOfDisapprovedPublicationRequests() {
        return disapprovedPublicationRequests.size();
    }

    public Iterator disapprovedPublicationRequestsIterator() {
        return disapprovedPublicationRequests.iterator();
    }

    public void addDisapprovedPublicationRequest(PublicationRequest request) {
        disapprovedPublicationRequests.add(request);
    }

    public void removeDisapprovedPublicationRequest(PublicationRequest request) {
        disapprovedPublicationRequests.remove(request);
    }

    public void requestDeletion() {
        if (hasPendingRequest()) {
            throw new IllegalStateException("Cannot start a request when a request is pending");
        }
        pendingDeletionRequest = new DeletionRequest(this);
        pendingDeletionRequest.setRequestor(currentUsernameSource.getCurrentUsername());
        pendingDeletionRequest.setCurrentUsernameSource(currentUsernameSource);
    }

    public DeletionRequest getPendingDeletionRequest() {
        return pendingDeletionRequest;
    }

    public void addDisapprovedDeletionRequest(DeletionRequest request) {
        disapprovedDeletionRequests.add(request);
    }

    public void removeDisapprovedDeletionRequest(DeletionRequest request) {
        disapprovedDeletionRequests.remove(request);
    }

    public int getNumberOfDisapprovedDeletionRequests() {
        return disapprovedDeletionRequests.size();
    }

    public Iterator disapprovedDeletionRequestsIterator() {
        return disapprovedDeletionRequests.iterator();
    }

    public void clearPendingDeletionRequest() {
        pendingDeletionRequest = null;
    }

    public void delete() {
        clearPendingDeletionRequest();
        if (document.isPublished()) {
            document.unpublish();
        }
    }

    public void unpublish() {
        if (!document.isPublished()) {
            throw new IllegalStateException("Cannot unpublish a document that is not published");
        }
        if (scheduledPublicationTaskId == null && scheduledUnpublicationTaskId != null) {
            scheduler.cancel(scheduledUnpublicationTaskId);
        }
        document.unpublish();
    }

    public void cancelScheduledPublication() {
        scheduler.cancel(scheduledPublicationTaskId);
        if (scheduledUnpublicationTaskId != null) {
            cancelScheduledUnpublication();
        }
    }

    public void cancelScheduledUnpublication() {
        scheduler.cancel(scheduledUnpublicationTaskId);
    }
}
