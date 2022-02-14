/*
 * Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.editor;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.BranchIdModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.usagestatistics.UsageEvent;
import org.hippoecm.frontend.usagestatistics.events.DocumentUsageEvent;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_BRANCHES_PROPERTY;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

/**
 * An editor that takes a hippo:handle for its JcrNodeModel and displays one of the variants.
 * The variant documents must be of type hippostd:publishable.
 * <p>
 * Algorithm to determine what is shown:
 * <code>
 * when draft exists:
 * show draft in edit mode
 * else when unpublished exists:
 * show unpublished in preview mode
 * else
 * show published in preview mode
 * </code>
 * </p>
 * The editor model is the variant that is shown.
 */
public class HippostdPublishableEditor extends AbstractCmsEditor<Node> implements EventListener {
    private static final Logger log = LoggerFactory.getLogger(HippostdPublishableEditor.class);

    private static final String UNABLE_TO_VALIDATE_THE_DOCUMENT = "Unable to validate the document";

    private BranchIdModel branchIdModel;
    private Boolean isValid;
    private boolean modified;
    private IModel<Node> editorModel;
    private boolean transferable;

    public HippostdPublishableEditor(final IEditorContext manager, final IPluginContext context, final IPluginConfig config, final IModel<Node> model)
            throws EditorException {
        super(manager, context, config, model, getMode(model));

        try {
            branchIdModel = new BranchIdModel(context, model.getObject().getIdentifier());
        } catch (final RepositoryException e) {
            log.warn(e.getMessage(), e);
        }
    }

    static Mode getMode(final IModel<Node> nodeModel) throws EditorException {
        return getMode(nodeModel, MASTER_BRANCH_ID);
    }

    static Mode getMode(final IModel<Node> nodeModel, final String branchId) throws EditorException {
        final HippoStdPublishableEditorModel model = getEditorStateModel(branchId, nodeModel.getObject());
        return model.getMode();
    }

    @Override
    protected IModel<Node> getEditorModel() throws EditorException {
        final Node node = super.getEditorModel().getObject();
        final HippoStdPublishableEditorModel editorStateModel = getEditorStateModel(branchIdModel.getBranchId(), node);
        return new JcrNodeModel(editorStateModel.getEditor());
    }

    @Override
    protected IModel<Node> getBaseModel() throws EditorException {
        final Node node = super.getEditorModel().getObject();
        final HippoStdPublishableEditorModel editorStateModel = getEditorStateModel(branchIdModel.getBranchId(), node);
        if (editorStateModel.getBase().isEmpty()) {
            return super.getBaseModel();
        }

        return new JcrNodeModel(editorStateModel.getBase());
    }

    private static HippoStdPublishableEditorModel getEditorStateModel(final String branchId, final Node handleOrVersion)
            throws EditorException {
        return HippoPublishableEditorModelBuilder
                .build(DocumentBuilder
                        .create()
                        .branchId(branchId)
                        .node(handleOrVersion)
                        .userId(UserSession.get().getJcrSession().getUserID())
                        .build());
    }

    @Override
    public void setMode(final Mode mode) throws EditorException {
        editorModel = getEditorModel();
        if (mode != getMode() && editorModel != null) {
            try {
                final EditableWorkflow workflow = getEditableWorkflow();
                final Map<IEditorFilter, Object> contexts = preClose();

                stop();

                postClose(contexts);

                try {
                    if (executeWorkflowForMode(mode, workflow)) {
                        super.setMode(mode);
                    }
                } finally {
                    start();
                }
            } catch (final MappingException e) {
                throw new EditorException("Workflow configuration error when setting editor mode", e);
            } catch (final RepositoryException e) {
                throw new EditorException("Repository error when setting editor mode", e);
            } catch (final RemoteException e) {
                throw new EditorException("Connection failure when setting editor mode", e);
            } catch (final WorkflowException e) {
                throw new EditorException("Workflow error when setting editor mode", e);
            }
        }
    }

