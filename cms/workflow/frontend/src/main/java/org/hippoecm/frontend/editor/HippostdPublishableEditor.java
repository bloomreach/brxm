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
package org.hippoecm.frontend.editor;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An editor that takes a hippo:handle for its JcrNodeModel and displays one of the variants.
 * The variant documents must be of type hippostd:publishable.
 * <p/>
 * Algorithm to determine what is shown:
 * <code>
 * when draft exists:
 *     show draft in edit mode
 * else when unpublished exists:
 *     show unpublished in preview mode
 * else
 *     show published in preview mode
 * </code>
 * <p/>
 * The editor model is the variant that is shown.
 */
public class HippostdPublishableEditor extends AbstractCmsEditor<Node> implements EventListener {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(HippostdPublishableEditor.class);

    private static class WorkflowState {
        private IModel<Node> draft;
        private IModel<Node> unpublished;
        private IModel<Node> published;
        private boolean isHolder;
        private String user;

        void process(Node child) throws RepositoryException {
            if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)) {
                String state = child.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                switch (state) {
                    case HippoStdNodeType.UNPUBLISHED:
                        unpublished = new JcrNodeModel(child);
                        break;
                    case HippoStdNodeType.PUBLISHED:
                        published = new JcrNodeModel(child);
                        break;
                    case HippoStdNodeType.DRAFT:
                        draft = new JcrNodeModel(child);
                        if (child.hasProperty(HippoStdNodeType.HIPPOSTD_HOLDER)
                                && child.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).getString().equals(user)) {
                            isHolder = true;
                        }
                        break;
                }
            }
        }

        void setUser(String user) {
            this.user = user;
        }

    }

    private Boolean isValid = null;
    private boolean modified = false;
    private IModel<Node> editorModel;

    public HippostdPublishableEditor(IEditorContext manager, IPluginContext context, IPluginConfig config, IModel<Node> model)
            throws EditorException {
        super(manager, context, config, model, getMode(model));
    }

    @Override
    protected IModel<Node> getEditorModel() throws EditorException {
        final IModel<Node> model = super.getEditorModel();
        Node node = model.getObject();
        boolean compareToVersion = false;
        try {
            if (node.isNodeType(JcrConstants.NT_VERSION)) {
                node = getVersionHandle(node);
                compareToVersion = true;
            }
        } catch (RepositoryException ex) {
            throw new EditorException("Error locating editor model", ex);
        }
        WorkflowState state = getWorkflowState(node);
        switch (getMode()) {
            case EDIT:
                if (state.draft == null || !state.isHolder) {
                    throw new EditorException("No draft present for editing");
                }
                return state.draft;
            case VIEW:
                if (state.unpublished != null) {
                    return state.unpublished;
                }
                if (state.published != null) {
                    return state.published;
                }
                return state.draft;
            default:
                if (state.draft != null) {
                    return state.draft;
                }
                if (!compareToVersion && (state.unpublished == null || state.published == null)) {
                    throw new EditorException("Can only compare when both unpublished and published are present");
                }
                return state.unpublished;
        }
    }

    @Override
    protected IModel<Node> getBaseModel() throws EditorException {
        Node node = super.getEditorModel().getObject();
        try {
            if (node.isNodeType(JcrConstants.NT_VERSION)) {
                return new JcrNodeModel(node.getNode(JcrConstants.JCR_FROZEN_NODE));
            }
        } catch (RepositoryException ex) {
            throw new EditorException("Error locating base revision", ex);
        }

        final WorkflowState state = getWorkflowState(node);
        switch (getMode()) {
            case EDIT:
                throw new EditorException("Base model is not supported in edit mode");
            default:
                if (state.published != null) {
                    return state.published;
                }
                if (state.unpublished != null) {
                    return state.unpublished;
                }
                return super.getBaseModel();
        }
    }

    @Override
    public void setMode(Mode mode) throws EditorException {
        IModel<Node> editorModel = getEditorModel();
        if (mode != getMode() && editorModel != null) {
            try {
                EditableWorkflow workflow = getEditableWorkflow();
                Map<IEditorFilter, Object> contexts = preClose();

                stop();

                postClose(contexts);

                try {
                    if (mode == Mode.EDIT || getMode() == Mode.EDIT) {
                        switch (mode) {
                            case EDIT:
                                workflow.obtainEditableInstance();
                                break;
                            case VIEW:
                            case COMPARE:
                                workflow.commitEditableInstance();
                                break;
                        }
                    }
                    super.setMode(mode);

                } finally {
                    start();
                }
            } catch (MappingException e) {
                throw new EditorException("Workflow configuration error when setting editor mode", e);
            } catch (RepositoryException e) {
                throw new EditorException("Repository error when setting editor mode", e);
            } catch (RemoteException e) {
                throw new EditorException("Connection failure when setting editor mode", e);
            } catch (WorkflowException e) {
                throw new EditorException("Workflow error when setting editor mode", e);
            }
        }
    }

    public void onEvent(EventIterator events) {
        this.modified = true;
    }

    public boolean isModified() {
        String path = "<unknown>";
        try {
            Node documentNode = getEditorModel().getObject();
            path = documentNode.getPath();

            HippoSession session = (HippoSession) documentNode.getSession();
            if (!this.modified) {
                return session.pendingChanges(documentNode, JcrConstants.NT_BASE, true).hasNext();
            } else {
                EditableWorkflow workflow = getEditableWorkflow();
                Map<String,Serializable> hints = workflow.hints();
                if (hints.containsKey("checkModified") && Boolean.TRUE.equals(hints.get("checkModified"))) {
                    modified = workflow.isModified();
                    return modified;
                } else {
                    modified = true;
                    return true;
                }
            }
        } catch (EditorException | RepositoryException |RemoteException | WorkflowException e) {
            log.error("Could not determine whether there are pending changes for '" + path + "'", e);
        }
        return false;
    }

    private EditableWorkflow getEditableWorkflow() throws RepositoryException {
        Node handleNode = getModel().getObject();
        if (handleNode == null) {
            throw new RepositoryException("No handle node available");
        }
        HippoSession session = (HippoSession) handleNode.getSession();
        WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        final Workflow workflow = manager.getWorkflow("editing", handleNode);
        if (!(workflow instanceof EditableWorkflow)) {
            throw new RepositoryException("Editing workflow not of type EditableWorkflow");
        }
        return (EditableWorkflow) workflow;
    }

    public boolean isValid() throws EditorException {
        if (isValid == null) {
            try {
                validate();
            } catch (ValidationException e) {
                throw new EditorException("Unable to validate the document", e);
            }
        }
        return this.isValid;
    }

    /**
     * Saves the document, and keeps the editor in its current mode (EDIT).
     *
     * @throws EditorException Unable to save the document.
     */
    public void save() throws EditorException {
        UserSession session = UserSession.get();
        String docPath = null;

        try {
            Node documentNode = getEditorModel().getObject();
            docPath = documentNode.getPath();

            if (isValid == null) {
                validate();
            }
            if (isValid) {
                EditableWorkflow workflow = getEditableWorkflow();
                workflow.commitEditableInstance();
                session.getJcrSession().refresh(true);
                workflow.obtainEditableInstance();
                modified = false;
            } else {
                throw new EditorException("The document is not valid");
            }
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.error("Unable to save the document {}: {}", docPath, e.getMessage());
            throw new EditorException("Unable to save the document", e);
        } catch (ValidationException e) {
            log.error("Unable to validate the document {}: {}", docPath, e.getMessage());
            throw new EditorException("Unable to validate the document", e);
        }

    }

    public void revert() throws EditorException {
        try {
            UserSession session = UserSession.get();

            WorkflowManager manager = session.getWorkflowManager();
            Node docNode = getEditorModel().getObject();
            Node handleNode = getModel().getObject();

            handleNode.refresh(false);

            NodeIterator docs = handleNode.getNodes(handleNode.getName());
            if (docs.hasNext()) {
                Node sibling = docs.nextNode();
                if (sibling.isSame(docNode)) {
                    if (!docs.hasNext()) {
                        Document folder = new Document(handleNode.getParent());
                        Workflow workflow = manager.getWorkflow("internal", folder);
                        if (workflow instanceof FolderWorkflow) {
                            ((FolderWorkflow) workflow).delete(new Document(docNode));
                        } else {
                            log.warn("cannot delete document which is not contained in a folder");
                        }

                        setMode(Mode.EDIT);
                        this.modified = false;
                        return;
                    }
                }
            } else {
                log.warn("No documents found under handle of edited document");
            }

            handleNode.getSession().refresh(true);
            getEditableWorkflow().disposeEditableInstance();
            session.getJcrSession().refresh(true);

        } catch (RepositoryException | RemoteException | WorkflowException ex) {
            log.error("failure while reverting", ex);
        }

    }

    public void done() throws EditorException {
        UserSession session = UserSession.get();
        String docPath = null;
        try {
            Node documentNode = getEditorModel().getObject();
            docPath = documentNode.getPath();

            if (isValid == null) {
                validate();
            }
            if (isValid) {
                final javax.jcr.Session jcrSession = session.getJcrSession();
                jcrSession.refresh(true);
                jcrSession.save();

                EditableWorkflow workflow = getEditableWorkflow();
                workflow.commitEditableInstance();
                jcrSession.refresh(false);
                this.modified = false;
            } else {
                throw new EditorException("The document is not valid");
            }

        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.error("Unable to save the document {}: {}", docPath, e.getMessage());
            throw new EditorException("Unable to save the document", e);
        } catch (ValidationException e) {
            log.error("Unable to validate the document {}: {}", docPath, e.getMessage());
            throw new EditorException("Unable to validate the document", e);
        }
    }


    public void discard() throws EditorException {
        try {
            UserSession session = UserSession.get();

            WorkflowManager manager = session.getWorkflowManager();
            Node docNode = getEditorModel().getObject();
            Node handleNode = getModel().getObject();

            handleNode.refresh(false);

            NodeIterator docs = handleNode.getNodes(handleNode.getName());
            if (docs.hasNext()) {
                Node sibling = docs.nextNode();
                if (sibling.isSame(docNode)) {
                    if (!docs.hasNext()) {
                        Document folder = new Document(handleNode.getParent());
                        Workflow workflow = manager.getWorkflow("internal", folder);
                        if (workflow instanceof FolderWorkflow) {
                            ((FolderWorkflow) workflow).delete(new Document(docNode));
                        } else {
                            log.warn("cannot delete document which is not contained in a folder");
                        }
                        return;
                    }
                }
            } else {
                log.warn("No documents found under handle of edited document");
            }

            handleNode.getSession().refresh(true);
            ((EditableWorkflow) manager.getWorkflow("editing", handleNode)).disposeEditableInstance();
            session.getJcrSession().refresh(true);
            this.modified = false;

        } catch (RepositoryException | WorkflowException | RemoteException ex) {
            log.error("failure while reverting", ex);
        }

    }

    @Override
    public void start() throws EditorException {
        super.start();
        editorModel = getEditorModel();
        if (getMode() == Mode.EDIT) {
            try {
                UserSession session = UserSession.get();
                session.getObservationManager().addEventListener(this,
                        Event.NODE_ADDED | Event.NODE_MOVED | Event.NODE_REMOVED |
                                Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                        editorModel.getObject().getPath(), true, null, null, false);
            } catch (RepositoryException e) {
                throw new EditorException(e);
            }

        }
    }


    void validate() throws ValidationException {
        isValid = true;

        List<IValidationService> validators = getPluginContext().getServices(
                getClusterConfig().getString(IValidationService.VALIDATE_ID), IValidationService.class);
        if (validators != null) {
            for (IValidationService validator : validators) {
                validator.validate();
                IValidationResult result = validator.getValidationResult();
                isValid = isValid && result.isValid();
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (getMode() == Mode.EDIT) {
            try {
                UserSession session = UserSession.get();
                session.getObservationManager().removeEventListener(this);
                modified = false;
            } catch (RepositoryException e) {
                log.warn("Unable to remove listener", e);
            }
        }
    }


    @Override
    public void refresh() {
        final IModel<Node> handle = getModel();

        // verify that the handle exists
        if (handle.getObject() == null) {
            try {
                close();
            } catch (EditorException ex) {
                log.error("Could not close editor for removed handle");
            }
            return;
        }

        // verify that a document exists, i.e. the document has not been deleted
        try {
            Mode newMode = getMode(handle);
            if (newMode != super.getMode()) {
                super.setMode(newMode);
            } else {
                IModel<Node> newModel = getEditorModel();
                if (!newModel.equals(editorModel)) {
                    stop();
                    start();
                }
            }
        } catch (EditorException ex) {
            try {
                close();
            } catch (EditorException ex2) {
                log.error("Could not close editor for empty handle");
            }
        }
    }

    @Override
    public void detach() {
        isValid = null;
        if (editorModel != null) {
            editorModel.detach();
        }
        super.detach();
    }

    static Mode getMode(IModel<Node> nodeModel) throws EditorException {
        Node node = nodeModel.getObject();
        try {
            if (node.isNodeType(JcrConstants.NT_VERSION)) {
                Node frozen = node.getNode(JcrConstants.JCR_FROZEN_NODE);
                String uuid = frozen.getProperty(JcrConstants.JCR_FROZEN_UUID).getString();
                try {
                    node.getSession().getNodeByIdentifier(uuid);
                    return Mode.COMPARE;
                } catch (ItemNotFoundException ex) {
                    return Mode.VIEW;
                }
            }
        } catch (RepositoryException e) {
            throw new EditorException("Could not determine mode", e);
        }
        WorkflowState wfState = getWorkflowState(nodeModel.getObject());

        // select draft if it exists
        if (wfState.draft != null) {
            if (wfState.isHolder) {
                return Mode.EDIT;
            } else {
                if (wfState.published != null) {
                    return Mode.COMPARE;
                }
                return Mode.VIEW;
            }
        }

        // show preview
        if (wfState.unpublished == null && wfState.published == null) {
            throw new EditorException("unable to find draft or unpublished variants");
        }

        if (wfState.unpublished != null && wfState.published != null) {
            return Mode.COMPARE;
        }
        return Mode.VIEW;
    }

    static WorkflowState getWorkflowState(Node handleNode) throws EditorException {
        WorkflowState wfState = new WorkflowState();
        try {
            String user = UserSession.get().getJcrSession().getUserID();
            wfState.setUser(user);
            if (!handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                throw new EditorException("Invalid node, not of type " + HippoNodeType.NT_HANDLE);
            }
            for (NodeIterator iter = handleNode.getNodes(); iter.hasNext(); ) {
                Node child = iter.nextNode();
                if (child.getName().equals(handleNode.getName())) {
                    wfState.process(child);
                }
            }
        } catch (RepositoryException ex) {
            throw new EditorException("Could not determine workflow state", ex);
        }
        return wfState;
    }

    static Node getVersionHandle(Node versionNode) throws EditorException {
        try {
            final Node frozenNode = versionNode.getNode(JcrConstants.JCR_FROZEN_NODE);
            String uuid = frozenNode.getProperty(JcrConstants.JCR_FROZEN_UUID).getString();
            Node variant = versionNode.getSession().getNodeByIdentifier(uuid);
            return variant.getParent();
        } catch (RepositoryException ex) {
            throw new EditorException("Failed to build version information", ex);
        }
    }

}
