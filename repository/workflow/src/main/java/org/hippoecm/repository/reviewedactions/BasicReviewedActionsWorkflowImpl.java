/*
 *  Copyright 2008-2010 Hippo.
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
import java.util.Date;
import java.util.Map;

import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PersistenceCapable(identityType=IdentityType.DATASTORE,cacheable="true",detachable="false",table="documents")
@DatastoreIdentity(strategy=IdGeneratorStrategy.NATIVE)
@Discriminator(strategy=DiscriminatorStrategy.CLASS_NAME)
public class BasicReviewedActionsWorkflowImpl extends WorkflowImpl implements BasicReviewedActionsWorkflow {

    private static final Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowImpl.class);

    private static final long serialVersionUID = 1L;

    @Persistent(column="hippostd:holder")
    protected String userIdentity;

    @Persistent(column="hippostd:state")
    protected String state;

    @Persistent(defaultFetchGroup="true",column="../{.}[hippostd:state='draft']")
    protected PublishableDocument draftDocument;

    @Persistent(defaultFetchGroup="true",column="../{.}[hippostd:state='unpublished']")
    protected PublishableDocument unpublishedDocument;

    @Persistent(defaultFetchGroup="true",column="../{.}[hippostd:state='published']")
    protected PublishableDocument publishedDocument;

    @Persistent(defaultFetchGroup="true",column="../hippo:request[hippostdpubwf:type!='rejected']")
    protected PublicationRequest current;

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> info = super.hints();
        boolean editable;
        boolean publishable = false;
        boolean depublishable = false;
        Boolean deleteable = false;
        boolean status = false;
        boolean pendingRequest;
        if (current != null) {
            pendingRequest = true;
        } else {
            pendingRequest = false;
        }
        if (PublishableDocument.DRAFT.equals(state)) {
            editable = draftDocument.getOwner() == null || draftDocument.getOwner().equals(super.getWorkflowContext().getUserIdentity());
            depublishable = false;
            publishable = false;
            status = true;
            deleteable = null;
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
            if (unpublishedDocument != null) {
                deleteable = null;
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
            info.put("inUseBy", draftDocument.getOwner());
        }
        info.put("obtainEditableInstance", editable);
        info.put("publish", publishable);
        info.put("depublish", depublishable);
        if (deleteable != null) {
            info.put("delete", deleteable);
        }
        info.put("status", status);
        return info;
    }

    public BasicReviewedActionsWorkflowImpl() throws RemoteException {
    }

    public Document obtainEditableInstance() throws WorkflowException {
        log.info("obtain editable instance on document ");
        if (draftDocument == null) {
            if (current != null) {
                throw new WorkflowException("unable to edit document with pending operation");
            }
            try {
                if (unpublishedDocument != null) {
                    draftDocument = (PublishableDocument) unpublishedDocument.clone();
                } else {
                    draftDocument = (PublishableDocument) publishedDocument.clone();
                }
                draftDocument.setState(PublishableDocument.DRAFT);
                draftDocument.setAvailability(new String[0]);
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
            if (draftDocument.getOwner() != null
                    && !getWorkflowContext().getUserIdentity().equals(draftDocument.getOwner()))
                throw new WorkflowException("document already being edited");
        }
        return draftDocument;
    }

    public Document commitEditableInstance() throws WorkflowException {
        log.info("commit editable instance of document ");
        if (draftDocument != null) {
            unpublishedDocument = null;
            draftDocument.setState(PublishableDocument.UNPUBLISHED);
            draftDocument.setAvailability(new String[] { "preview" });
            draftDocument.setModified(getWorkflowContext().getUserIdentity());
            if (publishedDocument != null) {
                publishedDocument.setAvailability(new String[] { "live" });
            }
            return draftDocument;
        } else {
            throw new WorkflowException("no draft version of publication");
        }
    }

    public Document disposeEditableInstance() throws WorkflowException {
        log.info("dispose editable instance on document ");
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
        log.info("deletion request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.DELETE, unpublishedDocument, getWorkflowContext()
                    .getUserIdentity());
        } else {
            throw new WorkflowException("request deletion failure");
        }
    }

    public void requestPublication() throws WorkflowException {
        log.info("publication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.PUBLISH, unpublishedDocument, getWorkflowContext()
                    .getUserIdentity());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestDepublication() throws WorkflowException {
        log.info("depublication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.DEPUBLISH, publishedDocument, getWorkflowContext()
                    .getUserIdentity());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate) throws WorkflowException {
        log.info("publication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.SCHEDPUBLISH, unpublishedDocument, getWorkflowContext()
                    .getUserIdentity(), publicationDate);
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate, Date depublicationDate) throws WorkflowException {
        log.info("publication request on document ");
        throw new WorkflowException("unsupported");
    }

    public void requestDepublication(Date depublicationDate) throws WorkflowException {
        log.info("depublication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.SCHEDDEPUBLISH, publishedDocument, getWorkflowContext()
                    .getUserIdentity(), depublicationDate);
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }
}
