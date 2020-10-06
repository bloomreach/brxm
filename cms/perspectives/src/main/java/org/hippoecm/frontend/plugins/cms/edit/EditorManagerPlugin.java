/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.IEditorContext;
import org.hippoecm.frontend.editor.IEditorFactory;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IEditorOpenListener;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class creates and keeps references to {@link IEditor} instances.</p>
 *
 * <p>It registers itself on the {@link IPluginContext}</p> with id {@value IEditorManager#EDITOR_ID}.</p>
 *
 * <p></p>
 * <p>It creates editors based on the node and mode.</p>
 * <p>An {@link ServiceException} is thrown if:
 * <ul>
 *     <li>None of the registered {@link IEditorFactory} instances created a editor</li>
 *     <li>If the node is of a black listed node type See {@link #validateNodeType(Node, IPluginConfig)} </li>
 * </ul></p>
 */
public class EditorManagerPlugin extends Plugin implements IEditorManager, IRefreshable, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(EditorManagerPlugin.class);

    private BrowserObserver browser;

    private List<IEditor<Node>> editors;
    private final Set<IEditorOpenListener> openListeners;
    private transient boolean active = false;

    /**
     * Parameter name for the black listed node type names
     */
    public static final String BLACK_LISTED_NODE_TYPE_NAMES = "blackListedNodeTypeNames";

    public EditorManagerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        browser = new BrowserObserver(this, context, config);

        editors = new LinkedList<>();
        openListeners = new LinkedHashSet<>();
        context.registerService(this, IRefreshable.class.getName());

        // register editor
        context.registerService(this, config.getString("editor.id"));
    }

    @Override
    public void registerOpenListener(final IEditorOpenListener listener) {
        openListeners.add(listener);
    }

    @Override
    public void unregisterOpenListener(final IEditorOpenListener listener) {
        openListeners.remove(listener);
    }

    public void detach() {
        browser.detach();
        for (IEditor<Node> editor : editors) {
            if (editor instanceof IDetachable) {
                ((IDetachable) editor).detach();
            }
        }
    }

    public IEditor<Node> getEditor(IModel<Node> model) {
        if (model instanceof JcrNodeModel) {
            JcrNodeModel editModel = getEditorModel((JcrNodeModel) model);
            for (IEditor<Node> editor : editors) {
                if (editor.getModel().equals(editModel)) {
                    return editor;
                }
            }
        } else {
            log.warn("Unknown model type", model);
        }
        return null;
    }

    public IEditor<Node> openEditor(IModel<Node> model) throws ServiceException {
        if (active) {
            throw new ServiceException("Cannot create editors recursively");
        }

        JcrNodeModel nodeModel = getEditorModel((JcrNodeModel) model);

        checkEditorDoesNotExist(nodeModel);

        try {
            active = true;

            return createEditor(nodeModel, IEditor.Mode.EDIT);
        } finally {
            active = false;
        }
    }

    public IEditor<Node> openPreview(IModel<Node> model) throws ServiceException {
        JcrNodeModel nodeModel = getEditorModel((JcrNodeModel) model);

        checkEditorDoesNotExist(nodeModel);

        return createEditor(nodeModel, IEditor.Mode.VIEW);
    }

    protected void checkEditorDoesNotExist(IModel<Node> model) throws ServiceException {
        for (IEditor<Node> editor : editors) {
            if (editor.getModel().equals(model)) {
                throw new ServiceException("editor already exists");
            }
        }
    }

    protected IEditor<Node> createEditor(final IModel<Node> model, IEditor.Mode mode) throws ServiceException {
        final IPluginConfig parameters = getPluginConfig().getPluginConfig("cluster.options");
        Node node = model.getObject();
        validateNodeType(node, parameters);
        final IEditorContext manager = new IEditorContext() {

            public IEditorManager getEditorManager() {
                return EditorManagerPlugin.this;
            }

            public void onClose() {
                EditorManagerPlugin.this.onClose(model);
            }

            public void onFocus() {
                EditorManagerPlugin.this.onFocus(model);
            }
        };
        List<IEditorFactory> upstream = getPluginContext().getServices(getPluginConfig().getString(IEditorFactory.SERVICE_ID,
                IEditorFactory.class.getName()), IEditorFactory.class);
        for (ListIterator<IEditorFactory> iter = upstream.listIterator(upstream.size()); iter.hasPrevious(); ) {
            IEditorFactory factory = iter.previous();
            IEditor<Node> editor;
            try {
                 editor = factory.newEditor(manager, model, mode, parameters);
            } catch (EditorException e) {
                throw new ServiceException(e);
            }
            if (editor != null) {
                openListeners.forEach(listener -> listener.onOpen(model));
                editors.add(editor);
                editor.focus();
                focusBrowser(model);
                return editor;
            }

        }
        throw new ServiceException(
                String.format("Could not create editor for node: { path %s }", JcrUtils.getNodePathQuietly(node)));
    }

    /**
     * <p>The property with the key {@value #BLACK_LISTED_NODE_TYPE_NAMES} can contain one or more string values
     * that contain valid nodetypes, e.g. rep:root, hippostd:folder and hippostd:directory.</p>
     *
     * <p></p>If the node is of one of type black listed node types a ServiceException will be thrown,
     * otherwise nothing.</p>
     *
     * @param node {@link Node} node to create an editor  for
     * @param parameters {@link IPluginConfig} for this class
     * @throws ServiceException if the node is of a black listed node type
     */
    private void validateNodeType(final Node node, final IPluginConfig parameters) throws ServiceException {
        final String[] blackListedNodeTypeNames = parameters.getStringArray(BLACK_LISTED_NODE_TYPE_NAMES);
        if (blackListedNodeTypeNames == null){
            log.debug("Could not find any parameter {} on {}.", BLACK_LISTED_NODE_TYPE_NAMES, parameters.getName());
            return;
        }

        for (String blackListedNodeTypeName : blackListedNodeTypeNames) {
            try {
                if (node.isNodeType(blackListedNodeTypeName)) {
                    throw new ServiceException(
                            String.format("Could not create an editor for node: { path: %s }, " +
                                            "because it is of the black listed type: %s. " +
                                            "Please correct the deep link to this document or delete the node type name from " +
                                            "the black list and provide a valid editor template."
                                    , JcrUtils.getNodePathQuietly(node), blackListedNodeTypeName));
                }
            } catch (RepositoryException e) {
                log.error("Could not get node type of node { path {} }", JcrUtils.getNodePathQuietly(node));
            }
        }
    }

    // validate existence of all open documents
    public void refresh() {
        active = true;
        try {
            List<IEditor<Node>> copy = new LinkedList<>(editors);
            for (final IEditor<Node> editor : copy) {
                if (editor instanceof IRefreshable) {
                    ((IRefreshable) editor).refresh();
                }
            }
        } finally {
            active = false;
        }
    }

    // callback methods for editor events

    void onFocus(IModel<Node> model) {
        if (!active) {
            active = true;
            try {
                focusBrowser(model);
            } finally {
                active = false;
            }
        }
    }

    private void focusBrowser(IModel<Node> nodeModel) {
        browser.setModel((JcrNodeModel) nodeModel);
    }

    void onClose(IModel<Node> model) {
        if (model != null) {
            // cleanup internals
            editors.remove(getEditor(model));
            if (editors.size() == 0) {
                browser.setModel(new JcrNodeModel((Node) null));
            }
        }
    }

    // internal

    private JcrNodeModel getEditorModel(JcrNodeModel nodeModel) {
        Node node = nodeModel.getNode();
        if (node != null) {
            try {
                if (node.getDepth() > 0 && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                    return new JcrNodeModel(node.getParent());
                }
            } catch (InvalidItemStateException iex) {
                try {
                    log.info("Item '{}' appears not to be existing anymore", node.getIdentifier());

                    // I am checking for debug level to avoid string concatenation if debug is not enabled
                    if (log.isDebugEnabled()) {
                        log.debug("Error happened when referencing item '" + node.getIdentifier() + "'", iex);
                    }
                } catch (RepositoryException rex) {
                    log.debug("Error resolving editor model", rex);
                }
            } catch (RepositoryException ex) {
                log.error("Error resolving editor model", ex);
            }
        }
        return nodeModel;
    }

}
