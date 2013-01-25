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
import javax.jcr.RepositoryException;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PersistenceCapable
public class FullRequestWorkflowImpl extends BasicRequestWorkflowImpl implements FullRequestWorkflow {

    private static final Logger log = LoggerFactory.getLogger(FullRequestWorkflowImpl.class);

    private static final long serialVersionUID = 1L;

    @Persistent(column="../{..}[hippostd:state='published']")
    protected PublishableDocument publishedDocument;

    @Persistent(column="../{..}[hippostd:state='unpublished']")
    protected PublishableDocument unpublishedDocument;

    @Persistent(column="../{..}[hippostd:state='draft']")
    protected PublishableDocument draftDocument;

    @Override
    public Map<String,Serializable> hints()  {
        Map<String,Serializable> info = super.hints();
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
        return info;
    }

    public FullRequestWorkflowImpl() throws RemoteException {
    }

    public void acceptRequest() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        log.info("accepting request for document ");
        String requestType = request.getType();
        if(PublicationRequest.DELETE.equals(requestType)) {
            if (publishedDocument != null) {
                throw new WorkflowException("cannot delete document when still published");
            }
            if (draftDocument != null) {
                throw new WorkflowException("cannot delete document which is being edited");
            }
            request = null;
            ((FullReviewedActionsWorkflow)getWorkflowContext().getWorkflow("default", unpublishedDocument)).delete();
        } else if(PublicationRequest.PUBLISH.equals(requestType)) {
            if (unpublishedDocument == null) {
                throw new WorkflowException("cannot publish document when no changes present");
            }
            request = null;
            ((FullReviewedActionsWorkflow)getWorkflowContext().getWorkflow("default", unpublishedDocument)).publish();
        } else if(PublicationRequest.DEPUBLISH.equals(requestType)) {
            if (publishedDocument == null) {
                throw new WorkflowException("cannot depublish document when not published");
            }
            request = null;
            ((FullReviewedActionsWorkflow)getWorkflowContext().getWorkflow("default", publishedDocument)).depublish();
        } else if(PublicationRequest.SCHEDPUBLISH.equals(requestType)) {
            if (unpublishedDocument == null) {
                throw new WorkflowException("cannot publish document when no changes present");
            }
            Workflow wf = getWorkflowContext().getWorkflow("default", request.getReference());
            ((FullReviewedActionsWorkflow)wf).publish(request.getScheduledDate());
            request = null;
        } else if(PublicationRequest.SCHEDDEPUBLISH.equals(requestType)) {
            if (publishedDocument == null) {
                throw new WorkflowException("cannot depublish document when not published");
            }
            Workflow wf = getWorkflowContext().getWorkflow("default", request.getReference());
            ((FullReviewedActionsWorkflow)wf).depublish(request.getScheduledDate());
            request = null;
        } else if(PublicationRequest.REJECTED.equals(requestType)) {
            throw new WorkflowException("request has already been rejected");
        } else {
            throw new MappingException("unknown publication request");
        }
    }

    public void rejectRequest(String reason) throws WorkflowException, MappingException, RepositoryException {
        log.info("rejecting request for document ");
        // it normally should not occur that unpublishedWorkflow is null, but due to invalid
        // actions in console, e.g. we guard against it.
        if(unpublishedDocument != null) {
            try {
                PublishableDocument rejected = (PublishableDocument) unpublishedDocument.clone();
                rejected.setState(PublishableDocument.STALE);
                request.setRejected(rejected, reason);
            } catch(CloneNotSupportedException ex) {
                request.setRejected(reason);
            }
        } else {
            request.setRejected(reason);
        }
    }
}
