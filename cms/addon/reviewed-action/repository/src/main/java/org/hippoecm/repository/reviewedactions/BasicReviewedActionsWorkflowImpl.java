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
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;

public class BasicReviewedActionsWorkflowImpl extends WorkflowImpl implements BasicReviewedActionsWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected String state;
    protected String userIdentity;
    protected PublicationRequest current;
    protected PublicationRequest current2;
    protected PublicationRequest current3;
    protected PublicationRequest current4;
    protected PublicationRequest current5;
    protected PublishableDocument publishedDocument;
    protected PublishableDocument unpublishedDocument;
    protected PublishableDocument draftDocument;

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> info = super.hints();
        boolean editable;
        boolean publishable = false;
        boolean depublishable = false;
        boolean deleteable = false;
        boolean locked = false;
        boolean status = false;
        boolean pendingRequest;
        if (current != null || current2 != null || current3 != null || current4 != null || current5 != null) {
            pendingRequest = true;
        } else {
            pendingRequest = false;
        }
        if (PublishableDocument.DRAFT.equals(state)) {
            locked = true;
            editable = draftDocument.username == null || draftDocument.username.equals(super.getWorkflowContext().getUserIdentity());
            depublishable = false;
            publishable = false;
            status = true;
        } else if (PublishableDocument.PUBLISHED.equals(state)) {
            if (draftDocument == null && unpublishedDocument == null) {
                status = true;
            }
            if (draftDocument != null || unpublishedDocument != null) {
                editable = false;
            } else if (pendingRequest) {
                editable = false;
            } else {
                editable = true;
            }
            if (draftDocument == null && !pendingRequest) {
                depublishable = true;
            }
        } else if (PublishableDocument.UNPUBLISHED.equals(state)) {
            if (draftDocument == null) {
                status = true;
            }
            if (draftDocument != null) {
                editable = false;
            } else if (pendingRequest) {
                editable = false;
            } else {
                editable = true;
            }
            if (draftDocument == null && !pendingRequest) {
                publishable = true;
            }
            if (draftDocument == null && publishedDocument == null && !pendingRequest) {
                deleteable = true;
            }
        } else {
            editable = false;
        }
        if (!editable && PublishableDocument.DRAFT.equals(state)) {
            info.put("inUseBy", draftDocument.username);
        }
        info.put("obtainEditableInstance", editable);
        info.put("publish", publishable);
        info.put("depublish", depublishable);
        info.put("delete", deleteable);
        info.put("status", status);
        return info;
    }

    public BasicReviewedActionsWorkflowImpl() throws RemoteException {
    }

    public Document obtainEditableInstance() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("obtain editable instance on document ");
        if (draftDocument == null) {
            if (current != null || current2 != null || current3 != null || current4 != null || current5 != null) {
                throw new WorkflowException("unable to edit document with pending operation");
            }
            try {
                if (unpublishedDocument != null) {
                    draftDocument = (PublishableDocument) unpublishedDocument.clone();
                } else {
                    draftDocument = (PublishableDocument) publishedDocument.clone();
                }
                draftDocument.state = PublishableDocument.DRAFT;
                draftDocument.setOwner(getWorkflowContext().getUserIdentity());
                if (unpublishedDocument != null) {
                    unpublishedDocument.setOwner(getWorkflowContext().getUserIdentity());
                }
                if (publishedDocument != null) {
                    publishedDocument.setOwner(getWorkflowContext().getUserIdentity());
                }
                userIdentity = getWorkflowContext().getUserIdentity();
            } catch (CloneNotSupportedException ex) {
                throw new WorkflowException("document is not a publishable document");
            }
        } else {
            if (draftDocument.username != null
                    && !getWorkflowContext().getUserIdentity().equals(draftDocument.username))
                throw new WorkflowException("document already being edited");
        }
        return draftDocument;
    }

    public Document commitEditableInstance() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("commit editable instance of document ");
        if (draftDocument != null) {
            unpublishedDocument = null;
            draftDocument.setState(PublishableDocument.UNPUBLISHED);
            draftDocument.setModified(getWorkflowContext().getUserIdentity());
            return draftDocument;
        } else {
            throw new WorkflowException("no draft version of publication");
        }
    }

    public Document disposeEditableInstance() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("dispose editable instance on document ");
        draftDocument = null;
        if (unpublishedDocument != null) {
            return unpublishedDocument;
        } else if (publishedDocument != null) {
            return publishedDocument;
        } else {
            return null;
        }
    }

    public void requestDeletion() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("deletion request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.DELETE, unpublishedDocument, getWorkflowContext()
                    .getUserIdentity());
        } else {
            throw new WorkflowException("request deletion failure");
        }
    }

    public void requestPublication() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.PUBLISH, unpublishedDocument, getWorkflowContext()
                    .getUserIdentity());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestDepublication() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("depublication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.DEPUBLISH, publishedDocument, getWorkflowContext()
                    .getUserIdentity());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void publish(Date publicationDate) throws WorkflowException, MappingException, RepositoryException,
            RemoteException {
        ReviewedActionsWorkflowImpl.log.info("publication on document ");
        throw new WorkflowException("unsupported");
    }

    public void publish(Date publicationDate, Date depublicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication on document ");
        throw new WorkflowException("unsupported");
    }

    public void requestPublication(Date publicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.SCHEDPUBLISH, unpublishedDocument, getWorkflowContext()
                    .getUserIdentity(), publicationDate);
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate, Date depublicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication request on document ");
        throw new WorkflowException("unsupported");
    }

    public void requestDepublication(Date depublicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("depublication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.SCHEDDEPUBLISH, publishedDocument, getWorkflowContext()
                    .getUserIdentity(), depublicationDate);
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    PublishableDocument getRejectedDocument() throws WorkflowException {
        try {
            PublishableDocument rejected = (PublishableDocument) unpublishedDocument.clone();
            rejected.setState(PublishableDocument.STALE);
            return rejected;
        } catch (CloneNotSupportedException ex) {
            throw new WorkflowException("document is not a publishable document");
        }
    }

}
