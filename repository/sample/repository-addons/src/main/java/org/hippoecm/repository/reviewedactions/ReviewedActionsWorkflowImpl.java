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
    PublicationRequest request;
    PublishableDocument published;
    PublishableDocument current;
    PublishableDocument editing;

    public ReviewedActionsWorkflowImpl() throws RemoteException {
    }
    public void obtainEditableInstance() throws WorkflowException {
        if(editing == null) {
            try {
                editing = (PublishableDocument) current.clone();
            } catch(CloneNotSupportedException ex) {
                throw new WorkflowException("document is not a publishable document");
            }
        } else {
            throw new WorkflowException("document already being edited");
        }
    }

    public void disposeEditableInstance() throws WorkflowException {
        editing = null;
    }

    public void delete() throws WorkflowException {
        if(request != null)
            throw new WorkflowException("cannot delete document with pending publication request");
        current = editing = null;
    }

    public void requestDeletion() throws WorkflowException {
        if(request == null) {
            request = new PublicationRequest(request.DELETE, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }            
    }

    public void publish() throws WorkflowException, WorkflowMappingException {
        try {
            if(editing != null) {
                published = (PublishableDocument) editing.clone();
            } else {
                published = (PublishableDocument) current.clone();
            }
        } catch(CloneNotSupportedException ex) {
            throw new WorkflowException("document is not a publishable document");
        }
        published.state = published.PUBLISHED;
    }

    public void publish(Date publicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    public void publish(Date publicationDate, Date depublicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    public void requestPublication() throws WorkflowException {
        if(request == null) {
            request = new PublicationRequest(request.PUBLISH, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate) throws WorkflowException {
        if(request == null) {
            request = new PublicationRequest(request.PUBLISH, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate, Date depublicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    public void depublish() throws WorkflowException {
        published = null;
    }

    public void requestDepublication() throws WorkflowException {
        if(request == null) {
            request = new PublicationRequest(request.DEPUBLISH, getWorkflowContext().getUsername());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestDepublication(Date publicationDate) throws WorkflowException {
        throw new WorkflowException("Unsupported operation");
    }
}
