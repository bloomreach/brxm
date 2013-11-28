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
package org.onehippo.repository.documentworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.model.ModelException;
import org.apache.commons.scxml2.model.SCXML;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.scxml.SCXMLException;
import org.onehippo.repository.scxml.SCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLRegistry;
import org.onehippo.repository.scxml.SCXMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCXMLReviewedActionsWorkflowImpl extends WorkflowImpl implements FullReviewedActionsWorkflow {

    private static final Logger log = LoggerFactory.getLogger(SCXMLReviewedActionsWorkflowImpl.class);

    private static final long serialVersionUID = 1L;

    private static final String SCXML_DEFINITION_ID = "reviewed-actions-workflow";

    private SCXMLExecutor scxmlExecutor;
    private DocumentHandle handle;

    public SCXMLReviewedActionsWorkflowImpl() throws RemoteException {
        super();
    }

    protected Document eventResultToUserDocument(Object obj) throws RepositoryException {
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

    protected void checkAllowedOperation(String hint, String error) throws WorkflowException {
        Boolean allowed = null;
        try {
            allowed = (Boolean)handle.getHints().get(hint);
        }
        catch (Exception e) {
            // ignore
        }
        if (allowed == null || !allowed.booleanValue()) {
            throw new WorkflowException(error);
        }
    }

    protected Document toUserDocument(Document document) throws RepositoryException {
        return new Document(getWorkflowContext().getUserSession().getNodeByIdentifier(document.getIdentity()));
    }

    @Override
    public void setNode(Node node) throws RepositoryException {
        super.setNode(node);

        handle = new DocumentHandle(getWorkflowContext(), node);

        try {
            SCXMLRegistry scxmlRegistry = HippoServiceRegistry.getService(SCXMLRegistry.class);
            SCXML scxml = scxmlRegistry.getSCXML(SCXML_DEFINITION_ID);

            if (scxml == null) {
                throw new WorkflowException("SCXML definition not found by id, '" + SCXML_DEFINITION_ID + "'.");
            }

            SCXMLExecutorFactory scxmlExecutorFactory = HippoServiceRegistry.getService(SCXMLExecutorFactory.class);
            scxmlExecutor = scxmlExecutorFactory.createSCXMLExecutor(scxml);

            scxmlExecutor.getRootContext().set("workflowContext", getWorkflowContext());
            scxmlExecutor.getRootContext().set("handle", handle);
            scxmlExecutor.getRootContext().set("eventResult", null);

            try {
                scxmlExecutor.go();
                log.info("scmxl.current.targets: {}", SCXMLUtils.getCurrentTransitionTargetIdList(scxmlExecutor));
                log.info("scmxl.handle.hints: {}", handle.getHints());
            } catch (ModelException e) {
                log.error("Failed to execute scxml", e);
            }
        }
        catch (WorkflowException wfe) {
            throw new RepositoryException(wfe);
        }
        catch (SCXMLException hse) {
            throw new RepositoryException(hse);
        }
    }

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> info = super.hints();
        info.putAll(handle.getHints());
        return info;
    }

    @Override
    public Document obtainEditableInstance() throws WorkflowException {
        log.info("obtain editable instance on document ");
        checkAllowedOperation("obtainEditableInstance", "Obtain editable instance operation not allowed");
        try {
            SCXMLUtils.triggerSignalEvents(scxmlExecutor, DocumentEvents.WFE_DOC_EDIT_OBTAIN);
            return eventResultToUserDocument(scxmlExecutor.getRootContext().get("eventResult"));
        } catch (ModelException e) {
            log.error("Error in WFE_DOC_EDIT_OBTAIN event triggering.", e);
            throw new WorkflowException("Error in WFE_DOC_EDIT_OBTAIN event triggering.", e);
        } catch (RepositoryException ex) {
            log.error("Error in WFE_DOC_EDIT_OBTAIN event triggering.", ex);
            throw new WorkflowException("failed to obtain editable instance", ex);
        }
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException {
        log.info("commit editable instance of document ");
        checkAllowedOperation("commitEditableInstance", "Commit editable instance operation not allowed");
        try {
            SCXMLUtils.triggerSignalEvents(scxmlExecutor, DocumentEvents.WFE_DOC_EDIT_COMMIT);
            return eventResultToUserDocument(scxmlExecutor.getRootContext().get("eventResult"));
        } catch (ModelException e) {
            log.error("Error in WFE_DOC_EDIT_COMMIT event triggering.", e);
            throw new WorkflowException("Error in WFE_DOC_EDIT_COMMIT event triggering.", e);
        } catch (RepositoryException ex) {
            log.error("Error in WFE_DOC_EDIT_COMMIT event triggering.", ex);
            throw new WorkflowException("failed to commit editable instance", ex);
        }
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException {
        log.info("dispose editable instance on document ");
        checkAllowedOperation("disposeEditableInstance", "Dispose editable instance operation not allowed");
        try {
            SCXMLUtils.triggerSignalEvents(scxmlExecutor, DocumentEvents.WFE_DOC_EDIT_DISPOSE);
            return eventResultToUserDocument(scxmlExecutor.getRootContext().get("eventResult"));
        } catch (ModelException me) {
            log.error("Error in WFE_DOC_EDIT_DISPOSE event triggering.", me);
            throw new WorkflowException("Error in WFE_DOC_EDIT_DISPOSE event triggering.", me);
        } catch (RepositoryException ex) {
            log.error("Error in WFE_DOC_EDIT_DISPOSE event triggering.", ex);
            throw new WorkflowException("failed to dispose editable instance", ex);
        }
    }

    @Override
    public void requestDeletion() throws WorkflowException {
        log.info("deletion request on document ");
        throw new WorkflowException("unsupported");
    }

    @Override
    public void requestPublication() throws WorkflowException {
        log.info("publication request on document ");
        throw new WorkflowException("unsupported");
    }

    @Override
    public void requestDepublication() throws WorkflowException {
        log.info("depublication request on document ");
        throw new WorkflowException("unsupported");
    }

    @Override
    public void requestPublication(Date publicationDate) throws WorkflowException {
        log.info("publication request on document ");
        throw new WorkflowException("unsupported");
    }

    @Override
    public void requestPublication(Date publicationDate, Date depublicationDate) throws WorkflowException {
        log.info("publication request on document ");
        throw new WorkflowException("unsupported");
    }

    @Override
    public void requestDepublication(Date depublicationDate) throws WorkflowException {
        log.info("depublication request on document ");
        throw new WorkflowException("unsupported");
    }

    @Override
    public void publish() throws WorkflowException {
        log.info("publication on document ");
        checkAllowedOperation("publish", "Publish operation not allowed");
        try {
            SCXMLUtils.triggerSignalEvents(scxmlExecutor, DocumentEvents.WFE_DOC_PUBLISH);
        } catch (ModelException me) {
            log.error("Error in WFE_DOC_PUBLISH event triggering.", me);
            throw new WorkflowException("Error in WFE_DOC_PUBLISH event triggering.", me);
        }
    }

    @Override
    public void depublish() throws WorkflowException {
        log.info("depublication on document ");
        checkAllowedOperation("depublish", "Depublish operation not allowed");
        try {
            SCXMLUtils.triggerSignalEvents(scxmlExecutor, DocumentEvents.WFE_DOC_DEPUBLISH);
        } catch (ModelException me) {
            log.error("Error in WFE_DOC_DEPUBLISH event triggering.", me);
            throw new WorkflowException("Error in WFE_DOC_DEPUBLISH event triggering.", me);
        }
    }

    @Override
    public void publish(Date publicationDate, Date depublicationDate) throws WorkflowException {
        log.info("publication on document ");
        throw new WorkflowException("unsupported");
    }

    @Override
    public void publish(Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        log.info("schedule publication on document ");
        checkAllowedOperation("publish", "Publish operation not allowed");
        try {
            SCXMLUtils.triggerSignalEventWithPayload(scxmlExecutor, DocumentEvents.WFE_DOC_SCHED_PUBLISH, publicationDate);
        } catch (ModelException me) {
            log.error("Error in WFE_DOC_SCHED_PUBLISH event triggering.", me);
            throw new WorkflowException("Error in WFE_DOC_SCHED_PUBLISH event triggering.", me);
        }
    }

    @Override
    public void depublish(Date depublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        log.info("schedule depublication on document ");
        checkAllowedOperation("depublish", "Depublish operation not allowed");
        try {
            SCXMLUtils.triggerSignalEventWithPayload(scxmlExecutor, DocumentEvents.WFE_DOC_SCHED_DEPUBLISH, depublicationDate);
        } catch (ModelException me) {
            log.error("Error in WFE_DOC_SCHED_DEPUBLISH event triggering.", me);
            throw new WorkflowException("Error in WFE_DOC_SCHED_DEPUBLISH event triggering.", me);
        }
    }

    @Override
    public void delete() throws WorkflowException {
        log.info("deletion of document ");
        checkAllowedOperation("delete", "Delete operation not allowed");
        try {
            SCXMLUtils.triggerSignalEvents(scxmlExecutor, DocumentEvents.WFE_DOC_DELETE);
        } catch (ModelException me) {
            log.error("Error in WFE_DOC_DELETE event triggering.", me);
            throw new WorkflowException("Error in WFE_DOC_DELETE event triggering.", me);
        }
    }

    @Override
    public void copy(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        log.info("copy document ");
        checkAllowedOperation("copy", "Copy operation not allowed");
        try {
            SCXMLUtils.triggerSignalEventWithPayload(scxmlExecutor, DocumentEvents.WFE_DOC_COPY, new DocumentCopyMovePayload(destination, newName));
        } catch (ModelException me) {
            log.error("Error in WFE_DOC_COPY event triggering.", me);
            throw new WorkflowException("Error in WFE_DOC_COPY event triggering.", me);
        }
    }

    @Override
    public void move(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        log.info("move document");
        checkAllowedOperation("move", "Move operation not allowed");
        try {
            SCXMLUtils.triggerSignalEventWithPayload(scxmlExecutor, DocumentEvents.WFE_DOC_MOVE, new DocumentCopyMovePayload(destination, newName));
        } catch (ModelException me) {
            log.error("Error in WFE_DOC_MOVE event triggering.", me);
            throw new WorkflowException("Error in WFE_DOC_MOVE event triggering.", me);
        }
    }

    @Override
    public void rename(String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        log.info("rename document ");
        checkAllowedOperation("rename", "Rename operation not allowed");
        try {
            SCXMLUtils.triggerSignalEventWithPayload(scxmlExecutor, DocumentEvents.WFE_DOC_RENAME, newName);
        } catch (ModelException me) {
            log.error("Error in WFE_DOC_RENAME event triggering.", me);
            throw new WorkflowException("Error in WFE_DOC_RENAME event triggering.", me);
        }
    }
}
