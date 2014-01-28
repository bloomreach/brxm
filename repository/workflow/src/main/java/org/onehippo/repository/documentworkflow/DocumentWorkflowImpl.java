/*
 *  Copyright 2013-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.documentworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.onehippo.repository.util.JcrConstants;

public class DocumentWorkflowImpl extends WorkflowImpl implements DocumentWorkflow {

    private static final long serialVersionUID = 1L;

    private HandleDocumentWorkflow handleDocumentWorkflow;
    private Node subject;

    public DocumentWorkflowImpl() throws RemoteException {
    }

    protected Document toUserDocument(Document document) throws RepositoryException {
        return new Document(getWorkflowContext().getUserSession().getNodeByIdentifier(document.getIdentity()));
    }

    protected Document workflowResultToUserDocument(Object obj) throws RepositoryException {
        Document document = null;
        if (obj != null) {
            if (obj instanceof DocumentVariant) {
                document = (DocumentVariant)obj;
            }
            if (obj instanceof Document) {
                document = (Document)obj;
            }
        }
        return document != null && document.getIdentity() != null ? toUserDocument(document) : null;
    }

    // Workflow implementation / WorkflowImpl override

    @Override
    public void setNode(final Node node) throws RepositoryException {
        super.setNode(node);

        this.subject = node;

        Node parent = node.getParent();
        if (!parent.isNodeType(HippoNodeType.NT_HANDLE) && !parent.isNodeType(JcrConstants.NT_VERSION)) {
            throw new RepositoryException("Invalid workflow subject " + node.getPath() + ", does not have a handle or version as it's parent");
        }

        try {
            final Workflow handleWorkflow = getNonChainingWorkflowContext().getWorkflow("default", new Document(parent));
            if (!(handleWorkflow instanceof HandleDocumentWorkflow)) {
                throw new RepositoryException("Workflow on handle, in category 'document', is not a HandleDocumentWorkflow");
            }

            handleDocumentWorkflow = (HandleDocumentWorkflow) handleWorkflow;
        }
        catch (WorkflowException wfe) {
            if (wfe.getCause() != null && wfe.getCause() instanceof RepositoryException) {
                throw (RepositoryException)wfe.getCause();
            }
            throw new RepositoryException(wfe);
        }
    }

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> hints = super.hints();
        hints.putAll(handleDocumentWorkflow.getInfo());
        hints.putAll(handleDocumentWorkflow.getActions());
        return hints;
    }

    // EditableWorkflow implementation

    @Override
    public boolean isModified() throws WorkflowException, RepositoryException {
        return (Boolean) handleDocumentWorkflow.triggerAction("checkModified");
    }

    @Override
    public Document obtainEditableInstance() throws RepositoryException, WorkflowException {
        return workflowResultToUserDocument(handleDocumentWorkflow.triggerAction("obtainEditableInstance"));
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(handleDocumentWorkflow.triggerAction("commitEditableInstance"));
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(handleDocumentWorkflow.triggerAction("disposeEditableInstance"));
    }

    // BasicReviewedActionsWorkflow implementation

    @Override
    public void requestDeletion() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("requestDelete");
    }

    @Override
    public void requestDepublication() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("requestDepublish");
    }

    @Override
    public void requestDepublication(final Date publicationDate) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("requestDepublish", publicationDate);
    }

    @Override
    public void requestPublication() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("publish");
    }

    @Override
    public void requestPublication(final Date publicationDate) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("requestPublish", publicationDate);
    }

    @Override
    public void requestPublication(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    // FullReviewedActionsWorkflow implementation

    @Override
    public void delete() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("delete");
    }

    @Override
    public void rename(final String newName) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("rename", newName);
    }

    @Override
    public void copy(final Document destination, final String newName) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("copy", new DocumentCopyMovePayload(destination, newName));
    }

    @Override
    public void move(final Document destination, final String newName) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("move", new DocumentCopyMovePayload(destination, newName));
    }

    @Override
    public void depublish() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("depublish");
    }

    @Override
    public void depublish(final Date depublicationDate) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("depublish", depublicationDate);
    }

    @Override
    public void publish() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("publish");
    }

    @Override
    public void publish(final Date publicationDate) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("publish", publicationDate);
    }

    @Override
    public void publish(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    // BasicRequestWorkflow implementation

    @Override
    public void cancelRequest() throws WorkflowException {
        try {
            handleDocumentWorkflow.triggerAction("cancelRequest", new RequestPayload(new WorkflowRequest(subject), null));
        } catch (RepositoryException e) {
            throw new WorkflowException("Unable to create PublicationRequest from subject", e);
        }
    }

    // FullRequestWorkflow implementation

    @Override
    public void acceptRequest() throws WorkflowException {
        try {
            handleDocumentWorkflow.triggerAction("acceptRequest", new RequestPayload(new WorkflowRequest(subject), null));
        } catch (RepositoryException e) {
            throw new WorkflowException("Unable to create PublicationRequest from subject", e);
        }
    }

    @Override
    public void rejectRequest(final String reason) throws WorkflowException {
        try {
            handleDocumentWorkflow.triggerAction("rejectRequest", new RequestPayload(new WorkflowRequest(subject), reason));
        } catch (RepositoryException e) {
            throw new WorkflowException("Unable to create PublicationRequest from subject", e);
        }
    }

    // UnlockWorkflow implementation

    @Override
    public void unlock() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("unlock");
    }

    // VersionWorkflow implementation

    @Override
    public Document version() throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(handleDocumentWorkflow.triggerAction("version"));
    }

    @Override
    public Document revert(final Calendar historic) throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(handleDocumentWorkflow.triggerAction("revert", new VersionPayload(historic, null)));
    }

    @Override
    public Document restoreTo(final Document target) throws WorkflowException, RepositoryException {
        Calendar historic = ((Version) subject).getCreated();
        return workflowResultToUserDocument(handleDocumentWorkflow.triggerAction("restoreTo", new VersionPayload(historic, new DocumentVariant(subject))));
    }

    @Override
    public Document restore(final Calendar historic) throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(handleDocumentWorkflow.triggerAction("restore", new VersionPayload(historic, null)));
    }

    @Override
    public Document restore(final Calendar historic, final Map<String,String[]> replacements) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    @Override
    @SuppressWarnings("unchecked")
    public SortedMap<Calendar, Set<String>> list() throws WorkflowException {
        return (SortedMap<Calendar, Set<String>>) handleDocumentWorkflow.triggerAction("listVersions");
    }

    @Override
    public Document retrieve(final Calendar historic) throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(handleDocumentWorkflow.triggerAction("retrieve", historic));
    }
}
