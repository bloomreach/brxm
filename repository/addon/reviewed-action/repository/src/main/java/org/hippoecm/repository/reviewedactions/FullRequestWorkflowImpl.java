/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.reviewedactions;

import java.io.Serializable;
import java.rmi.RemoteException;

import java.util.Map;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;

public class FullRequestWorkflowImpl extends BasicRequestWorkflowImpl implements FullRequestWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected FullReviewedActionsWorkflowImpl publishedWorkflow;
    protected FullReviewedActionsWorkflowImpl unpublishedWorkflow;
    protected FullReviewedActionsWorkflowImpl draftWorkflow;

    @Override
    public Map<String,Serializable> hints()  {
        Map<String,Serializable> info = super.hints();
        if(PublicationRequest.REJECTED.equals(request.getType())) {
            info.put("acceptRequest", new Boolean(false));
            info.put("rejectRequest", new Boolean(false));
            info.put("cancelRequest", new Boolean(true));
        } else {
            info.put("acceptRequest", new Boolean(true));
            info.put("rejectRequest", new Boolean(true));
            info.put("cancelRequest", new Boolean(false));
        }
        return info;
    }

    public FullRequestWorkflowImpl() throws RemoteException {
    }

    public void acceptRequest() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        ReviewedActionsWorkflowImpl.log.info("accepting request for document ");
        String requestType = request.getType();
        if(PublicationRequest.DELETE.equals(requestType)) {
            if (publishedWorkflow != null) {
                throw new WorkflowException("cannot delete document when still published");
            }
            if (draftWorkflow != null) {
                throw new WorkflowException("cannot delete document which is being edited");
            }
            unpublishedWorkflow.setWorkflowContext(getWorkflowContext()); // FIXME; should use workflow chaining
            unpublishedWorkflow.doDelete();
            request = null;
        } else if(PublicationRequest.PUBLISH.equals(requestType)) {
            if (unpublishedWorkflow == null) {
                throw new WorkflowException("cannot publish document when no changes present");
            }
            unpublishedWorkflow.setWorkflowContext(getWorkflowContext()); // FIXME; should use workflow chaining
            unpublishedWorkflow.doPublish();
            request = null;
        } else if(PublicationRequest.DEPUBLISH.equals(requestType)) {
            if (publishedWorkflow == null) {
                throw new WorkflowException("cannot depublish document when not published");
            }
            publishedWorkflow.setWorkflowContext(getWorkflowContext()); // FIXME; should use workflow chaining
            publishedWorkflow.doDepublish();
            request = null;
        } else if(PublicationRequest.SCHEDPUBLISH.equals(requestType)) {
            if (unpublishedWorkflow == null) {
                throw new WorkflowException("cannot publish document when no changes present");
            }
            unpublishedWorkflow.setWorkflowContext(getWorkflowContext().getWorkflowContext(document)); // FIXME; should use workflow chaining
            unpublishedWorkflow.doSchedPublish(request.getScheduledDate());
            request = null;
        } else if(PublicationRequest.SCHEDDEPUBLISH.equals(requestType)) {
            if (publishedWorkflow == null) {
                throw new WorkflowException("cannot depublish document when not published");
            }
            publishedWorkflow.setWorkflowContext(getWorkflowContext().getWorkflowContext(document)); // FIXME; should use workflow chaining
            publishedWorkflow.doSchedDepublish(request.getScheduledDate());
            request = null;
        } else if(PublicationRequest.REJECTED.equals(requestType)) {
            throw new WorkflowException("request has already been rejected");
        } else {
            throw new MappingException("unknown publication request");
        }
    }

    public void rejectRequest(String reason) throws WorkflowException, MappingException, RepositoryException {
        ReviewedActionsWorkflowImpl.log.info("rejecting request for document ");
        // it normally should not occur that unpublishedWorkflow is null, but due to invalid
        // actions in console, e.g. we guard against it.
        if(unpublishedWorkflow != null) {
            PublishableDocument rejected = unpublishedWorkflow.getRejectedDocument();
            request.setRejected(rejected, reason);
        } else {
            request.setRejected(reason);
        }
    }
}
