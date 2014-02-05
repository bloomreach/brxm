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
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.onehippo.repository.scxml.SCXMLWorkflowExecutor;

public class DocumentWorkflowImpl extends WorkflowImpl implements DocumentWorkflow {

    private static final long serialVersionUID = 1L;

    public static final String SCXML_DEFINITION_KEY = "scxml-definition";

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

        String scxmlId = "documentworkflow";
        // Critical: MUST use getNonChainingWorkflowContext() or getWorkflowContext(null), NOT getWorkflowContext()!

        try {
            final RepositoryMap workflowConfiguration = getWorkflowContext().getWorkflowConfiguration();
            if (workflowConfiguration != null && workflowConfiguration.exists() && workflowConfiguration.get(SCXML_DEFINITION_KEY) instanceof String) {
                scxmlId = (String) workflowConfiguration.get(SCXML_DEFINITION_KEY);
            }

            dm = new DocumentHandle(scxmlId, getNonChainingWorkflowContext(), node);
            workflowExecutor = new SCXMLWorkflowExecutor(dm);
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
    public Map<String, Serializable> hints() throws WorkflowException {
        if (workflowExecutor.ensureStarted()) {
            Map<String, Serializable> hints = super.hints();
            hints.putAll(dm.getInfo());
            hints.putAll(dm.getActions());
            return Collections.unmodifiableMap(hints);
        }
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Serializable> getInfo() throws WorkflowException {
        if (workflowExecutor.ensureStarted()) {
            return Collections.unmodifiableMap(dm.getInfo());
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Boolean> getActions() throws WorkflowException {
        if (workflowExecutor.ensureStarted()) {
            return Collections.unmodifiableMap(dm.getActions());
        } else {
            return Collections.emptyMap();
        }
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
        workflowExecutor.triggerAction("requestDepublish", null);
    }

    @Override
    public void requestDepublication(final Date publicationDate) throws WorkflowException {
        workflowExecutor.triggerAction("requestDepublish", publicationDate);
    }

    @Override
    public void requestPublication() throws WorkflowException {
        workflowExecutor.triggerAction("requestPublish", null);
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
    public void depublish(final Date depublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        // create 'future' documentworkflow proxy
        WorkflowContext ctx = getWorkflowContext().getWorkflowContext(depublicationDate);
        DocumentWorkflow workflow = (DocumentWorkflow)ctx.getWorkflow("default");
        workflow.depublish();
    }

    @Override
    public void publish() throws WorkflowException {
        workflowExecutor.triggerAction("publish");
    }

    @Override
    public void publish(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        // create 'future' documentworkflow proxy
        WorkflowContext ctx = getWorkflowContext().getWorkflowContext(publicationDate);
        DocumentWorkflow workflow = (DocumentWorkflow)ctx.getWorkflow("default");
        workflow.publish();
    }

    @Override
    public void publish(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    // Request Workflow on Document handle level

    @Override
    public void cancelRequest(String requestIdentifier) throws WorkflowException {
        workflowExecutor.triggerAction("cancelRequest", requestIdentifier);
    }

    @Override
    public void acceptRequest(String requestIdentifier) throws WorkflowException {
        workflowExecutor.triggerAction("acceptRequest", requestIdentifier);
    }

    @Override
    public void rejectRequest(String requestIdentifier, final String reason) throws WorkflowException {
        workflowExecutor.triggerAction("rejectRequest", new RejectRequestPayload(requestIdentifier, reason));
    }

    // UnlockWorkflow implementation

    @Override
    public void unlock() throws WorkflowException {
        workflowExecutor.triggerAction("unlock");
    }

    // Version Workflow on Document handle level

    @Override
    public Document version() throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("version"));
    }

    @Override
    public Document versionRestoreTo(final Calendar historic, Document target) throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("versionRestoreTo", new VersionRestoreToPayload(historic, target)));
    }

    @Override
    public Document restoreVersion(final Calendar historic) throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("restoreVersion", historic));
    }

    @Override
    @SuppressWarnings("unchecked")
    public SortedMap<Calendar, Set<String>> listVersions() throws WorkflowException {
        return (SortedMap<Calendar, Set<String>>) workflowExecutor.triggerAction("listVersions");
    }

    @Override
    public Document retrieveVersion(final Calendar historic) throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(workflowExecutor.triggerAction("retrieveVersion", historic));
    }
}
