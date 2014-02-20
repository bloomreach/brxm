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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.RepositoryMap;
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

    protected Map<String, Object> createPayload(String var, Object val) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(var, val);
        return map;
    }

    protected Map<String, Object> createPayload(String var1, Object val1, String var2, Object val2) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(var1, val1);
        map.put(var2, val2);
        return map;
    }

    protected Map<String, Boolean> getRequestActionActions(String requestIdentifier, String action) throws WorkflowException {
        Map<String, Map<String, Boolean>> requestActionsInfo =
                (Map<String, Map<String, Boolean>>)dm.getInfo().get("requests");
        if (requestActionsInfo != null) {
            Map<String, Boolean> requestActions = requestActionsInfo.get(requestIdentifier);
            if (requestActions != null) {
                return requestActions;
            }
        }
        throw new WorkflowException("Cannot invoke workflow "+dm.getScxmlId()+" action "+action+": request "+
                requestIdentifier+" not found");
    }

    // Workflow implementation / WorkflowImpl override

    @Override
    public void setNode(final Node node) throws RepositoryException {
        super.setNode(node);

        String scxmlId = "documentworkflow";

        try {
            final RepositoryMap workflowConfiguration = getWorkflowContext().getWorkflowConfiguration();
            if (workflowConfiguration != null && workflowConfiguration.exists() &&
                    workflowConfiguration.get(SCXML_DEFINITION_KEY) instanceof String) {
                scxmlId = (String) workflowConfiguration.get(SCXML_DEFINITION_KEY);
            }

            dm = new DocumentHandle(scxmlId, getWorkflowContext(), node);
            workflowExecutor = new SCXMLWorkflowExecutor(dm);
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
        workflowExecutor.start();
        Map<String, Serializable> hints = super.hints();
        hints.putAll(dm.getInfo());
        hints.putAll(dm.getActions());
        return Collections.unmodifiableMap(hints);
    }

    @Override
    public Map<String, Serializable> getInfo() throws WorkflowException {
        workflowExecutor.start();
        return Collections.unmodifiableMap(dm.getInfo());
    }

    @Override
    public Map<String, Boolean> getActions() throws WorkflowException {
        workflowExecutor.start();
        return Collections.unmodifiableMap(dm.getActions());
    }

    // EditableWorkflow implementation

    @Override
    public boolean isModified() throws WorkflowException, RepositoryException {
        workflowExecutor.start();
        return (Boolean)workflowExecutor.triggerAction("checkModified");
    }

    @Override
    public Document obtainEditableInstance() throws RepositoryException, WorkflowException {
        workflowExecutor.start();
        return workflowResultToUserDocument(workflowExecutor.triggerAction("obtainEditableInstance"));
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException, RepositoryException {
        workflowExecutor.start();
        return workflowResultToUserDocument(workflowExecutor.triggerAction("commitEditableInstance"));
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException, RepositoryException {
        workflowExecutor.start();
        return workflowResultToUserDocument(workflowExecutor.triggerAction("disposeEditableInstance"));
    }

    // BasicReviewedActionsWorkflow implementation

    @Override
    public void requestDeletion() throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("requestDelete");
    }

    @Override
    public void requestDepublication() throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("requestDepublication");
    }

    @Override
    public void requestDepublication(final Date depublicationDate) throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("requestDepublication", createPayload("targetDate", depublicationDate));
    }

    @Override
    public void requestPublication() throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("requestPublication");
    }

    @Override
    public void requestPublication(final Date publicationDate) throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("requestPublication", createPayload("targetDate", publicationDate));
    }

    @Override
    public void requestPublication(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    // FullReviewedActionsWorkflow implementation

    @Override
    public void delete() throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("delete");
    }

    @Override
    public void rename(final String newName) throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("rename", createPayload("name", newName));
    }

    @Override
    public void copy(final Document destination, final String newName) throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("copy", createPayload("destination", destination, "name", newName));
    }

    @Override
    public void move(final Document destination, final String newName) throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("move", createPayload("destination", destination, "name", newName));
    }

    @Override
    public void depublish() throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("depublish");
    }

    @Override
    public void depublish(final Date depublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("depublish", createPayload("targetDate", depublicationDate));
    }

    @Override
    public void publish() throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("publish");
    }

    @Override
    public void publish(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("publish", createPayload("targetDate", publicationDate));
    }

    @Override
    public void publish(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    // Request Workflow on Document handle level

    @Override
    public void cancelRequest(String requestIdentifier) throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("cancelRequest", getRequestActionActions(requestIdentifier, "cancelRequest"),
                createPayload("request", dm.getRequests().get(requestIdentifier)));
    }

    @Override
    public void acceptRequest(String requestIdentifier) throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("acceptRequest", getRequestActionActions(requestIdentifier, "acceptRequest"),
                createPayload("request", dm.getRequests().get(requestIdentifier)));
    }

    @Override
    public void rejectRequest(String requestIdentifier, final String reason) throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("rejectRequest", getRequestActionActions(requestIdentifier, "rejectRequest"),
                createPayload("request", dm.getRequests().get(requestIdentifier), "reason", reason));
    }

    // UnlockWorkflow implementation

    @Override
    public void unlock() throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("unlock");
    }

    // Version Workflow on Document handle level

    @Override
    public Document version() throws WorkflowException, RepositoryException {
        workflowExecutor.start();
        return workflowResultToUserDocument(workflowExecutor.triggerAction("version"));
    }

    @Override
    public Document versionRestoreTo(final Calendar historic, Document target) throws WorkflowException, RepositoryException {
        workflowExecutor.start();
        return workflowResultToUserDocument(workflowExecutor.triggerAction("versionRestoreTo",
                createPayload("date", historic, "target", target)));
    }

    @Override
    public Document restoreVersion(final Calendar historic) throws WorkflowException, RepositoryException {
        workflowExecutor.start();
        return workflowResultToUserDocument(workflowExecutor.triggerAction("restoreVersion",
                createPayload("date",historic)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public SortedMap<Calendar, Set<String>> listVersions() throws WorkflowException {
        workflowExecutor.start();
        return (SortedMap<Calendar, Set<String>>) workflowExecutor.triggerAction("listVersions");
    }

    @Override
    public Document retrieveVersion(final Calendar historic) throws WorkflowException, RepositoryException {
        workflowExecutor.start();
        return workflowResultToUserDocument(workflowExecutor.triggerAction("retrieveVersion",
                createPayload("date", historic)));
    }
}
