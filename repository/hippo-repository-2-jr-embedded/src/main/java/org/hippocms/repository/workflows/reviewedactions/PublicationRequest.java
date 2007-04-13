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

public class PublicationRequest {
    private ReviewedActionsWorkflow workflow;
    private Date requestedPublicationDate;
    private Date requestedUnpublicationDate;
    private String requestor;
    private String disapprovalReason;

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

    public void cancel() {
        workflow.clearPendingPublicationRequest();
    }

    public void disapprove(String reason) {
        workflow.clearPendingPublicationRequest();
        this.disapprovalReason = reason;
    }

    public String getDisapprovalReason() {
        return disapprovalReason;
    }
}