    private boolean executeWorkflowForMode(final Mode mode, final EditableWorkflow workflow) throws RepositoryException, RemoteException, WorkflowException {
        if (mode == Mode.EDIT || getMode() == Mode.EDIT) {
            final String branchId = branchIdModel.getBranchId();
            switch (mode) {
                case EDIT:
                    if (isFalse((Boolean) workflow.hints(branchId).get("obtainEditableInstance"))) {
                        if (isTrue((Boolean) workflow.hints(branchId).get("editDraft"))) {
                            workflow.editDraft();
                            return true;
                        }
                        return false;
                    }
                    workflow.obtainEditableInstance(branchId);
                    break;
                case VIEW:
                case COMPARE:
                    if (isFalse((Boolean) workflow.hints(branchId).get("commitEditableInstance"))) {
                        return false;
                    }
                    workflow.commitEditableInstance();
                    break;
            }
        }
        return true;
    }

    public void onEvent(final EventIterator events) {
        modified = true;
    }

    public boolean isModified() {
        String path = "<unknown>";
        try {
            final Node documentNode = getEditorModel().getObject();
            path = documentNode.getPath();

            if (!modified) {
                // Get session from handle node to make sure we get a decorated hippo session.
                final HippoSession session = (HippoSession) super.getModel().getObject().getSession();
                return session.pendingChanges(documentNode, JcrConstants.NT_BASE, true).hasNext();
            } else {
                final EditableWorkflow workflow = getEditableWorkflow();
                final Map<String, Serializable> hints = workflow.hints(branchIdModel.getBranchId());
                if (Boolean.TRUE.equals(hints.get("checkModified"))) {
                    modified = workflow.isModified();
                    return modified;
                } else {
                    modified = true;
                    return true;
                }
            }
        } catch (final EditorException | RepositoryException | RemoteException | WorkflowException e) {
            log.error("Could not determine whether there are pending changes for '{}'", path, e);
        }
        return false;
    }

    private EditableWorkflow getEditableWorkflow() throws RepositoryException {
        final Workflow workflow = getWorkflow();
        if (!(workflow instanceof EditableWorkflow)) {
            throw new RepositoryException("Editing workflow not of type EditableWorkflow");
        }
        return (EditableWorkflow) workflow;
    }

    private Workflow getWorkflow() throws RepositoryException {
        final Node handleNode = getModel().getObject();
        if (handleNode == null) {
            throw new RepositoryException("No handle node available");
        }

        final HippoSession session = (HippoSession) handleNode.getSession();
        final WorkflowManager manager = session.getWorkspace().getWorkflowManager();
        return manager.getWorkflow("editing", handleNode);
    }

    public boolean isValid() throws EditorException {
        if (isValid == null) {
            try {
                validate();
            } catch (final ValidationException e) {
                throw new EditorException(UNABLE_TO_VALIDATE_THE_DOCUMENT, e);
            }
        }
        return isValid;
    }

    @Override
    public boolean isTransferable() throws EditorException {
        return transferable;
    }

    public void saveDraft() throws EditorException {
        try {
            final DocumentWorkflow wf = (DocumentWorkflow) getEditableWorkflow();
            wf.saveDraft();
            final Map<String, Serializable> hints = wf.hints();
            final Boolean transferable = (Boolean) hints.get("transferable");
            this.transferable = transferable == null ? false : transferable;
        } catch (final RepositoryException | WorkflowException | RemoteException e) {
            throw new EditorException("Error during saving draft");
        }
    }

