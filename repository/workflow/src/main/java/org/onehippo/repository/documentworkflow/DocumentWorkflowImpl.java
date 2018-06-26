/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.api.DocumentWorkflowAction;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.WorkflowAction;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.repository.scxml.SCXMLWorkflowContext;
import org.onehippo.repository.scxml.SCXMLWorkflowExecutor;

import static org.hippoecm.repository.api.DocumentWorkflowAction.DocumentPayloadKey.BRANCH_ID;
import static org.hippoecm.repository.api.DocumentWorkflowAction.DocumentPayloadKey.BRANCH_NAME;
import static org.hippoecm.repository.api.DocumentWorkflowAction.DocumentPayloadKey.DATE;
import static org.hippoecm.repository.api.DocumentWorkflowAction.DocumentPayloadKey.DESTINATION;
import static org.hippoecm.repository.api.DocumentWorkflowAction.DocumentPayloadKey.NAME;
import static org.hippoecm.repository.api.DocumentWorkflowAction.DocumentPayloadKey.REASON;
import static org.hippoecm.repository.api.DocumentWorkflowAction.DocumentPayloadKey.REQUEST;
import static org.hippoecm.repository.api.DocumentWorkflowAction.DocumentPayloadKey.STATE;
import static org.hippoecm.repository.api.DocumentWorkflowAction.DocumentPayloadKey.TARGET_DATE;
import static org.hippoecm.repository.api.DocumentWorkflowAction.DocumentPayloadKey.TARGET_DOCUMENT;
import static org.hippoecm.repository.api.DocumentWorkflowAction.checkModified;
import static org.hippoecm.repository.api.DocumentWorkflowAction.requestDelete;

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

    /**
     * All implementations of a work-flow must provide a single, no-argument constructor.
     *
     * @throws RemoteException mandatory exception that must be thrown by all Remote objects
     */
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
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
                    throw new WorkflowException("Invalid document handle factory class '" + className + "'", e);
                }
            }
        }
        return new DocumentHandle(node);
    }

    protected SCXMLWorkflowExecutor getWorkflowExecutor() {
        return workflowExecutor;
    }

    /**
     * @deprecated since 5.1.0 Do not use this method any more to create a Map: Create it yourself
     */
    @Deprecated
    protected final Map<String, Object> createPayload(String var, Object val) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(var, val);
        return map;
    }


    /**
     * @deprecated since 5.1.0 Do not use this method any more to create a Map: Create it yourself
     */
    @Deprecated
    protected final Map<String, Object> createPayload(String var1, Object val1, String var2, Object val2) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(var1, val1);
        map.put(var2, val2);
        return map;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Boolean> getRequestActionActions(String requestIdentifier, String action) throws WorkflowException {
        Map<String, Map<String, Boolean>> requestActionsInfo =
                (Map<String, Map<String, Boolean>>) workflowExecutor.getContext().getFeedback().get("requests");
        if (requestActionsInfo != null) {
            Map<String, Boolean> requestActions = requestActionsInfo.get(requestIdentifier);
            if (requestActions != null) {
                return requestActions;
            }
        }
        throw new WorkflowException("Cannot invoke workflow " + workflowExecutor.getContext().getScxmlId() + " action " + action + ": request " +
                requestIdentifier + " not found");
    }

    // Workflow implementation / WorkflowImpl override

    @Override
    public void setNode(final Node node) throws RepositoryException {
        super.setNode(node);
        try {
            // instantiate SCXMLWorkflowExecutor using default SCXMLWorkflowContext and DocumentHandle implementing SCXMLWorkflowData
            workflowExecutor = new SCXMLWorkflowExecutor<>(new SCXMLWorkflowContext(getScxmlId(), getWorkflowContext()), createDocumentHandle(node));
        } catch (WorkflowException wfe) {
            if (wfe.getCause() != null && wfe.getCause() instanceof RepositoryException) {
                throw (RepositoryException) wfe.getCause();
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
                entry.setValue((Serializable) Collections.unmodifiableCollection((Collection) entry.getValue()));
            }
        }
        return Collections.unmodifiableMap(hints);
    }

    // EditableWorkflow implementation

    @Override
    public boolean isModified() throws WorkflowException, RepositoryException {
        return (boolean) triggerAction(checkModified());
    }

    @Override
    public Document obtainEditableInstance() throws WorkflowException {
        return (Document) triggerAction(DocumentWorkflowAction.obtainEditableInstance());
    }

    @Override
    public Document obtainEditableInstance(final String branchId, final String branchName) throws WorkflowException {
        // TODO (meggermont): implement
        return this.obtainEditableInstance();
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException, RepositoryException {
        return (Document) triggerAction(DocumentWorkflowAction.commitEditableInstance());
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException, RepositoryException {
        return (Document) triggerAction(DocumentWorkflowAction.disposeEditableInstance());
    }

    // BasicReviewedActionsWorkflow implementation

    @Override
    public void requestDeletion() throws WorkflowException {
        triggerAction(requestDelete());
    }

    @Override
    public void requestDepublication() throws WorkflowException {
        triggerAction(DocumentWorkflowAction.requestDepublication());
    }

    @Override
    public void requestDepublication(final Date depublicationDate) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.requestDepublication().addEventPayload(TARGET_DATE, depublicationDate));
    }

    @Override
    public void requestPublication() throws WorkflowException {
        triggerAction(DocumentWorkflowAction.requestPublication());
    }

    @Override
    public void requestPublication(final Date publicationDate) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.requestPublication().addEventPayload(TARGET_DATE, publicationDate));
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
        triggerAction(DocumentWorkflowAction.delete());
    }

    @Override
    public void rename(final String newName) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.rename().addEventPayload(NAME, newName));
    }

    @Override
    public void copy(final Document destination, final String newName) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.copy().addEventPayload(DESTINATION, destination).addEventPayload(NAME, newName));
    }

    @Override
    public void move(final Document destination, final String newName) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.move().addEventPayload(DESTINATION, destination).addEventPayload(NAME, newName));
    }

    @Override
    public void depublish() throws WorkflowException {
        triggerAction(DocumentWorkflowAction.depublish());
    }

    @Override
    public void depublish(final Date depublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        triggerAction(DocumentWorkflowAction.depublish().addEventPayload(TARGET_DATE, depublicationDate));
    }

    @Override
    public void publish() throws WorkflowException {
        triggerAction(DocumentWorkflowAction.publish());
    }

    @Override
    public void publish(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        triggerAction(DocumentWorkflowAction.publish().addEventPayload(TARGET_DATE, publicationDate));
    }

    @Override
    public void publish(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException(UNSUPPORTED);
    }

    // Request Workflow on Document handle level

    @Override
    public void cancelRequest(String requestIdentifier) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.cancelRequest().requestIdentifier(requestIdentifier));
    }

    @Override
    public void cancelRequest(String requestIdentifier, String reason) throws WorkflowException {
        cancelRequest(requestIdentifier);
    }

    @Override
    public void acceptRequest(String requestIdentifier) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.acceptRequest().requestIdentifier(requestIdentifier));
    }

    @Override
    public void acceptRequest(String requestIdentifier, String reason) throws WorkflowException {
        acceptRequest(requestIdentifier);
    }

    @Override
    public void rejectRequest(String requestIdentifier, final String reason) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.rejectRequest().requestIdentifier(requestIdentifier).addEventPayload(REASON, reason));
    }

    // UnlockWorkflow implementation

    @Override
    public void unlock() throws WorkflowException {
        triggerAction(DocumentWorkflowAction.unlock());
    }

    // Version Workflow on Document handle level

    @Override
    public Document version() throws WorkflowException, RepositoryException {
        return (Document) triggerAction(DocumentWorkflowAction.version());
    }

    @Override
    public Document versionRestoreTo(final Calendar historic, Document target) throws WorkflowException, RepositoryException {
        return (Document) triggerAction(DocumentWorkflowAction.versionRestoreTo().addEventPayload(DATE, historic).addEventPayload(TARGET_DOCUMENT, target));
    }

    @Override
    public Document restoreVersion(final Calendar historic) throws WorkflowException, RepositoryException {
        return (Document) triggerAction(DocumentWorkflowAction.restoreVersion().addEventPayload(DATE, historic));
    }

    @Override
    @SuppressWarnings("unchecked")
    public SortedMap<Calendar, Set<String>> listVersions() throws WorkflowException {
        return (SortedMap<Calendar, Set<String>>) triggerAction(DocumentWorkflowAction.listVersions());
    }

    @Override
    public Document retrieveVersion(final Calendar historic) throws WorkflowException, RepositoryException {
        return (Document) triggerAction(DocumentWorkflowAction.retrieveVersion().addEventPayload(DATE, historic));
    }

    @Override
    public Set<String> listBranches() throws WorkflowException {
        return (Set<String>) triggerAction(DocumentWorkflowAction.listBranches());
    }

    @Override
    public Document branch(final String branchId, final String branchName) throws WorkflowException {
        return (Document) triggerAction(DocumentWorkflowAction.branch()
                .addEventPayload(BRANCH_ID, branchId)
                .addEventPayload(BRANCH_NAME, branchName));
    }

    @Override
    public Document getBranch(final String branchId, final WorkflowUtils.Variant state) throws WorkflowException {
        return (Document) triggerAction(DocumentWorkflowAction.getBranch()
                .addEventPayload(BRANCH_ID, branchId)
                .addEventPayload(STATE, state.getState()));
    }

    @Override
    public void removeBranch(final String branchId) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.removeBranch().addEventPayload(BRANCH_ID, branchId));
    }

    @Override
    public Document checkoutBranch(final String branchId) throws WorkflowException {
        return (Document) triggerAction(DocumentWorkflowAction.checkoutBranch()
                .addEventPayload(BRANCH_ID, branchId));
    }

    @Override
    public void reintegrateBranch(final String branchId, final boolean publish) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.reintegrateBranch()
                .addEventPayload(BRANCH_ID, branchId)
                .addEventPayload("publish", publish));
    }

    @Override
    public void publishBranch(final String branchId) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.publishBranch()
                .addEventPayload(BRANCH_ID, branchId));
    }

    @Override
    public void depublishBranch(final String branchId) throws WorkflowException {
        triggerAction(DocumentWorkflowAction.depublishBranch()
                .addEventPayload(BRANCH_ID, branchId));
    }

    @Override
    public Object triggerAction(final WorkflowAction action) throws WorkflowException {
        if (!(action instanceof DocumentWorkflowAction)) {
            throw new IllegalArgumentException(String.format("action class must be of type '%s' for document workflow but " +
                    "was of type '%s'.", DocumentWorkflowAction.class.getName(), action.getClass().getName()));
        }
        DocumentWorkflowAction dwfAction = (DocumentWorkflowAction) action;
        workflowExecutor.start();
        final Map<String, Object> eventPayload = dwfAction.getEventPayload();
        final String requestIdentifier = dwfAction.getRequestIdentifier();
        if (requestIdentifier != null) {
            dwfAction.addEventPayload(REQUEST, workflowExecutor.getData().getRequests().get(requestIdentifier));
        }
        if (requestIdentifier == null) {
            return workflowExecutor.triggerAction(dwfAction.getAction(), eventPayload);
        } else {
            return workflowExecutor.triggerAction(dwfAction.getAction(), getRequestActionActions(requestIdentifier, dwfAction.getAction()), eventPayload);
        }
    }

}
