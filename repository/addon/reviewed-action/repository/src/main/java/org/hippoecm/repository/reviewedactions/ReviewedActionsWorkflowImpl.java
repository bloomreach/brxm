/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowMappingException;

public class ReviewedActionsWorkflowImpl extends WorkflowImpl implements ReviewedActionsWorkflow {
    public String a; // FIXME: workaround for current mapping issues
    public String content;
    String username;
    public PublicationRequest current;
    public PublishableDocument published;
    public PublishableDocument unpublished;
    public PublishableDocument draft;

    public ReviewedActionsWorkflowImpl() throws RemoteException {
    }
    public void obtainEditableInstance() throws WorkflowException {
        System.err.println("obtain editable instance on document "+a);
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
    }

    public void disposeEditableInstance() throws WorkflowException {
        System.err.println("dispose editable instance on document "+a);
        draft = null;
    }

    public void delete() throws WorkflowException {
        System.err.println("deletion on document "+a);
        if(current != null)
            throw new WorkflowException("cannot delete document with pending publication request");
        unpublished = draft = null;
    }

    public void requestDeletion() throws WorkflowException {
        System.err.println("deletion request on document "+a);
        if(current == null) {
            current = new PublicationRequest(PublicationRequest.DELETE, unpublished, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }            
    }

    public void publish() throws WorkflowException, WorkflowMappingException {
        System.err.println("publication on document "+a);
        try {
            if(draft != null) {
                published = (PublishableDocument) draft.clone();
            } else {
                published = (PublishableDocument) unpublished.clone();
            }
        } catch(CloneNotSupportedException ex) {
            throw new WorkflowException("document is not a publishable document");
        }
        published.state = PublishableDocument.PUBLISHED;
    }

    public void publish(Date publicationDate) throws WorkflowException {
        System.err.println("publication on document "+a);
        throw new WorkflowException("unsupported");
    }

    public void publish(Date publicationDate, Date depublicationDate) throws WorkflowException {
        System.err.println("publication on document "+a);
        throw new WorkflowException("unsupported");
    }

    public void requestPublication() throws WorkflowException {
        System.err.println("publication request on document "+a);
        if(current == null) {
            current = new PublicationRequest(PublicationRequest.PUBLISH, draft, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate) throws WorkflowException {
        System.err.println("publication request on document "+a);
        if(current == null) {
            current = new PublicationRequest(PublicationRequest.PUBLISH, draft, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate, Date depublicationDate) throws WorkflowException {
        System.err.println("publication request on document "+a);
        throw new WorkflowException("unsupported");
    }

    public void depublish() throws WorkflowException {
        System.err.println("depublication on document "+a);
        published = null;
    }

    public void requestDepublication() throws WorkflowException {
        System.err.println("depublication request on document "+a);
        if(current == null) {
            current = new PublicationRequest(PublicationRequest.DEPUBLISH, published, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestDepublication(Date publicationDate) throws WorkflowException {
        throw new WorkflowException("Unsupported operation");
    }

    public void post() throws RepositoryException {
        System.err.println("request performed on workflow "+a);
        super.post();
    }
}