    /**
     * Saves the document, and keeps the editor in its current mode (EDIT).
     *
     * @throws EditorException Unable to save the document.
     */
    public void save() throws EditorException {
        String docPath = null;

        try {
            final Node documentNode = getEditorModel().getObject();
            docPath = documentNode.getPath();

            if (isValid == null) {
                validate();
            }

            if (!isValid) {
                throw new EditorException("The document is not valid");
            }

            final EditableWorkflow workflow = getEditableWorkflow();
            workflow.commitEditableInstance();

            final UserSession session = UserSession.get();
            session.getJcrSession().refresh(true);

            workflow.obtainEditableInstance(branchIdModel.getBranchId());
            modified = false;
        } catch (final RepositoryException | WorkflowException | RemoteException e) {
            log.error("Unable to save the document {}: {}", docPath, e.getMessage());
            throw new EditorException("Unable to save the document", e);
        } catch (final ValidationException e) {
            log.error("Unable to validate the document {}: {}", docPath, e.getMessage());
            throw new EditorException(UNABLE_TO_VALIDATE_THE_DOCUMENT, e);
        }
    }

    public void revert() throws EditorException {
        try {
            final UserSession session = UserSession.get();
            final WorkflowManager manager = session.getWorkflowManager();
            final Node handleNode = getModel().getObject();

            handleNode.refresh(false);

            final NodeIterator docs = handleNode.getNodes(handleNode.getName());
            if (docs.hasNext()) {
                final Node docNode = getEditorModel().getObject();
                final Node sibling = docs.nextNode();
                if (sibling.isSame(docNode) && !docs.hasNext()) {
                    final Document folder = new Document(handleNode.getParent());
                    final Workflow workflow = manager.getWorkflow("internal", folder);
                    if (workflow instanceof FolderWorkflow) {
                        ((FolderWorkflow) workflow).delete(new Document(docNode));
                    } else {
                        log.warn("cannot delete document which is not contained in a folder");
                    }

                    setMode(Mode.EDIT);
                    modified = false;
                    return;
                }
            } else {
                log.warn("No documents found under handle of edited document");
            }

            handleNode.getSession().refresh(true);
            getEditableWorkflow().disposeEditableInstance();
            session.getJcrSession().refresh(true);

        } catch (final RepositoryException | RemoteException | WorkflowException ex) {
            log.error("failure while reverting", ex);
        }

    }

    public void done() throws EditorException {
        String docPath = null;
        try {
            final Node documentNode = getEditorModel().getObject();
            docPath = documentNode.getPath();

            if (isValid == null) {
                validate();
            }

            if (!isValid) {
                throw new EditorException("The document is not valid");
            }

            final UserSession session = UserSession.get();
            final javax.jcr.Session jcrSession = session.getJcrSession();
            jcrSession.refresh(true);
            jcrSession.save();

            final EditableWorkflow workflow = getEditableWorkflow();
            workflow.commitEditableInstance();
            jcrSession.refresh(false);
            modified = false;
        } catch (final RepositoryException | WorkflowException | RemoteException e) {
            log.error("Unable to save the document {}: {}", docPath, e.getMessage());
            throw new EditorException("Unable to save the document", e);
        } catch (final ValidationException e) {
            log.error("Unable to validate the document {}: {}", docPath, e.getMessage());
            throw new EditorException(UNABLE_TO_VALIDATE_THE_DOCUMENT, e);
        }
    }

    public void discard() throws EditorException {
        try {
            final UserSession session = UserSession.get();
            final WorkflowManager manager = session.getWorkflowManager();
            final Node handleNode = getModel().getObject();

            handleNode.refresh(false);

            final NodeIterator docs = handleNode.getNodes(handleNode.getName());
            if (docs.hasNext()) {
                final Node docNode = getEditorModel().getObject();
                final Node sibling = docs.nextNode();
                if (sibling.isSame(docNode) && (!docs.hasNext() && getMode() == Mode.EDIT)) {
                    final Document folder = new Document(handleNode.getParent());
                    final Workflow workflow = manager.getWorkflow("internal", folder);
                    if (workflow instanceof FolderWorkflow) {
                        ((FolderWorkflow) workflow).delete(new Document(docNode));
                    } else {
                        log.warn("cannot delete document which is not contained in a folder");
                    }
                    return;
                }
            } else {
                log.warn("No documents found under handle of edited document");
            }

            handleNode.getSession().refresh(true);
            if (getMode() == Mode.EDIT) {
                ((EditableWorkflow) manager.getWorkflow("editing", handleNode)).disposeEditableInstance();
            }
            session.getJcrSession().refresh(true);
            modified = false;
        } catch (RepositoryException | WorkflowException | RemoteException ex) {
            log.error("failure while reverting", ex);
        }
    }

