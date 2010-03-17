/*
 *  Copyright 2008 Hippo.
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

import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An editor that takes a hippo:handle for its JcrNodeModel and displays one of the variants.
 * The variant documents must be of type hippostd:publishable.
 * <p>
 * Algorithm to determine what is shown:
 * <code>
 * when draft exists:
 *   show draft in edit mode
 * else:
 *   when unpublished exists:
 *     show unpublished in preview mode
 *   else
 *     show published in preview mode
 * </code>
 * <p>
 * The editor model is the variant that is shown.
 */
class HippostdPublishableEditor extends AbstractCmsEditor<Node> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(HippostdPublishableEditor.class);

    static class WorkflowState {
        IModel<Node> draft;
        IModel<Node> unpublished;
        IModel<Node> published;
        boolean isHolder;
    }

    private IModel<Node> editorModel;

    HippostdPublishableEditor(IEditorContext manager, IPluginContext context, IPluginConfig config, IModel<Node> model)
            throws EditorException {
        super(manager, context, config, model, getMode(model));
    }

    @Override
    protected IModel<Node> getEditorModel() throws EditorException {
        WorkflowState state = getWorkflowState(super.getEditorModel());
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
            return state.published;
        default:
            if (state.unpublished == null || state.published == null) {
                throw new EditorException("Can only compare when both unpublished and published are present");
            }
            return state.unpublished;
        }
    }

    @Override
    protected IModel<Node> getBaseModel() throws EditorException {
        if (getMode() != Mode.COMPARE) {
            return super.getBaseModel();
        }
        WorkflowState state = getWorkflowState(super.getEditorModel());
        return state.published;
    }
    
    @Override
    public void setMode(Mode mode) throws EditorException {
        IModel<Node> editorModel = getEditorModel();
        if (mode != getMode() && editorModel != null) {
            WorkflowManager wflMgr = ((UserSession) Session.get()).getWorkflowManager();
            try {
                Workflow workflow = wflMgr.getWorkflow("default", editorModel.getObject());
                if (workflow instanceof EditableWorkflow) {
                    Map<IEditorFilter, Object> contexts = preClose();

                    stop();

                    postClose(contexts);

                    try {
                        if (mode == Mode.EDIT || getMode() == Mode.EDIT) {
                            switch (mode) {
                            case EDIT:
                                ((EditableWorkflow) workflow).obtainEditableInstance();
                                break;
                            case VIEW:
                            case COMPARE:
                                ((EditableWorkflow) workflow).commitEditableInstance();
                                break;
                            }
                        }
                        super.setMode(mode);

                    } finally {
                        start();
                    }
                } else {
                    throw new EditorException("No editable workflow available");
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

    @Override
    protected void start() throws EditorException {
        super.start();
        editorModel = getEditorModel();
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

    static Mode getMode(IModel<Node> nodeModel) throws EditorException {
        WorkflowState wfState = getWorkflowState(nodeModel);

        // select draft if it exists
        if (wfState.draft != null && wfState.isHolder) {
            return Mode.EDIT;
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

    static WorkflowState getWorkflowState(IModel<Node> handle) throws EditorException {
        WorkflowState wfState = new WorkflowState();
        try {
            String user = ((UserSession) Session.get()).getJcrSession().getUserID();
            Node handleNode = handle.getObject();
            if (!handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                throw new EditorException("Invalid node, not of type " + HippoNodeType.NT_HANDLE);
            }
            for (NodeIterator iter = handleNode.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                if (child.getName().equals(handleNode.getName())) {
                    if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)) {
                        String state = child.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                        if (state.equals(HippoStdNodeType.UNPUBLISHED)) {
                            wfState.unpublished = new JcrNodeModel(child);
                        } else if (state.equals(HippoStdNodeType.PUBLISHED)) {
                            wfState.published = new JcrNodeModel(child);
                        } else if (state.equals(HippoStdNodeType.DRAFT)) {
                            wfState.draft = new JcrNodeModel(child);
                            if (!child.hasProperty(HippoStdNodeType.HIPPOSTD_HOLDER)
                                    || child.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).getString().equals(user)) {
                                wfState.isHolder = true;
                            }
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            throw new EditorException("Could not determine workflow state", ex);
        }
        return wfState;
    }

    @Override
    public void detach() {
        if (editorModel != null) {
            editorModel.detach();
        }
        super.detach();
    }
}
