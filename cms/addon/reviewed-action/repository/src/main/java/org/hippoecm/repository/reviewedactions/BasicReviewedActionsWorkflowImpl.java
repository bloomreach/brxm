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

import java.util.Date;
import java.rmi.RemoteException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.ext.WorkflowImpl;

public class BasicReviewedActionsWorkflowImpl extends WorkflowImpl implements FullReviewedActionsWorkflow {

    protected String username;
    protected PublicationRequest current;
    protected PublishableDocument published;
    protected PublishableDocument unpublished;
    protected PublishableDocument draft;

    public BasicReviewedActionsWorkflowImpl() throws RemoteException {
    }

    public Document obtainEditableInstance() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("obtain editable instance on document ");
        if(draft == null) {
            try {
                draft = (PublishableDocument) unpublished.clone();
                draft.state = PublishableDocument.DRAFT;
            } catch(CloneNotSupportedException ex) {
                throw new WorkflowException("document is not a publishable document");
            }
        } else {
            if(!getWorkflowContext().getUsername().equals(username))
            throw new WorkflowException("document already being edited");
        }
        return draft;
    }

    public void commitEditableInstance() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("commit editable instance of document ");
        if(draft != null) {
            try {
                unpublished = (PublishableDocument) draft.clone();
                unpublished.state = PublishableDocument.UNPUBLISHED;
            } catch(CloneNotSupportedException ex) {
                throw new WorkflowException("document is not a publishable document");
            }
        } else {
            throw new WorkflowException("no draft version of publication");
        }
    }

    public void disposeEditableInstance() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("dispose editable instance on document ");
        draft = null;
    }

    public void delete() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("deletion on document ");
        if(current != null)
            throw new WorkflowException("cannot delete document with pending publication request");
        unpublished = draft = null;
    }

    public void requestDeletion() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("deletion request on document ");
        if(current == null) {
            current = new PublicationRequest(PublicationRequest.DELETE, unpublished, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void publish() throws WorkflowException, MappingException {
        ReviewedActionsWorkflowImpl.log.info("publication on document ");
        try {
            if(draft != null) {
                published = (PublishableDocument) draft.clone();
                published.state = PublishableDocument.PUBLISHED;
                unpublished = (PublishableDocument) draft.clone();
                unpublished.state = PublishableDocument.UNPUBLISHED;
            } else {
                published = (PublishableDocument) unpublished.clone();
                published.state = PublishableDocument.PUBLISHED;
            }
        } catch(CloneNotSupportedException ex) {
            throw new WorkflowException("document is not a publishable document");
        }
    }

    public void publish(Date publicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication on document ");
        throw new WorkflowException("unsupported");
    }

    public void publish(Date publicationDate, Date depublicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication on document ");
        throw new WorkflowException("unsupported");
    }

    public void requestPublication() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication request on document ");
        if(current == null) {
            current = new PublicationRequest(PublicationRequest.PUBLISH, draft, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication request on document ");
        if(current == null) {
            current = new PublicationRequest(PublicationRequest.PUBLISH, draft, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate, Date depublicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication request on document ");
        throw new WorkflowException("unsupported");
    }

    public void depublish() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("depublication on document ");
        published = null;
    }

    public void requestDepublication() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("depublication request on document ");
        if(current == null) {
            current = new PublicationRequest(PublicationRequest.DEPUBLISH, published, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestDepublication(Date publicationDate) throws WorkflowException {
        throw new WorkflowException("Unsupported operation");
    }

}
