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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
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
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorManagerPlugin implements IPlugin, IEditorManager, IObserver, IRefreshable, IDetachable {
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
    transient boolean active = false;

    // map physical handle -> virtual path
    private Map<JcrNodeModel, JcrNodeModel> lastReferences;

    public EditorManagerPlugin(final IPluginContext context, final IPluginConfig config) {
        this.context = context;
        this.config = config;

        editors = new HashMap<JcrNodeModel, CmsEditor>();
        pending = new LinkedList<JcrNodeModel>();
        editorFactory = new EditorFactory(this, context, config.getString("cluster.edit.name"), config
                .getPluginConfig("cluster.edit.options"));
        previewFactory = new EditorFactory(this, context, config.getString("cluster.preview.name"), config
                .getPluginConfig("cluster.preview.options"));

        lastReferences = new HashMap<JcrNodeModel, JcrNodeModel>();

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

        context.registerService(this, IRefreshable.class.getName());

        // register editor
        context.registerService(this, config.getString("editor.id"));
    }

    public void detach() {
        for (Map.Entry<JcrNodeModel, JcrNodeModel> entry : lastReferences.entrySet()) {
            entry.getKey().detach();
            entry.getValue().detach();
        }
        for (JcrNodeModel model : editors.keySet()) {
            model.detach();
        }
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

                    // find physical node
                    Node node = nodeModel.getNode();
                    if (node instanceof HippoNode) {
                        try {
                            Node canonical = ((HippoNode) node).getCanonicalNode();
                            if (canonical == null) {
                                return;
                            }
                            if (!canonical.isSame(node)) {
                                // use physical handle as the basis for lookup
                                if (canonical.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                                    Node parent = canonical.getParent();
                                    if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                                        canonical = parent;
                                    }
                                }

                                // put in LRU map for reverse lookup when editor is selected
                                JcrNodeModel canonicalModel = new JcrNodeModel(canonical);
                                lastReferences.put(canonicalModel, nodeModel.getParentModel());

                                node = canonical;
                                nodeModel = canonicalModel;
                            } else {
                                if (node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                                    lastReferences.remove(nodeModel.getParentModel());
                                }
                            }
                        } catch (ItemNotFoundException ex) {
                            // physical node no longer exists
                            return;
                        }
                    }

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
                active = true;

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

                setActiveModel(nodeModel);
                return editor;
            } catch (CmsEditorException ex) {
                log.error(ex.getMessage());
                throw new ServiceException("Initialization failed", ex);
            } finally {
                active = false;
            }
        } else {
            IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
            dialogService.show(new TooManyEditorsDialog());
            pending.add(nodeModel);
            throw new ServiceException("Too many editors open");
        }
    }

    // validate existence of all open documents
    public void refresh() {
        Iterator<Map.Entry<JcrNodeModel, CmsEditor>> iter = editors.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<JcrNodeModel, CmsEditor> entry = iter.next();
            if (!entry.getKey().getItemModel().exists()) {
                try {
                    entry.getValue().close();
                    iter.remove();
                } catch (EditorException ex) {
                    log.warn("failed to close editor for non-existing document");
                }
            }
        }

        for(Iterator<JcrNodeModel> pendingIter = pending.iterator(); pendingIter.hasNext();) {
            JcrNodeModel model = pendingIter.next();
            if (!model.getItemModel().exists()) {
                pendingIter.remove();
            }
        }

        JcrNodeModel nodeModel = (JcrNodeModel) modelReference.getModel();
        if (nodeModel != null && !nodeModel.getItemModel().exists()) {
            // close preview when a new document is selected
            if (preview != null) {
                try {
                    preview.close();
                    preview = null;
                } catch (EditorException ex) {
                    log.warn("failed to close preview");
                }
            }
        }
    }

    void setActiveModel(JcrNodeModel nodeModel) {
        try {
            IModelReference modelService = context.getService(config.getString(RenderService.MODEL_ID),
                    IModelReference.class);
            if (modelService != null) {
                if (nodeModel != null && nodeModel.getParentModel() != null) {
                    JcrNodeModel parentModel = nodeModel.getParentModel();
                    Node parentNode = parentModel.getNode();
                    if (parentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                        if (lastReferences.containsKey(parentModel)) {
                            JcrNodeModel targetParent = lastReferences.get(parentModel);
                            // Locate document in target.  The first node (lowest sns index in target)
                            // whose canonical equivalent is under the handle will be used.
                            int index = 0;
                            Node target = null;
                            try {
                                NodeIterator nodes = targetParent.getNode().getNodes(parentModel.getNode().getName());
                                while (nodes.hasNext()) {
                                    Node node = nodes.nextNode();
                                    if (node == null || !(node instanceof HippoNode)) {
                                        continue;
                                    }
                                    try {
                                        Node canonical = ((HippoNode) node).getCanonicalNode();
                                        if (canonical == null) {
                                            continue;
                                        }
                                        if (canonical.getParent().isSame(parentNode)) {
                                            if (index == 0 || node.getIndex() < index) {
                                                index = node.getIndex();
                                                target = node;
                                            }
                                        }
                                    } catch (ItemNotFoundException ex) {
                                        // physical node no longer exists
                                        continue;
                                    }
                                }
                            } catch (RepositoryException ex) {
                                log.error(ex.getMessage(), ex);
                            }
                            if (target != null) {
                                modelService.setModel(new JcrNodeModel(target));
                                return;
                            } else {
                                log.warn("unable to find virtual equivalent");
                            }
                        }
                        modelService.setModel(parentModel);
                    } else {
                        modelService.setModel(nodeModel);
                    }
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
            
            if (!active) {
                active = true;

                JcrNodeModel parentModel = model.getParentModel();
                if (parentModel.getItemModel().exists()) {
                    try {
                        Node parent = parentModel.getNode();
                        if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                            // Deselect the currently selected node if it corresponds
                            // to the editor that is being closed.
                            JcrNodeModel selectedNodeModel = (JcrNodeModel) modelReference.getModel();
                            if(selectedNodeModel != null) {
                                Node selected = selectedNodeModel.getNode();
                                if (selected != null && selected instanceof HippoNode) {
                                    try {
                                        Node canonical = ((HippoNode) selected).getCanonicalNode();
                                        if (canonical != null) {
                                            if (canonical.isSame(selected) || canonical.getParent().isSame(parent)) {
                                                modelReference.setModel(null);
                                            }
                                        }
                                    } catch (ItemNotFoundException ex) {
                                        // physical item no longer exists
                                    }
                                }
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                }

                // cleanup lru list
                lastReferences.remove(parentModel);

                active = false;
            }

            // cleanup internals
            if (editors.containsKey(model)) {
                editors.remove(model);
            }
            if(preview == editor) {
                preview = null;
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
