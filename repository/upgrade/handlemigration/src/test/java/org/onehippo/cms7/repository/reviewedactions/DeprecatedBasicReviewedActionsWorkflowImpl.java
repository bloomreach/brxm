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
package org.onehippo.cms7.repository.reviewedactions;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.hippoecm.repository.reviewedactions.PublicationRequest;
import org.hippoecm.repository.reviewedactions.PublishableDocument;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeprecatedBasicReviewedActionsWorkflowImpl extends WorkflowImpl implements BasicReviewedActionsWorkflow {

    private static final Logger log = LoggerFactory.getLogger(DeprecatedBasicReviewedActionsWorkflowImpl.class);

    private static final long serialVersionUID = 1L;

    protected PublishableDocument draftDocument;

    protected PublishableDocument unpublishedDocument;

    protected PublishableDocument publishedDocument;

    protected PublicationRequest current;

    protected Node cloneDocumentNode(Document document) throws RepositoryException {
        Node srcNode = document.getNode();
        final Node parent = srcNode.getParent();
        JcrUtils.ensureIsCheckedOut(parent);
        return JcrUtils.copy(srcNode, srcNode.getName(), parent);
    }

    protected void deleteDocument(Document document) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(document.getNode());
        JcrUtils.ensureIsCheckedOut(document.getNode().getParent());
        document.getNode().remove();
    }

    public void setNode(Node node) throws RepositoryException {
        super.setNode(node);

        Node parent = node.getParent();

        draftDocument = unpublishedDocument = publishedDocument = null;
        for (Node sibling : new NodeIterable(parent.getNodes(node.getName()))) {
            String state = JcrUtils.getStringProperty(sibling, "hippostd:state","");
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
        current = null;
        String requestType = null;
        for (Node request : new NodeIterable(parent.getNodes("hippo:request"))) {
            requestType = JcrUtils.getStringProperty(request, "hippostdpubwf:type", "");
            if (!("rejected".equals(JcrUtils.getStringProperty(request, "hippostdpubwf:type", "")))) {
                current = new PublicationRequest(request);
            }
        }
    }

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> info = super.hints();
        boolean editable = false;
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
        try {
            String state = JcrUtils.getStringProperty(getNode(), "hippostd:state", "");

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
        }
        catch (RepositoryException ex) {
            // TODO DEJDO: ignore?
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

    public DeprecatedBasicReviewedActionsWorkflowImpl() throws RemoteException {
    }

    public Document obtainEditableInstance() throws WorkflowException {
        log.info("obtain editable instance on document ");
        try {
            if (draftDocument == null) {
                if (current != null) {
                    throw new WorkflowException("unable to edit document with pending operation");
                }
                Node draftNode = cloneDocumentNode(unpublishedDocument != null ? unpublishedDocument : publishedDocument);
                draftDocument = new PublishableDocument(draftNode);
                draftDocument.setState(PublishableDocument.DRAFT);
                draftDocument.setAvailability(new String[0]);
                draftDocument.setOwner(getWorkflowContext().getUserIdentity());
                if (unpublishedDocument != null) {
                    unpublishedDocument.setOwner(getWorkflowContext().getUserIdentity());
                }
                if (publishedDocument != null) {
                    publishedDocument.setOwner(getWorkflowContext().getUserIdentity());
                }
            } else {
                if (draftDocument.getOwner() != null
                        && !getWorkflowContext().getUserIdentity().equals(draftDocument.getOwner()))
                    throw new WorkflowException("document already being edited");
            }
        } catch (RepositoryException ex) {
            throw new WorkflowException("Failed to obtain an editable instance", ex);
        }
        return draftDocument;
    }

    public Document commitEditableInstance() throws WorkflowException {
        log.info("commit editable instance of document ");
        try {
            if (draftDocument != null) {
                if (unpublishedDocument != null) {
                    deleteDocument(unpublishedDocument);
                }
                unpublishedDocument = draftDocument;
                draftDocument = null;
                unpublishedDocument.setState(PublishableDocument.UNPUBLISHED);
                unpublishedDocument.setAvailability(new String[] { "preview" });
                unpublishedDocument.setModified(getWorkflowContext().getUserIdentity());
                if (publishedDocument != null) {
                    publishedDocument.setAvailability(new String[] { "live" });
                }
                return unpublishedDocument;
            } else {
                throw new WorkflowException("no draft version of publication");
            }
        }
        catch (RepositoryException ex) {
            throw new WorkflowException("failed to commit editable instance");
        }
    }

    public Document disposeEditableInstance() throws WorkflowException {
        log.info("dispose editable instance on document ");
        if (draftDocument != null) {
            try {
                deleteDocument(draftDocument);
                draftDocument = null;
            }
            catch (RepositoryException ex) {
                throw new WorkflowException("failed to dispose editable instance");
            }
        }
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
            try {
                current = new PublicationRequest(PublicationRequest.DELETE, getNode(), unpublishedDocument, getWorkflowContext()
                        .getUserIdentity());
            } catch (RepositoryException e) {
                throw new WorkflowException("request deletion failure");
            }
        } else {
            throw new WorkflowException("request deletion failure");
        }
    }

    public void requestPublication() throws WorkflowException {
        log.info("publication request on document ");
        if (current == null) {
            try {
                current = new PublicationRequest(PublicationRequest.PUBLISH, getNode(), unpublishedDocument, getWorkflowContext()
                        .getUserIdentity());
            } catch (RepositoryException e) {
                throw new WorkflowException("request publication failure", e);
            }
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestDepublication() throws WorkflowException {
        log.info("depublication request on document ");
        if (current == null) {
            try {
                current = new PublicationRequest(PublicationRequest.DEPUBLISH, getNode(), publishedDocument, getWorkflowContext()
                        .getUserIdentity());
            } catch (RepositoryException e) {
                throw new WorkflowException("request de-publication failure");
            }
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate) throws WorkflowException {
        log.info("publication request on document ");
        if (current == null) {
            try {
                current = new PublicationRequest(PublicationRequest.SCHEDPUBLISH, getNode(), unpublishedDocument, getWorkflowContext()
                        .getUserIdentity(), publicationDate);
            } catch (RepositoryException e) {
                throw new WorkflowException("request publication failure");
            }
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
            try {
                current = new PublicationRequest(PublicationRequest.SCHEDDEPUBLISH, getNode(), publishedDocument, getWorkflowContext()
                        .getUserIdentity(), depublicationDate);
            } catch (RepositoryException e) {
                throw new WorkflowException("request de-publication failure");
            }
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }
}
