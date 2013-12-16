/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.onehippo.repository.scxml.SCXMLWorkflowExecutor;

public class DocumentWorkflowImpl extends WorkflowImpl implements DocumentWorkflow {

    private static final long serialVersionUID = 1L;

    private static final String SCXML_DEFINITION_ID = "document-workflow";

    private SCXMLWorkflowExecutor workflowExecutor;
    private DocumentHandle dm;

    public DocumentWorkflowImpl() throws RemoteException {
    }

    SCXMLWorkflowExecutor getWorkflowExecutor() {
        return workflowExecutor;
    }

    protected Document toUserDocument(Document document) throws RepositoryException {
        return new Document(getWorkflowContext().getUserSession().getNodeByIdentifier(document.getIdentity()));
    }

    protected Document workflowResultToUserDocument(Object obj) throws RepositoryException {
        Document document = null;
        if (obj != null) {
            if (obj instanceof PublishableDocument) {
                document = (PublishableDocument)obj;
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

        // Critical: MUST use getNonChainingWorkflowContext() or getWorkflowContext(null), NOT getWorkflowContext()!
        dm = new DocumentHandle(getNonChainingWorkflowContext(), node);

        try {
            workflowExecutor = new SCXMLWorkflowExecutor(SCXML_DEFINITION_ID, dm);
            workflowExecutor.start();
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
        if (workflowExecutor.isStarted()) {
            hints.putAll(dm.getInfo());
            hints.putAll(dm.getActions());
        }
        return hints;
    }

    // EditableWorkflow implementation

    @Override
    public boolean isModified() throws WorkflowException, RepositoryException {
        return (Boolean)workflowExecutor.triggerAction("checkModified");
    }

    @Override
    public Document obtainEditableInstance() throws RepositoryException, WorkflowException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("obtainEditableInstance"));
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("commitEditableInstance"));
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("disposeEditableInstance"));
    }

    // BasicReviewedActionsWorkflow implementation

    @Override
    public void requestDeletion() throws WorkflowException {
        workflowExecutor.triggerAction("requestDelete");
    }

    @Override
    public void requestDepublication() throws WorkflowException {
        workflowExecutor.triggerAction("requestDepublish");
    }

    @Override
    public void requestDepublication(final Date publicationDate) throws WorkflowException {
        workflowExecutor.triggerAction("requestDepublish", publicationDate);
    }

    @Override
    public void requestPublication() throws WorkflowException {
        workflowExecutor.triggerAction("publish");
    }

    @Override
    public void requestPublication(final Date publicationDate) throws WorkflowException {
        workflowExecutor.triggerAction("requestPublish", publicationDate);
    }

    @Override
    public void requestPublication(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    // FullReviewedActionsWorkflow implementation

    @Override
    public void delete() throws WorkflowException {
        workflowExecutor.triggerAction("delete");
    }

    @Override
    public void rename(final String newName) throws WorkflowException {
        workflowExecutor.triggerAction("rename", newName);
    }

    @Override
    public void copy(final Document destination, final String newName) throws WorkflowException {
        workflowExecutor.triggerAction("copy", new DocumentCopyMovePayload(destination, newName));
    }

    @Override
    public void move(final Document destination, final String newName) throws WorkflowException {
        workflowExecutor.triggerAction("move", new DocumentCopyMovePayload(destination, newName));
    }

    @Override
    public void depublish() throws WorkflowException {
        workflowExecutor.triggerAction("depublish");
    }

    @Override
    public void depublish(final Date depublicationDate) throws WorkflowException {
        workflowExecutor.triggerAction("depublish", depublicationDate);
    }

    @Override
    public void publish() throws WorkflowException {
        workflowExecutor.triggerAction("publish");
    }

    @Override
    public void publish(final Date publicationDate) throws WorkflowException {
        workflowExecutor.triggerAction("publish", publicationDate);
    }

    @Override
    public void publish(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    // BasicRequestWorkflow implementation

    @Override
    public void cancelRequest() throws WorkflowException {
        workflowExecutor.triggerAction("cancelRequest");
    }

    // FullRequestWorkflow implementation

    @Override
    public void acceptRequest() throws WorkflowException {
        workflowExecutor.triggerAction("acceptRequest");
    }

    @Override
    public void rejectRequest(final String reason) throws WorkflowException {
        workflowExecutor.triggerAction("rejecctRequest", reason);
    }

    // UnlockWorkflow implementation

    @Override
    public void unlock() throws WorkflowException {
        workflowExecutor.triggerAction("unlock");
    }

    // VersionWorkflow implementation

    @Override
    public Document version() throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("version"));
    }

    @Override
    public Document revert(final Calendar historic) throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("revert", historic));
    }

    @Override
    public Document restoreTo(final Document target) throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("restoreTo", target));
    }

    @Override
    public Document restore(final Calendar historic) throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("restore", historic));
    }

    @Override
    public Document restore(final Calendar historic, final Map<String,String[]> replacements) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    @Override
    @SuppressWarnings("unchecked")
    public SortedMap<Calendar, Set<String>> list() throws WorkflowException {
        return (SortedMap<Calendar, Set<String>>) workflowExecutor.triggerAction("listVersions");
    }

    @Override
    public Document retrieve(final Calendar historic) throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("retrieve", historic));
    }
}
