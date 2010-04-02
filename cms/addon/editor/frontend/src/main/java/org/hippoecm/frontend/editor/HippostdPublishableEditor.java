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
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

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
        private String user;

        void process(Node child) throws RepositoryException, ValueFormatException, PathNotFoundException {
            if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)) {
                String state = child.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                if (state.equals(HippoStdNodeType.UNPUBLISHED)) {
                    unpublished = new JcrNodeModel(child);
                } else if (state.equals(HippoStdNodeType.PUBLISHED)) {
                    published = new JcrNodeModel(child);
                } else if (state.equals(HippoStdNodeType.DRAFT)) {
                    draft = new JcrNodeModel(child);
                    if (!child.hasProperty(HippoStdNodeType.HIPPOSTD_HOLDER)
                            || child.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).getString().equals(user)) {
                        isHolder = true;
                    }
                }
            }
        }

        void setUser(String user) {
            this.user = user;
        }

    }

    static class VersionState {
        IModel<Node> version;
        IModel<Node> current;
    }

    private IModel<Node> editorModel;

    HippostdPublishableEditor(IEditorContext manager, IPluginContext context, IPluginConfig config, IModel<Node> model)
            throws EditorException {
        super(manager, context, config, model, getMode(model));
    }

    @Override
    protected IModel<Node> getEditorModel() throws EditorException {
        Node node = super.getEditorModel().getObject();
        if (getMode() == Mode.COMPARE) {
            try {
                if (node.isNodeType("nt:version")) {
                    VersionState vs = getVersionState(node);
                    if (vs.current != null) {
                        return vs.current;
                    } else if (vs.version != null) {
                        return vs.version;
                    } else {
                        throw new EditorException("No current version found");
                    }
                }
            } catch (RepositoryException ex) {
                throw new EditorException("Error locating editor model", ex);
            }
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
        Node node = super.getEditorModel().getObject();
        try {
            if (node.isNodeType("nt:version")) {
                VersionState vs = getVersionState(node);
                if (vs.version != null) {
                    return vs.version;
                } else {
                    throw new EditorException("No base version found");
                }
            }
        } catch (RepositoryException ex) {
            throw new EditorException("Error locating base revision", ex);
        }
        WorkflowState state = getWorkflowState(node);
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
        Node node = nodeModel.getObject();
        try {
            if (node.isNodeType("nt:version")) {
                Node frozen = node.getNode("jcr:frozenNode");
                String uuid = frozen.getProperty("jcr:frozenUuid").getString();
                try {
                    Node handle = node.getSession().getNodeByUUID(uuid);
                    if (handle.hasNode(handle.getName())) {
                        return Mode.COMPARE;
                    } else {
                        throw new EditorException("Cannot display deleted document revision");
                    }
                } catch (ItemNotFoundException ex) {
                    return Mode.VIEW;
                }
            }
        } catch (RepositoryException e) {
            throw new EditorException("Could not determine mode", e);
        }
        WorkflowState wfState = getWorkflowState(nodeModel.getObject());

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

    static WorkflowState getWorkflowState(Node handleNode) throws EditorException {
        WorkflowState wfState = new WorkflowState();
        try {
            String user = ((UserSession) Session.get()).getJcrSession().getUserID();
            wfState.setUser(user);
            if (!handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                throw new EditorException("Invalid node, not of type " + HippoNodeType.NT_HANDLE);
            }
            for (NodeIterator iter = handleNode.getNodes(); iter.hasNext();) {
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

    static VersionState getVersionState(Node versionNode) throws EditorException {
        VersionState vs = new VersionState();
        try {
            WorkflowState baseState = new WorkflowState();
            Set<Node> variants = EditorFactory.getDocuments(versionNode);
            for (Node variant : variants) {
                baseState.process(variant.getNode("jcr:frozenNode"));
            }
            if (baseState.unpublished != null) {
                vs.version = baseState.unpublished;
            } else {
                vs.version = baseState.published;
            }
            String uuid = versionNode.getNode("jcr:frozenNode").getProperty("jcr:frozenUuid").getString();
            Node handle = versionNode.getSession().getNodeByUUID(uuid);
            WorkflowState currentState = getWorkflowState(handle);
            if (currentState.draft != null) {
                vs.current = currentState.draft;
            } else if (currentState.unpublished != null) {
                vs.current = currentState.unpublished;
            } else {
                vs.current = currentState.published;
            }
            return vs;
        } catch (RepositoryException ex) {
            throw new EditorException("Failed to build version information");
        }
    }

    @Override
    public void detach() {
        if (editorModel != null) {
            editorModel.detach();
        }
        super.detach();
    }
}
