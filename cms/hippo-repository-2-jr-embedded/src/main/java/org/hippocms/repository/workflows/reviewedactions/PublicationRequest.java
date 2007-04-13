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
import org.hippocms.repository.model.CurrentUsernameSource;

public class PublicationRequest {
    private static final int AWAITING_ACTION_STATE_ID = 0;
    private static final int DISAPPROVED_STATE_ID = 1;
    private static final int CANCELLED_STATE_ID = 2;
    private static final int REMOVED_STATE_ID = 3;

    private ReviewedActionsWorkflow workflow;
    private Date requestedPublicationDate;
    private Date requestedUnpublicationDate;
    private String requestor;
    private int state = AWAITING_ACTION_STATE_ID;
    private String disapprover;
    private String disapprovalReason;
    private CurrentUsernameSource currentUsernameSource;

    public PublicationRequest(ReviewedActionsWorkflow workflow, Date requestedPublicationDate,
            Date requestedUnpublicationDate) {
        super();

        this.workflow = workflow;
        this.requestedPublicationDate = requestedPublicationDate;
        this.requestedUnpublicationDate = requestedUnpublicationDate;
    }

    public Date getRequestedPublicationDate() {
        return requestedPublicationDate;
    }

    public Date getRequestedUnpublicationDate() {
        return requestedUnpublicationDate;
    }

    public String getRequestor() {
        return requestor;
    }

    public void setRequestor(String username) {
        requestor = username;
    }

    public void setCurrentUsernameSource(CurrentUsernameSource currentUsernameSource) {
        this.currentUsernameSource = currentUsernameSource;
    }

    public void cancel() {
        if (!isAwaitingAction()) {
            throw new IllegalStateException("Cannot cancel publication request that has already been processed");
        }
        workflow.clearPendingPublicationRequest();
        state = CANCELLED_STATE_ID;
    }

    public void disapprove(String reason) {
        if (!isAwaitingAction()) {
            throw new IllegalStateException("Cannot disapprove publication request that has already been processed");
        }
        workflow.clearPendingPublicationRequest();
        workflow.addDisapprovedPublicationRequest(this);
        this.disapprovalReason = reason;
        disapprover = currentUsernameSource.getCurrentUsername();
        state = DISAPPROVED_STATE_ID;
    }

    public String getDisapprovalReason() {
        return disapprovalReason;
    }

    public String getDisapprover() {
        return disapprover;
    }

    private boolean isAwaitingAction() {
        return state == AWAITING_ACTION_STATE_ID;
    }

    public void remove() {
        workflow.removeDisapprovedPublicationRequest(this);
        state = REMOVED_STATE_ID;
    }
}