    public void close() throws EditorException {
        super.close();
        if (branchIdModel != null) {
            branchIdModel.destroy();
        }
    }

    @Override
    public void start() throws EditorException {
        super.start();

        editorModel = getEditorModel();

        if (getMode() == Mode.EDIT) {
            try {
                final UserSession session = UserSession.get();
                session.getObservationManager().addEventListener(this,
                        Event.NODE_ADDED | Event.NODE_MOVED | Event.NODE_REMOVED |
                        Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                        editorModel.getObject().getPath(), true, null, null, false);
            } catch (final RepositoryException e) {
                throw new EditorException(e);
            }
        }
    }

    void validate() throws ValidationException {
        isValid = true;

        final List<IValidationService> validators = getPluginContext().getServices(
                getClusterConfig().getString(IValidationService.VALIDATE_ID), IValidationService.class);
        if (validators != null) {
            for (final IValidationService validator : validators) {
                validator.validate();
                final IValidationResult result = validator.getValidationResult();
                isValid = isValid && result.isValid();
            }
        }
    }

    @Override
    public void stop() {
        super.stop();

        if (getMode() == Mode.EDIT) {
            try {
                final UserSession session = UserSession.get();
                session.getObservationManager().removeEventListener(this);
                modified = false;
            } catch (final RepositoryException e) {
                log.warn("Unable to remove listener", e);
            }
        }
    }

    @Override
    public void refresh() {
        final IModel<Node> handleModel = getModel();
        final Node handle = handleModel.getObject();

        if (handle == null) {
            try {
                close();
            } catch (final EditorException e) {
                log.warn("Could not close editor for removed handle");
            }
            return;
        }

        try {
            if (isBranchDeleted(handle)) {
                close();
            }
        } catch (final EditorException e) {
            log.warn("Could not close editor for deleted branch");
            return;
        }

        // verify that a document exists, i.e. the document has not been deleted
        try {
            final Mode newMode = getMode(handleModel, branchIdModel.getBranchId());
            if (newMode != super.getMode()) {
                super.setMode(newMode);
            } else {
                final IModel<Node> oldModel = editorModel;
                final IModel<Node> newModel = getEditorModel();
                if (!newModel.equals(oldModel)) {
                    stop();
                    start();
                }
            }
        } catch (final EditorException ex) {
            try {
                close();
            } catch (final EditorException ex2) {
                log.error("Could not close editor for empty handle");
            }
        }
    }

    private boolean isBranchDeleted(final Node handle) throws EditorException {
        final String branchId = branchIdModel.getBranchId();
        if (branchId.equals(MASTER_BRANCH_ID)) {
            // The master branch can never be deleted, so we can immediately return false.
            return false;
        }
        try {
            final String[] existingBranches = getMultipleStringProperty(handle, HIPPO_BRANCHES_PROPERTY, new String[0]);
            return Stream.of(existingBranches).noneMatch(existingBranchId -> existingBranchId.equals(branchId));
        } catch (final RepositoryException e) {
            throw new EditorException(e);
        }
    }

    @Override
    public void detach() {
        isValid = null;
        if (editorModel != null) {
            editorModel.detach();
        }
        if (branchIdModel != null) {
            branchIdModel.detach();
        }
        super.detach();
    }

    @Override
    protected UsageEvent createUsageEvent(final String name, final IModel<Node> model) {
        return new DocumentUsageEvent(name, model, "publishable-editor");
    }
}
