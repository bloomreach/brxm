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
import java.util.Collection;
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
import org.onehippo.repository.scxml.SCXMLWorkflowContext;
import org.onehippo.repository.scxml.SCXMLWorkflowExecutor;

/**
 * DocumentWorkflow implementation which delegates the document workflow state management and action processing
 * to an SCXML state machine executed through an {@link SCXMLWorkflowExecutor} using a {@link DocumentHandle}
 * instance as {@link org.onehippo.repository.scxml.SCXMLWorkflowData} backing model object.
 * <p>
 * All workflow operations will (re)start the backing SCXML state machine to ensure the current external (repository)
 * state is (re)evaluated again and the current set of allowable actions, as determined by the state machine, is used to
 * validate if the intended operation (action) is actually allowed within the current state.
 * </p>
 */
public class DocumentWorkflowImpl extends WorkflowImpl implements DocumentWorkflow {

    private static final long serialVersionUID = 1L;

    /**
     * Workflow repository configuration property name under which a custom SCXML definition id can be provided.
     * If undefined SCXML definition id "documentworkflow" will be used.
     */
    public static final String SCXML_DEFINITION_KEY = "scxml-definition";

    /**
     * Optional workflow repository configuration property name through which a custom factory class name can be
     * configured for creating a DocumentHandle instance,
     *
     * @see #createDocumentHandle(javax.jcr.Node)
     */
    public static final String DOCUMENT_HANDLE_FACTORY_CLASS_KEY = "documentHandleFactoryClass";

    private SCXMLWorkflowExecutor<SCXMLWorkflowContext, DocumentHandle> workflowExecutor;

    public DocumentWorkflowImpl() throws RemoteException {
    }

    @SuppressWarnings("unchecked")
    protected DocumentHandle createDocumentHandle(Node node) throws WorkflowException {
        final RepositoryMap workflowConfiguration = getWorkflowContext().getWorkflowConfiguration();
        Object configurationValue;
        if (workflowConfiguration != null && workflowConfiguration.exists() &&
                (configurationValue = workflowConfiguration.get(DOCUMENT_HANDLE_FACTORY_CLASS_KEY)) != null) {
            String className = configurationValue.toString().trim();
            if (!className.isEmpty()) {
                try {
                    Class<DocumentHandleFactory> clazz = (Class<DocumentHandleFactory>) Class.forName(className);
                    return clazz.newInstance().createDocumentHandle(node);
                } catch (ClassNotFoundException|InstantiationException|IllegalAccessException|ClassCastException e) {
                    throw new WorkflowException("Invalid document handle factory class '"+className+"'", e);
                }
            }
        }
        return new DocumentHandle(node);
    }

    SCXMLWorkflowExecutor getWorkflowExecutor() {
        return workflowExecutor;
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

    @SuppressWarnings("unchecked")
    protected Map<String, Boolean> getRequestActionActions(String requestIdentifier, String action) throws WorkflowException {
        Map<String, Map<String, Boolean>> requestActionsInfo =
                (Map<String, Map<String, Boolean>>)workflowExecutor.getContext().getFeedback().get("requests");
        if (requestActionsInfo != null) {
            Map<String, Boolean> requestActions = requestActionsInfo.get(requestIdentifier);
            if (requestActions != null) {
                return requestActions;
            }
        }
        throw new WorkflowException("Cannot invoke workflow "+workflowExecutor.getContext().getScxmlId()+" action "+action+": request "+
                requestIdentifier+" not found");
    }

    // Workflow implementation / WorkflowImpl override

    @Override
    public void setNode(final Node node) throws RepositoryException {
        super.setNode(node);

        String scxmlId = "documentworkflow";

        try {
            final RepositoryMap workflowConfiguration = getWorkflowContext().getWorkflowConfiguration();
            // check if a custom scxml-definition identifier is configured for this workflow instance
            if (workflowConfiguration != null && workflowConfiguration.exists() &&
                    workflowConfiguration.get(SCXML_DEFINITION_KEY) instanceof String) {
                // use custom scxml-definition identifier
                scxmlId = (String) workflowConfiguration.get(SCXML_DEFINITION_KEY);
            }

            // instantiate SCXMLWorkflowExecutor using default SCXMLWorkflowContext and DocumentHandle implementing SCXMLWorkflowData
            workflowExecutor = new SCXMLWorkflowExecutor<>(new SCXMLWorkflowContext(scxmlId, getWorkflowContext()), createDocumentHandle(node));
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
        hints.putAll(workflowExecutor.getContext().getFeedback());
        hints.putAll(workflowExecutor.getContext().getActions());
        for (Map.Entry<String, Serializable> entry : hints.entrySet()) {
            if (entry.getValue() instanceof Collection) {
                // protect against modifications
                entry.setValue((Serializable)Collections.unmodifiableCollection((Collection)entry.getValue()));
            }
        }
        return Collections.unmodifiableMap(hints);
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
        return (Document)workflowExecutor.triggerAction("obtainEditableInstance");
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException, RepositoryException {
        workflowExecutor.start();
        return (Document)workflowExecutor.triggerAction("commitEditableInstance");
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException, RepositoryException {
        workflowExecutor.start();
        return (Document)workflowExecutor.triggerAction("disposeEditableInstance");
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
                createPayload("request", workflowExecutor.getData().getRequests().get(requestIdentifier)));
    }

    @Override
    public void acceptRequest(String requestIdentifier) throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("acceptRequest", getRequestActionActions(requestIdentifier, "acceptRequest"),
                createPayload("request", workflowExecutor.getData().getRequests().get(requestIdentifier)));
    }

    @Override
    public void rejectRequest(String requestIdentifier, final String reason) throws WorkflowException {
        workflowExecutor.start();
        workflowExecutor.triggerAction("rejectRequest", getRequestActionActions(requestIdentifier, "rejectRequest"),
                createPayload("request", workflowExecutor.getData().getRequests().get(requestIdentifier), "reason", reason));
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
        return (Document)workflowExecutor.triggerAction("version");
    }

    @Override
    public Document versionRestoreTo(final Calendar historic, Document target) throws WorkflowException, RepositoryException {
        workflowExecutor.start();
        return (Document)workflowExecutor.triggerAction("versionRestoreTo", createPayload("date", historic, "target", target));
    }

    @Override
    public Document restoreVersion(final Calendar historic) throws WorkflowException, RepositoryException {
        workflowExecutor.start();
        return (Document)workflowExecutor.triggerAction("restoreVersion", createPayload("date",historic));
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
        return (Document)workflowExecutor.triggerAction("retrieveVersion", createPayload("date", historic));
    }
}
