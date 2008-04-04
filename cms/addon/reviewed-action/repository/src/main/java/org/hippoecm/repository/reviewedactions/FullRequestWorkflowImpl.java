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

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;

public class FullRequestWorkflowImpl extends WorkflowImpl implements FullRequestWorkflow {
    private static final long serialVersionUID = 1L;
    
    protected PublicationRequest request;
    protected PublishableDocument document;

    protected FullReviewedActionsWorkflowImpl workflow;

    protected FullReviewedActionsWorkflowImpl workflow2;

    public FullRequestWorkflowImpl() throws RemoteException {
    }

    public void acceptRequest() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        ReviewedActionsWorkflowImpl.log.info("accepting request for document ");
        if(PublicationRequest.DELETE.equals(request.type)) {
            if(workflow != null)
                workflow.delete();
            else
                workflow2.delete();
            workflow.current = null;
            workflow2.current = null;
            request.document = null;
            request = null;
        } else if(PublicationRequest.PUBLISH.equals(request.type)) {
            if(workflow != null)
                workflow.publish();
            else
                workflow2.publish();
            workflow.current = null;
            workflow2.current = null;
            request.document = null;
            request = null;
        } else if(PublicationRequest.DEPUBLISH.equals(request.type)) {
            if(workflow != null)
                workflow.depublish();
            else
                workflow2.depublish();
            workflow.current = null;
            workflow2.current = null;
            request.document = null;
            request = null;
        } else if(PublicationRequest.REJECTED.equals(request.type)) {
            throw new WorkflowException("request has already been rejected");
        } else
            throw new MappingException("unknown publication request");
    }

    public void cancelRequest() throws WorkflowException, MappingException, RepositoryException {
        request = null;
    }

    public void rejectRequest(String reason) throws WorkflowException, MappingException, RepositoryException {
        ReviewedActionsWorkflowImpl.log.info("rejecting request for document ");
        request.type = PublicationRequest.REJECTED;
        request.reason = reason;
        workflow.draft.state = PublishableDocument.STALE;
    }

}
