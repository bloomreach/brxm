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
package org.hippoecm.frontend.plugins.cms.edit;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorManagerPlugin implements IPlugin, IEditorManager, IObserver, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(EditorManagerPlugin.class);

    private IPluginContext context;
    private IPluginConfig config;

    private EditorFactory previewFactory;
    private EditorFactory editorFactory;

    private final IModelReference modelReference;
    private CmsEditor preview;
    private Map<JcrNodeModel, CmsEditor> editors;
    private List<JcrNodeModel> pending;
    private transient boolean active = false;

    public EditorManagerPlugin(final IPluginContext context, final IPluginConfig config) {
        this.context = context;
        this.config = config;

        editors = new HashMap<JcrNodeModel, CmsEditor>();
        pending = new LinkedList<JcrNodeModel>();
        editorFactory = new EditorFactory(this, context, config.getString("cluster.edit.name"), config
                .getPluginConfig("cluster.edit.options"));
        previewFactory = new EditorFactory(this, context, config.getString("cluster.preview.name"), config
                .getPluginConfig("cluster.preview.options"));

        // monitor document in browser 
        if (config.getString(RenderService.MODEL_ID) != null) {
            modelReference = context.getService(config.getString(RenderService.MODEL_ID), IModelReference.class);
            if (modelReference != null) {
                context.registerService(this, IObserver.class.getName());
            }
        } else {
            modelReference = null;
            log.warn("No model defined ({})", RenderService.MODEL_ID);
        }

        // register editor
        context.registerService(this, config.getString("editor.id"));
    }

    public void detach() {
    }

    public IObservable getObservable() {
        return modelReference;
    }

    public void onEvent(IEvent event) {
        if (!active) {
            active = true;
            try {
                JcrNodeModel nodeModel = (JcrNodeModel) modelReference.getModel();
                if (nodeModel != null && nodeModel.getNode() != null) {
                    // close preview when a new document is selected
                    if (preview != null) {
                        try {
                            preview.close();
                        } catch (EditorException ex) {
                            log.error("Failed to close preview", ex);
                        }
                        preview = null;
                    }

                    // find existing editor
                    Node node = nodeModel.getNode();
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        // focus existing editor, if it exists
                        for (JcrNodeModel editorModel : editors.keySet()) {
                            if (editorModel.getParentModel().equals(nodeModel)) {
                                editors.get(editorModel).focus();
                                return;
                            }
                        }

                        // FIXME: add auto-switching of active editors
                        for (JcrNodeModel pendingModel : pending) {
                            if (pendingModel.getParentModel().equals(nodeModel)) {
                                return;
                            }
                        }

                        // open editor if there is a draft
                        JcrNodeModel draftDocument = getDraftModel(nodeModel);
                        if (draftDocument != null) {
                            try {
                                CmsEditor editor = openEditor(draftDocument);
                                editor.focus();
                            } catch (ServiceException ex) {
                                log.error(ex.getMessage());
                            }
                            return;
                        }

                        // show preview
                        JcrNodeModel previewDocument = getPreviewModel(nodeModel);
                        if (previewDocument != null) {
                            try {
                                this.preview = previewFactory.newEditor(previewDocument);
                                this.preview.focus();
                            } catch (CmsEditorException ex) {
                                log.error(ex.getMessage());
                            }
                        } else {
                            log.error("No preview version found of document");
                        }
                    } else {
                        // focus existing editor, if it exists
                        if (editors.containsKey(nodeModel)) {
                            editors.get(nodeModel).focus();
                            return;
                        }

                        // FIXME: add auto-switching of active editors
                        if (pending.contains(nodeModel)) {
                            return;
                        }

                        // open editor
                        try {
                            this.preview = previewFactory.newEditor(nodeModel);
                            this.preview.focus();
                        } catch (CmsEditorException ex) {
                            log.error(ex.getMessage());
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            } finally {
                active = false;
            }
        }
    }

    public IEditor getEditor(IModel editModel) {
        if (editModel instanceof JcrNodeModel) {
            // find editor
            if (editors.containsKey(editModel)) {
                return editors.get(editModel);
            }
        } else {
            log.warn("Unknown model type", editModel);
        }
        return null;
    }

    public CmsEditor openEditor(IModel model) throws ServiceException {
        JcrNodeModel nodeModel = (JcrNodeModel) model;

        if (editors.containsKey(nodeModel) || pending.contains(nodeModel)) {
            throw new ServiceException("editor already exists");
        }

        if (editors.size() < 4) {
            try {
                // Close preview when it is
                // 1) another document below the same handle as the passed-in model
                // 2) the exact same node
                if (preview != null) {
                    JcrNodeModel previewModel = (JcrNodeModel) preview.getModel();
                    if (nodeModel.getParentModel().equals(previewModel.getParentModel())) {
                        Node node = nodeModel.getParentModel().getNode();
                        try {
                            if (node.isNodeType(HippoNodeType.NT_HANDLE) || nodeModel.equals(previewModel)) {
                                preview.close();
                            }
                        } catch (EditorException ex) {
                            log.error("Failed to close preview", ex);
                        } catch (RepositoryException ex) {
                            log.error("Unable to determine parent nodetype", ex);
                        }
                        preview = null;
                    }
                }

                CmsEditor editor = editorFactory.newEditor(nodeModel);
                editors.put(nodeModel, editor);
                editor.focus();
                return editor;
            } catch (CmsEditorException ex) {
                log.error(ex.getMessage());
                throw new ServiceException("Initialization failed", ex);
            }
        } else {
            IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
            dialogService.show(new TooManyEditorsDialog());
            pending.add(nodeModel);
            throw new ServiceException("Too many editors open");
        }
    }

    void setActiveModel(JcrNodeModel nodeModel) {
        try {
            IModelReference modelService = context.getService(config.getString(RenderService.MODEL_ID),
                    IModelReference.class);
            if (modelService != null) {
                if (nodeModel != null && nodeModel.getParentModel() != null
                        && nodeModel.getParentModel().getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                    modelService.setModel(nodeModel.getParentModel());
                } else {
                    modelService.setModel(nodeModel);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    void unregister(CmsEditor editor) {
        JcrNodeModel model = (JcrNodeModel) editor.getModel();
        if (model != null) {

            // update selected node
            if (!active) {
                active = true;
                JcrNodeModel nodeModel = (JcrNodeModel) modelReference.getModel();
                if (nodeModel != null && nodeModel.getNode() != null) {
                    try {
                        Node node = nodeModel.getNode();
                        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                            if (model.getParentModel() != null && model.getParentModel().equals(nodeModel)) {
                                setActiveModel(null);
                            }
                        } else {
                            if (nodeModel.equals(model)) {
                                setActiveModel(null);
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error("unable to update selected document");
                    }
                }
                active = false;
            }

            // cleanup internals
            if (editor == preview) {
                preview = null;
                return;
            }
            if (editors.containsKey(model)) {
                editors.remove(model);
            }
            if (pending.contains(model)) {
                pending.remove(model);
            }
        }
    }

    JcrNodeModel getPreviewModel(JcrNodeModel handle) {
        try {
            Node handleNode = handle.getNode();
            Node published = null;
            if (handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = handleNode.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    if (child.getName().equals(handleNode.getName())) {
                        // FIXME: This has knowledge of hippostd reviewed actions, which within this new context wrong
                        if (child.hasProperty("hippostd:state")) {
                            String state = child.getProperty("hippostd:state").getString();
                            if (state.equals("unpublished")) {
                                return new JcrNodeModel(child);
                            } else if (state.equals("published")) {
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

    JcrNodeModel getDraftModel(JcrNodeModel handle) {
        String user = ((UserSession) Session.get()).getCredentials().getString("username");
        try {
            Node handleNode = handle.getNode();
            if (handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = handleNode.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    if (child.getName().equals(handleNode.getName())) {
                        // FIXME: This has knowledge of hippostd reviewed actions, which here is not fundamentally wrong, but could raise hairs
                        if (child.hasProperty("hippostd:state")
                                && child.getProperty("hippostd:state").getString().equals("draft")
                                && child.getProperty("hippostd:holder").getString().equals(user)) {
                            return new JcrNodeModel(child);
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
