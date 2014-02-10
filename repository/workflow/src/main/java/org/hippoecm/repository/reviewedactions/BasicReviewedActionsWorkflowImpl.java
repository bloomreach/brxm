/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated since CMS 7.9, use/configure {@link org.onehippo.repository.documentworkflow.DocumentWorkflowImpl} instead.
 */
@Deprecated
public class BasicReviewedActionsWorkflowImpl extends AbstractReviewedActionsWorkflow implements BasicReviewedActionsWorkflow {

    static final Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowImpl.class);

    protected PublishableDocument draftDocument;
    protected PublishableDocument unpublishedDocument;
    protected PublishableDocument publishedDocument;

    public BasicReviewedActionsWorkflowImpl() throws RemoteException {
    }

    @Override
    public void setNode(final Node node) throws RepositoryException {
        super.setNode(node);

        Node parent = node.getParent();

        draftDocument = unpublishedDocument = publishedDocument = null;
        for (Node sibling : new NodeIterable(parent.getNodes(node.getName()))) {
            String state = JcrUtils.getStringProperty(sibling, HippoStdNodeType.HIPPOSTD_STATE, "");
            switch (state) {
                case "draft":
                    draftDocument = new PublishableDocument(sibling);
                    break;
                case "unpublished":
                    unpublishedDocument = new PublishableDocument(sibling);
                    break;
                case "published":
                    publishedDocument = new PublishableDocument(sibling);
                    break;
            }
        }
    }

    @Override
    public Map<String, Serializable> hints() throws WorkflowException {
        Map<String, Serializable> info = new HashMap<>(super.hints());
        try {
            final String state = JcrUtils.getStringProperty(getNode(), HippoStdNodeType.HIPPOSTD_STATE, "");

            boolean status = Boolean.TRUE.equals(info.get("status"));
            boolean editable = Boolean.TRUE.equals(info.get("obtainEditableInstance"));
            boolean publishable = Boolean.TRUE.equals(info.get("publish"));
            boolean depublishable = Boolean.TRUE.equals(info.get("depublish"));
            Boolean deleteable = (Boolean) info.get("delete");

            // put everything on the unpublished; unless it doesn't exist
            if (unpublishedDocument != null && !PublishableDocument.UNPUBLISHED.equals(state)) {
                status = editable = publishable = depublishable = false;
                deleteable = null;
            } else if (unpublishedDocument == null) {
                if (PublishableDocument.DRAFT.equals(state)) {
                    if (publishedDocument != null) {
                        depublishable = status = false;
                        deleteable = null;
                    }
                } else if (PublishableDocument.PUBLISHED.equals(state)) {
                    if (draftDocument != null) {
                        editable = false;
                    }
                }
            }

            if (PublishableDocument.DRAFT.equals(state) && unpublishedDocument != null) {
                info.put("checkModified", true);
            }

            if (PublishableDocument.DRAFT.equals(state)) {
                info.put("inUseBy", draftDocument.getOwner());
            }
            info.put("obtainEditableInstance", editable);
            info.put("publish", publishable);
            info.put("depublish", depublishable);
            if (deleteable != null) {
                info.put("delete", deleteable);
            } else {
                info.remove("delete");
            }
            info.put("status", status);
        } catch (RepositoryException ex) {
            log.error("Failed to calculate hints", ex);
        }

        return info;
    }

    // EditableWorkflow implementation

    @Override
    public boolean isModified() throws WorkflowException, RepositoryException, RemoteException {
        return documentWorkflow.isModified();
    }

    @Override
    public Document obtainEditableInstance() throws RepositoryException, WorkflowException, RemoteException {
        return documentWorkflow.obtainEditableInstance();
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException, RepositoryException, RemoteException {
        return documentWorkflow.commitEditableInstance();
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException, RepositoryException, RemoteException {
        return documentWorkflow.disposeEditableInstance();
    }

    // BasicReviewedActionsWorkflow implementation

    @Override
    public void requestDeletion() throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestDeletion();
    }

    @Override
    public void requestDepublication() throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestDepublication();
    }

    @Override
    public void requestDepublication(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestDepublication(publicationDate);
    }

    @Override
    public void requestPublication() throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestPublication();
    }

    @Override
    public void requestPublication(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestPublication(publicationDate);
    }

    @Override
    public void requestPublication(final Date publicationDate, final Date unpublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestPublication(publicationDate, unpublicationDate);
    }

}
