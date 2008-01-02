/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.reviewedactions;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.MappingException;

public class FullRequestWorkflowImpl extends BasicRequestWorkflowImpl implements FullRequestWorkflow {

    protected FullReviewedActionsWorkflowImpl workflow;

    public FullRequestWorkflowImpl() throws RemoteException {
    }

    public void acceptRequest() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        ReviewedActionsWorkflowImpl.log.info("accepting request for document ");
        if(PublicationRequest.DELETE.equals(request.type)) {
            workflow.delete();
        } else if(PublicationRequest.PUBLISH.equals(request.type)) {
            workflow.publish();
        } else if(PublicationRequest.DEPUBLISH.equals(request.type)) {
            workflow.depublish();
        } else if(PublicationRequest.REJECTED.equals(request.type)) {
            throw new WorkflowException("request has already been rejected");
        } else
            throw new MappingException("unknown publication request");
    }

    public void rejectRequest(String reason) throws WorkflowException, MappingException, RepositoryException {
        ReviewedActionsWorkflowImpl.log.info("rejecting request for document ");
        request.type = PublicationRequest.REJECTED;
        request.reason = reason;
        workflow.draft.state = PublishableDocument.STALE;
    }

}
