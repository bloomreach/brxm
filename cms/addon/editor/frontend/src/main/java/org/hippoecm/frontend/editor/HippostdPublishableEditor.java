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

    private IModel<Node> editorModel;

    HippostdPublishableEditor(IEditorContext manager, IPluginContext context, IPluginConfig config,
            IModel<Node> model) throws EditorException {
        super(manager, context, config, model, getMode(model));
    }

    @Override
    protected IModel<Node> getEditorModel() {
        switch (getMode()) {
        case EDIT:
            return getDraftModel(super.getEditorModel());
        case VIEW:
        default:
            return getPreviewModel(super.getEditorModel());
        }
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
                        switch (mode) {
                        case EDIT:
                            ((EditableWorkflow) workflow).obtainEditableInstance();
                            break;
                        case VIEW:
                            ((EditableWorkflow) workflow).commitEditableInstance();
                            break;
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
        // select draft if it exists
        IModel<Node> draftDocument = getDraftModel(nodeModel);
        if (draftDocument != null) {
            return Mode.EDIT;
        }

        // show preview
        IModel<Node> previewDocument = getPreviewModel(nodeModel);
        if (previewDocument != null) {
            return Mode.VIEW;
        }

        throw new EditorException("unable to find draft or unpublished variants");
    }

    static IModel<Node> getPreviewModel(IModel<Node> handle) {
        try {
            Node handleNode = handle.getObject();
            Node published = null;
            if (handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = handleNode.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    if (child.getName().equals(handleNode.getName())) {
                        if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)) {
                            String state = child.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                            if (state.equals(HippoStdNodeType.UNPUBLISHED)) {
                                return new JcrNodeModel(child);
                            } else if (state.equals(HippoStdNodeType.PUBLISHED)) {
                                published = child;
                            }
                        } else {
                            published = child;
                        }
                    }
                }
                if (published != null) {
                    return new JcrNodeModel(published);
                }
            } else {
                return handle;
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    static IModel<Node> getDraftModel(IModel<Node> handle) {
        String user = ((UserSession) Session.get()).getJcrSession().getUserID();
        try {
            Node handleNode = handle.getObject();
            if (handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = handleNode.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    if (child.getName().equals(handleNode.getName())) {
                        if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)
                                && child.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString().equals(
                                        HippoStdNodeType.DRAFT)
                                && child.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).getString().equals(user)) {
                            return new JcrNodeModel(child);
                        }
                    }
                }
            } else {
                log.warn("Editor model is not a handle");
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public void detach() {
        if (editorModel != null) {
            editorModel.detach();
        }
        super.detach();
    }
}
