/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.ACCEPT_REQUEST;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.CANCEL_REQUEST;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.CHECK_MODIFIED;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.COMMIT_EDITABLE_INSTANCE;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.COPY;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.DELETE;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.DEPUBLISH;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.DISPOSE_EDITABLE_INSTANCE;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.LIST_VERSIONS;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.MOVE;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.OBTAIN_EDITABLE_INSTANCE;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.PUBLISH;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.REJECT_REQUEST;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.RENAME;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.REQUEST_DELETE;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.REQUEST_DEPUBLICATION;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.REQUEST_PUBLICATION;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.RESTORE_VERSION;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.RETRIEVE_VERSION;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.UNLOCK;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.VERSION;
import static org.onehippo.repository.documentworkflow.DocumentWorkflowAction.VERSION_RESTORE_TO;

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
    public static final String UNSUPPORTED = "unsupported";


    private SCXMLWorkflowExecutor<SCXMLWorkflowContext, DocumentHandle> workflowExecutor;

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

    protected SCXMLWorkflowExecutor getWorkflowExecutor() {
        return workflowExecutor;
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
        try {
            // instantiate SCXMLWorkflowExecutor using default SCXMLWorkflowContext and DocumentHandle implementing SCXMLWorkflowData
            workflowExecutor = new SCXMLWorkflowExecutor<>(new SCXMLWorkflowContext(getScxmlId(), getWorkflowContext()),      createDocumentHandle(node));
        }
        catch (WorkflowException wfe) {
            if (wfe.getCause() != null && wfe.getCause() instanceof RepositoryException) {
                throw (RepositoryException)wfe.getCause();
            }
            throw new RepositoryException(wfe);
        }
    }

    protected String getScxmlId() {
        final RepositoryMap workflowConfiguration = getWorkflowContext().getWorkflowConfiguration();
        // check if a custom scxml-definition identifier is configured for this workflow instance
        if (workflowConfiguration != null && workflowConfiguration.exists() &&
                workflowConfiguration.get(SCXML_DEFINITION_KEY) instanceof String) {
            // use custom scxml-definition identifier
            return (String) workflowConfiguration.get(SCXML_DEFINITION_KEY);
        }
        return "documentworkflow";
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
        return (boolean) transition(CHECK_MODIFIED);
    }

    @Override
    public Document obtainEditableInstance() throws RepositoryException, WorkflowException {
        return (Document) transition(OBTAIN_EDITABLE_INSTANCE);
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException, RepositoryException {
        return (Document) transition(COMMIT_EDITABLE_INSTANCE);
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException, RepositoryException {
        return (Document) transition(DISPOSE_EDITABLE_INSTANCE);
    }

    // BasicReviewedActionsWorkflow implementation

    @Override
    public void requestDeletion() throws WorkflowException {
        transition(REQUEST_DELETE);
    }

    @Override
    public void requestDepublication() throws WorkflowException {
        transition(REQUEST_DEPUBLICATION);
    }

    @Override
    public void requestDepublication(final Date depublicationDate) throws WorkflowException {
        transition(getBuilder()
                .action(REQUEST_DEPUBLICATION)
                .eventPayload(TARGET_DATE,depublicationDate)
                .build());
    }

    @Override
    public void requestPublication() throws WorkflowException {
        transition(REQUEST_PUBLICATION);
    }

    @Override
    public void requestPublication(final Date publicationDate) throws WorkflowException {
        transition(getBuilder()
                .action(REQUEST_PUBLICATION)
                .eventPayload(TARGET_DATE,publicationDate)
                .build());
    }

    @Override
    public void requestPublication(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException(UNSUPPORTED);
    }

    @Override
    public void requestPublicationDepublication(final Date publicationDate, final Date unpublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        throw new WorkflowException(UNSUPPORTED);
    }

    // FullReviewedActionsWorkflow implementation

    @Override
    public void delete() throws WorkflowException {
        transition(DELETE);
    }

    @Override
    public void rename(final String newName) throws WorkflowException {
        transition(getBuilder()
                .action(RENAME)
                .eventPayload(NAME,newName)
                .build());
    }

    @Override
    public void copy(final Document destination, final String newName) throws WorkflowException {
        transition(getBuilder()
                .action(COPY)
                .eventPayload(DESTINATION,destination, NAME,newName)
                .build());
    }

    @Override
    public void move(final Document destination, final String newName) throws WorkflowException {
        transition(getBuilder()
                .action(MOVE)
                .eventPayload(DESTINATION,destination, NAME,newName)
                .build());
    }

    @Override
    public void depublish() throws WorkflowException {
        transition(DEPUBLISH);
    }

    @Override
    public void depublish(final Date depublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        transition(getBuilder()
                .action(DEPUBLISH)
                .eventPayload(TARGET_DATE,depublicationDate)
                .build());
    }

    @Override
    public void publish() throws WorkflowException {
        transition(PUBLISH);
    }

    @Override
    public void publish(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        transition(getBuilder()
                .action(PUBLISH)
                .eventPayload(TARGET_DATE,publicationDate)
                .build());
    }

    @Override
    public void publish(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException(UNSUPPORTED);
    }

    // Request Workflow on Document handle level

    @Override
    public void cancelRequest(String requestIdentifier) throws WorkflowException {
        transition(getBuilder()
                .action(CANCEL_REQUEST)
                .requestIdentifier(requestIdentifier)
                .build());
    }

       @Override
    public void cancelRequest(String requestIdentifier, String reason) throws WorkflowException {
        cancelRequest(requestIdentifier);
    }

    @Override
    public void acceptRequest(String requestIdentifier) throws WorkflowException {
        transition(getBuilder()
                .action(ACCEPT_REQUEST)
                .requestIdentifier(requestIdentifier)
                .build());
    }

    @Override
    public void acceptRequest(String requestIdentifier, String reason) throws WorkflowException{
        acceptRequest(requestIdentifier);
    }

    @Override
    public void rejectRequest(String requestIdentifier, final String reason) throws WorkflowException {
        transition(getBuilder()
                .action(REJECT_REQUEST)
                .requestIdentifier(requestIdentifier)
                .eventPayload("reason",reason)
                .build());
    }

    // UnlockWorkflow implementation

    @Override
    public void unlock() throws WorkflowException {
        transition(UNLOCK);
    }

    // Version Workflow on Document handle level

    @Override
    public Document version() throws WorkflowException, RepositoryException {
        return (Document) transition(VERSION);
    }

    @Override
    public Document versionRestoreTo(final Calendar historic, Document target) throws WorkflowException, RepositoryException {
        return (Document) transition(getBuilder()
                .action(VERSION_RESTORE_TO)
                .eventPayload(DATE,historic, TARGET_DOCUMENT,target)
                .build());
    }

    @Override
    public Document restoreVersion(final Calendar historic) throws WorkflowException, RepositoryException {
        return (Document) transition(getBuilder()
                .action(RESTORE_VERSION)
                .eventPayload(DATE,historic)
                .build());
    }

    @Override
    @SuppressWarnings("unchecked")
    public SortedMap<Calendar, Set<String>> listVersions() throws WorkflowException {
        return (SortedMap<Calendar, Set<String>>) transition(LIST_VERSIONS);
    }

    @Override
    public Document retrieveVersion(final Calendar historic) throws WorkflowException, RepositoryException {
        return (Document) transition(getBuilder()
                .action(RETRIEVE_VERSION)
                .eventPayload(DATE,historic)
                .build());
    }

    @Override
    public Object transition(DocumentWorkflowTransition transition) throws WorkflowException {
        workflowExecutor.start();
        final Map<String, Object> eventPayload = transition.getEventPayload();
        Map<String, Boolean> actionsMap = transition.getActionsMap();
        final String requestIdentifier = transition.getRequestIdentifier();
        final String action = transition.getAction();
        if (requestIdentifier !=null){
            addRequestToEventPayload(eventPayload, requestIdentifier);
            if (actionsMap==null){
                actionsMap = new HashMap<>();
            }
            actionsMap.putAll(getRequestActionActions(requestIdentifier, action));
        }
        return triggerAction(eventPayload, actionsMap, action);
    }

    public void addRequestToEventPayload(final Map<String, Object> eventPayload, final String requestIdentifier) {
        eventPayload.put(REQUEST,workflowExecutor.getData().getRequests().get(requestIdentifier));
    }

    public Object triggerAction(final Map<String, Object> eventPayload, final Map<String, Boolean> actionsMap, final String action) throws WorkflowException {
        return actionsMap==null?workflowExecutor.triggerAction(action, eventPayload):workflowExecutor.triggerAction(action, actionsMap, eventPayload);
    }

    private  Object transition(DocumentWorkflowAction documentWorkflowAction) throws WorkflowException {
        return transition(getBuilder().action(documentWorkflowAction.getAction()).build());
    }

    private DocumentWorkflowTransition.Builder getBuilder() {
        return new DocumentWorkflowTransition.Builder();
    }

}
