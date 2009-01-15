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
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorManagerPlugin implements IPlugin, IModelListener, IJcrNodeModelListener, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(EditorManagerPlugin.class);

    private IPluginContext context;
    private IPluginConfig config;

    private EditorFactory previewFactory;
    private EditorFactory editorFactory;

    private Editor preview;
    private Map<JcrNodeModel, Editor> editors;
    private List<JcrNodeModel> pending;
    private transient boolean active = false;

    public EditorManagerPlugin(final IPluginContext context, final IPluginConfig config) {
        this.context = context;
        this.config = config;

        editors = new HashMap<JcrNodeModel, Editor>();
        pending = new LinkedList<JcrNodeModel>();
        editorFactory = new EditorFactory(context, config.getString("cluster.edit.name"));
        previewFactory = new EditorFactory(context, config.getString("cluster.preview.name"));

        context.registerService(this, IJcrService.class.getName());

        // monitor document in browser 
        if (config.getString(RenderService.MODEL_ID) != null) {
            context.registerService(this, config.getString(RenderService.MODEL_ID));
        } else {
            log.warn("No model defined ({})", RenderService.MODEL_ID);
        }

        // register editor
        context.registerService(new IEditService() {
            private static final long serialVersionUID = 1L;

            public void close(IModel model) {
                EditorManagerPlugin.this.close(model);
            }

            public void edit(IModel model) {
                EditorManagerPlugin.this.edit(model);
            }

        }, config.getString("editor.id"));
    }

    public void detach() {
    }

    public void updateModel(IModel model) {
        if (!active) {
            active = true;
            try {
                JcrNodeModel nodeModel = (JcrNodeModel) model;
                if (nodeModel != null && nodeModel.getNode() != null) {
                    // close preview when a new document is selected
                    if (preview != null) {
                        preview.close();
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
                            openEditor(draftDocument);
                            return;
                        }

                        // show preview
                        JcrNodeModel previewDocument = getPreviewModel(nodeModel);
                        if (previewDocument != null) {
                            try {
                                this.preview = previewFactory.newEditor(previewDocument);
                                this.preview.focus();
                            } catch (EditorException ex) {
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
                        } catch (EditorException ex) {
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

    public void onFlush(JcrNodeModel nodeModel) {
        if (!active) {
            active = true;
            try {
                if (nodeModel != null && nodeModel.getNode() != null) {
                    if (preview != null) {
                        JcrNodeModel previewModel = (JcrNodeModel) preview.getModel();
                        if (previewModel.getParentModel().equals(nodeModel)) {
                            if (!previewModel.getItemModel().exists()) {
                                preview.close();
                                preview = null;
                                return;
                            }
                        }
                    }

                    for (JcrNodeModel editorModel : editors.keySet()) {
                        if (editorModel.getParentModel().equals(nodeModel)) {
                            if (!editorModel.getItemModel().exists()) {
                                editors.get(editorModel).close();
                                editors.remove(editorModel);
                                return;
                            }
                        }
                    }

                    for (JcrNodeModel pendingModel : pending) {
                        if (pendingModel.getParentModel().equals(nodeModel)) {
                            if (!pendingModel.getItemModel().exists()) {
                                pending.remove(pendingModel);
                                return;
                            }
                        }
                    }
                }
            } finally {
                active = false;
            }
        }
    }

    void edit(IModel editModel) {
        if (!active) {
            active = true;
            try {
                if (editModel instanceof JcrNodeModel) {
                    IModelService modelService = context.getService(config.getString(RenderService.MODEL_ID),
                            IModelService.class);
                    if (modelService != null) {
                        JcrNodeModel nodeModel = (JcrNodeModel) editModel;
                        if (nodeModel.getParentModel() != null
                                && nodeModel.getParentModel().getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                            modelService.setModel(nodeModel.getParentModel());
                        } else {
                            modelService.setModel(nodeModel);
                        }
                        if (preview != null) {
                            preview.close();
                            preview = null;
                        }
                    }
                    if (editors.containsKey(editModel)) {
                        editors.get(editModel).focus();
                    } else if (!pending.contains(editModel)) {
                        openEditor((JcrNodeModel) editModel);
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            } finally {
                active = false;
            }
        }
    }

    void close(IModel model) {
        if (!active) {
            active = true;
            try {
                if (model != null && model instanceof JcrNodeModel) {
                    Node node = ((JcrNodeModel) model).getNode();
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        for (JcrNodeModel nodeModel : editors.keySet()) {
                            if (nodeModel.getParentModel().equals(model)) {
                                editors.get(nodeModel).close();
                                editors.remove(nodeModel);
                                break;
                            }
                        }
                        for (JcrNodeModel nodeModel : pending) {
                            if (nodeModel.getParentModel().equals(model)) {
                                pending.remove(nodeModel);
                            }
                        }
                    } else {
                        if (editors.containsKey(model)) {
                            editors.get(model).close();
                            editors.remove(model);
                        }
                        if (pending.contains(model)) {
                            pending.remove(model);
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

    void openEditor(JcrNodeModel nodeModel) {
        if (editors.size() < 4) {
            try {
                Editor editor = editorFactory.newEditor(nodeModel);
                editors.put(nodeModel, editor);
            } catch (EditorException ex) {
                log.error(ex.getMessage());
            } finally {
                Editor editor = editors.get(nodeModel);
                if (editor != null) {
                    editor.focus();
                }
            }
        } else {
            IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
            dialogService.show(new TooManyEditorsWarningDialog());
            pending.add(nodeModel);
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
