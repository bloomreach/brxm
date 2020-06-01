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
package org.hippoecm.frontend.plugins.cms.edit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BrowserObserver implements IObserver<IModelReference<Node>>, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowserObserver.class);

    private EditorManagerPlugin editorMgr;

    // map physical handle -> virtual parent
    private Map<JcrNodeModel, JcrNodeModel> lastReferences;
    private IModelReference<Node> modelReference;
    private transient boolean active = false;

    @SuppressWarnings("unchecked")
    BrowserObserver(EditorManagerPlugin plugin, final IPluginContext context, IPluginConfig config) {
        this.editorMgr = plugin;

        lastReferences = new HashMap<JcrNodeModel, JcrNodeModel>();

        context.registerTracker(new ServiceTracker<IModelReference>(IModelReference.class) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onServiceAdded(IModelReference service, String name) {
                super.onServiceAdded(service, name);
                if (modelReference == null) {
                    modelReference = service;
                    context.registerService(BrowserObserver.this, IObserver.class.getName());
                }
            }

            @Override
            protected void onRemoveService(IModelReference service, String name) {
                if (service == modelReference) {
                    context.unregisterService(BrowserObserver.this, IObserver.class.getName());
                    modelReference = null;
                }
                super.onRemoveService(service, name);
            }

        }, config.getString(RenderService.MODEL_ID));
    }

    IModel<Node> getModel() {
        if (modelReference == null) {
            return null;
        }

        try {
            return getEditorModel(modelReference.getModel());
        } catch (InvalidItemStateException iex) {
            final Node node = modelReference.getModel().getObject();

            try {
                log.info("Item '{}' appears not to be existing anymore", node.getIdentifier());

                // I am checking for debug level to avoid string concatenation if debug is not enabled
                if (log.isDebugEnabled()) {
                    log.debug("Error happened when referencing item '" + node.getIdentifier() + "'", iex);
                }
            } catch (RepositoryException rex) {
                log.debug("Could not retrieve editor model", rex);
            }

            return null;
        } catch (RepositoryException e) {
            log.error("Could not retrieve editor model", e);
            return null;
        }
    }

    void setModel(JcrNodeModel nodeModel) {
        if (modelReference == null) {
            return;
        }
        if (!active) {
            active = true;
            try {
                modelReference.setModel(getBrowserModel(nodeModel));
            } catch (RepositoryException e) {
                log.error("Failed to select document", e);
            } finally {
                active = false;
            }
        }
    }

    public IModelReference<Node> getObservable() {
        return modelReference;
    }

    public void onEvent(Iterator<? extends IEvent<IModelReference<Node>>> event) {
        if (!active) {
            active = true;
            try {
                IModel<Node> nodeModel = getEditorModel(modelReference.getModel());
                if (nodeModel != null && nodeModel.getObject() != null) {
                    IEditor<Node> editor = editorMgr.getEditor(nodeModel);
                    if (editor == null) {
                        editor = editorMgr.openPreview(nodeModel);
                    }
                    editor.focus();
                }
            } catch (ServiceException ex) {
                log.error("could not open preview", ex);
            } catch (RepositoryException ex) {
                log.error("could not open preview", ex);
            } finally {
                active = false;
            }
        }
    }

    public void detach() {
        for (Map.Entry<JcrNodeModel, JcrNodeModel> entry : lastReferences.entrySet()) {
            entry.getKey().detach();
            entry.getValue().detach();
        }
    }

    private IModel<Node> getEditorModel(IModel<Node> nodeModel) throws RepositoryException {
        // find physical node
        if (nodeModel == null) {
            return null;
        }
        Node node = nodeModel.getObject();
        if (node == null) {
            return null;
        }
        if (node instanceof HippoNode) {
            try {
                Node canonical = ((HippoNode) node).getCanonicalNode();
                if (canonical == null) {
                    return null;
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
                    lastReferences.put(canonicalModel, new JcrNodeModel(node.getParent()));

                    return new JcrNodeModel(canonical);
                }
            } catch (ItemNotFoundException ex) {
                // physical node no longer exists
                return null;
            }
        }
        return nodeModel;
    }

    private JcrNodeModel getBrowserModel(JcrNodeModel nodeModel) throws RepositoryException {
        Node parentNode = nodeModel.getNode();
        if (parentNode == null) {
            return nodeModel;
        }
        if (parentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            if (lastReferences.containsKey(nodeModel)) {
                JcrNodeModel targetParent = lastReferences.get(nodeModel);
                Node targetParentNode = targetParent.getNode();
                if (targetParentNode != null) {
                    boolean isResultSet = targetParentNode.isNodeType(HippoNodeType.NT_FACETRESULT);
                    // Locate document in target.  The first node (lowest sns index in target)
                    // whose canonical equivalent is under the handle will be used.
                    int index = 0;
                    Node target = null;
                    try {
                        NodeIterator nodes = targetParent.getNode().getNodes(nodeModel.getNode().getName());
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
                                if (isResultSet) {
                                    if (canonical.getParent().isSame(parentNode)) {
                                        if (index == 0 || node.getIndex() < index) {
                                            index = node.getIndex();
                                            target = node;
                                        }
                                    }
                                } else {
                                    if (canonical.isSame(parentNode)) {
                                        target = node;
                                        break;
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
                        return new JcrNodeModel(target);
                    } else {
                        log.warn("unable to find virtual equivalent");
                    }
                }
            }
        }
        return nodeModel;
    }

}
