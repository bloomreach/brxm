/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullRequestWorkflowImpl extends BasicRequestWorkflowImpl implements FullRequestWorkflow {

    private static final Logger log = LoggerFactory.getLogger(FullRequestWorkflowImpl.class);

    private static final long serialVersionUID = 1L;

    protected PublishableDocument publishedDocument;

    protected PublishableDocument unpublishedDocument;

    protected PublishableDocument draftDocument;

    public void setNode(Node node) throws RepositoryException {
        super.setNode(node);

        Node parent = node.getParent();

        draftDocument = unpublishedDocument = publishedDocument = null;
        for (Node sibling : new NodeIterable(parent.getNodes(parent.getName()))) {
            String state = JcrUtils.getStringProperty(sibling, "hippostd:state", "");
            if ("draft".equals(state)) {
                draftDocument = new PublishableDocument(sibling);
            }
            else if ("unpublished".equals(state)) {
                unpublishedDocument = new PublishableDocument(sibling);
            }
            else if ("published".equals(state)) {
                publishedDocument = new PublishableDocument(sibling);
            }
        }
    }

    @Override
    public Map<String,Serializable> hints()  {
        Map<String,Serializable> info = super.hints();
        try {
            if (PublicationRequest.REJECTED.equals(request.getType())) {
                info.put("acceptRequest", false);
                info.put("rejectRequest", false);
                info.put("cancelRequest", true);
            } else if (PublicationRequest.COLLECTION.equals(request.getType())) {
                info.put("acceptRequest", false);
                info.put("rejectRequest", false);
                info.put("cancelRequest", false);
            } else {
                info.put("acceptRequest", true);
                if(request.getOwner() != null) {
                    if(request.getOwner().equals(getWorkflowContext().getUserIdentity())) {
                        info.put("rejectRequest", false);
                        info.put("cancelRequest", true);
                    } else {
                        info.put("rejectRequest", true);
                        info.put("cancelRequest", false);
                    }
                } else {
                    info.put("rejectRequest", true);
                    info.put("cancelRequest", false);
                }
            }
        }
        catch (RepositoryException ex) {
            // TODO DEDJO: ignore?
        }
        return info;
    }

    public FullRequestWorkflowImpl() throws RemoteException {
    }

    public void acceptRequest() throws WorkflowException, RepositoryException, RemoteException {
        log.info("accepting request for document ");
        String requestType = request.getType();
        if(PublicationRequest.DELETE.equals(requestType)) {
            if (publishedDocument != null) {
                throw new WorkflowException("cannot delete document when still published");
            }
            if (draftDocument != null) {
                throw new WorkflowException("cannot delete document which is being edited");
            }
            deleteRequest();
            ((FullReviewedActionsWorkflow)getWorkflowContext().getWorkflow("default", unpublishedDocument)).delete();
        } else if(PublicationRequest.PUBLISH.equals(requestType)) {
            if (unpublishedDocument == null) {
                throw new WorkflowException("cannot publish document when no changes present");
            }
            deleteRequest();
            ((FullReviewedActionsWorkflow)getWorkflowContext().getWorkflow("default", unpublishedDocument)).publish();
        } else if(PublicationRequest.DEPUBLISH.equals(requestType)) {
            if (publishedDocument == null) {
                throw new WorkflowException("cannot depublish document when not published");
            }
            deleteRequest();
            ((FullReviewedActionsWorkflow)getWorkflowContext().getWorkflow("default", publishedDocument)).depublish();
        } else if(PublicationRequest.SCHEDPUBLISH.equals(requestType)) {
            if (unpublishedDocument == null) {
                throw new WorkflowException("cannot publish document when no changes present");
            }
            Workflow wf = getWorkflowContext().getWorkflow("default", request.getReference());
            ((FullReviewedActionsWorkflow)wf).publish(request.getScheduledDate());
            deleteRequest();
        } else if(PublicationRequest.SCHEDDEPUBLISH.equals(requestType)) {
            if (publishedDocument == null) {
                throw new WorkflowException("cannot depublish document when not published");
            }
            Workflow wf = getWorkflowContext().getWorkflow("default", request.getReference());
            ((FullReviewedActionsWorkflow)wf).depublish(request.getScheduledDate());
            deleteRequest();
        } else if(PublicationRequest.REJECTED.equals(requestType)) {
            throw new WorkflowException("request has already been rejected");
        } else {
            throw new MappingException("unknown publication request");
        }
    }

    protected void deleteRequest() throws WorkflowException, RepositoryException {
        if (request != null) {
            JcrUtils.ensureIsCheckedOut(getNode().getParent(),false);
            getCheckedOutNode().remove();
            request = null;
        }
    }

    public void rejectRequest(String reason) throws WorkflowException, RepositoryException {
        log.info("rejecting request for document ");
        request.setRejected(reason);
    }
}
