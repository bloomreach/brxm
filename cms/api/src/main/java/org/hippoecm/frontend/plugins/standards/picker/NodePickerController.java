/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.picker;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.preferences.IPreferencesStore;
import org.hippoecm.frontend.service.render.AbstractRenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NodePickerController implements IDetachable {

    private static final Logger log = LoggerFactory.getLogger(NodePickerController.class);

    private static final String LAST_VISITED = "last.visited";
    private static final String ROOT_PATH = "root.path";
    private static final String ROOT_PATHS = "root.paths";
    private static final String STRICT_ROOT_PATH = "strict.root.path";

    private final IPluginContext context;
    private final NodePickerControllerSettings settings;

    private IClusterControl control;
    private IRenderService renderer;

    private IObserver selectionModelObserver;
    private IObserver folderModelObserver;

    private IModelReference<Node> selectionModelReference;
    private IModelReference<Node> folderModelReference;

    private IModel<Node> selectedModel;
    private IModel<Node> lastModelVisited;
    private IModel<Node> baseModel;

    public NodePickerController(final IPluginContext context, final NodePickerControllerSettings settings) {
        this.context = context;
        this.settings = settings;

        if (settings.isLastVisitedEnabled()) {
            lastModelVisited = getLastVisitedFromPreferences();
        }
        if (settings.hasBaseUUID()) {
            final String baseUUID = settings.getBaseUUID();
            try {
                final Node baseNode = UserSession.get().getJcrSession().getNodeByIdentifier(baseUUID);
                baseModel = new JcrNodeModel(baseNode);
            } catch (final RepositoryException e) {
                log.error("Could not create base model from UUID[" + baseUUID + "]", e);
            }
        }
    }

    public Component create(final String id) {
        final IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);

        final IClusterConfig template = pluginConfigService.getCluster(settings.getClusterName());
        final IPluginConfig parameters = settings.getClusterOptions();

        control = context.newCluster(template, parameters);
        control.start();

        final IClusterConfig clusterConfig = control.getClusterConfig();

        final String selectionModelServiceId = clusterConfig.getString(settings.getSelectionServiceKey());
        selectionModelReference = context.getService(selectionModelServiceId, IModelReference.class);
        context.registerService(selectionModelObserver = new IObserver() {

            public IObservable getObservable() {
                return selectionModelReference;
            }

            public void onEvent(final Iterator events) {
                setSelectedModel(selectionModelReference.getModel());
            }

        }, IObserver.class.getName());

        final String folderModelServiceId = clusterConfig.getString(settings.getFolderServiceKey());
        if (folderModelServiceId != null) {
            folderModelReference = context.getService(folderModelServiceId, IModelReference.class);
            context.registerService(folderModelObserver = new IObserver() {

                public IObservable getObservable() {
                    return folderModelReference;
                }

                public void onEvent(final Iterator events) {
                    setSelectedFolder(folderModelReference.getModel());
                }
            }, IObserver.class.getName());
        }

        renderer = context.getService(clusterConfig.getString(AbstractRenderService.WICKET_ID), IRenderService.class);
        renderer.bind(null, id);
        return renderer.getComponent();
    }

    public void setSelectedFolder(final IModel<Node> model) {
        selectionModelReference.setModel(model);
        setSelectedModel(model);
        onFolderSelected(model);
    }

    protected void onFolderSelected(final IModel<Node> model) {
    }

    /**
     * Try to determine which model should be used as initial selection. First, retrieve the model that represents the
     * actual selected node and see if it is valid. If so, select it and return, if not, see if a last visited model
     * is set and if so select it and return. If not, see if a default model is set, if so, select it and return. If
     * not, fallback to the model currently in the selectionModelReference.
     *
     * TODO: We should try and see if the last-visited model is visible in the browser, if not, go on to default model
     **/
    public void initSelection() {
        final IModel<Node> initialModel = getInitialModel();
        if (isValidSelection(initialModel)) {
            selectionModelReference.setModel(initialModel);
            return;
        }

        if (settings.isLastVisitedEnabled() && lastModelVisited != null) {
            selectionModelReference.setModel(lastModelVisited);
            return;
        }

        final IModel<Node> defaultModel = getBaseModel();
        if (defaultModel != null) {
            selectionModelReference.setModel(defaultModel);
            return;
        }

        setSelectedModel(selectionModelReference.getModel());
    }

    /**
     * A hook that allows subclasses to specify a default location.
     *
     * @return An model used as default initial selection
     */
    protected IModel<Node> getBaseModel() {
        return baseModel;
    }

    private void setSelectedModel(final IModel<Node> model) {
        selectedModel = model;

        onSelect(isValidSelection(model));

        if (settings.isLastVisitedEnabled() && model != null) {
            lastModelVisited = model;
        }
    }

    public IModel<Node> getSelectedModel() {
        return selectedModel;
    }

    /**
     * Return the initially selected model
     *
     * @return The model that is initially selected
     */
    protected abstract IModel<Node> getInitialModel();

    /**
     * This method is called when a new model is selected.
     *
     * @param isValid If the model is considered a valid selection model, value will be true, otherwise false.
     */
    protected void onSelect(final boolean isValid) {
    }

    /**
     * This method determines the validity of the selected node in context of this dialog. Null values are always
     * considered invalid. Validity is based on whether the nodetype is allowed and if the node is linkable.
     *
     * @param targetModel The model providing the node to be validated.
     * @return If the provided node is a valid dialog selection.
     */
    protected boolean isValidSelection(final IModel<Node> targetModel) {
        if (targetModel == null) {
            return false;
        }

        final Node targetNode = targetModel.getObject();

        if (targetNode != null) {
            try {
                return isValidNodeType(targetNode) && isLinkable(targetNode);
            } catch (final RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return false;
    }

    /**
     * Determine if the provided node is of nodeType["mix:referenceable"] and if it's not a hippo:document below a
     * hippo:handle.
     *
     * @param node Node to test for linkability
     * @return If this node is linkable
     * @throws RepositoryException Something went wrong in the repository
     */
    protected boolean isLinkable(final Node node) throws RepositoryException {
        // do not enable linking to not referenceable nodes
        if (!node.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
            return false;
        }
        // do not enable linking to hippo documents below hippo handle
        return !(node.isNodeType(HippoNodeType.NT_DOCUMENT) && node.getParent().isNodeType(HippoNodeType.NT_HANDLE));
    }

    /**
     * <p>
     * Determine if the node type of the provided node is valid. In case a handle is passed,
     * it will use the nested hippo-document node, or, if none found (in case of a delete), return false.
     * <p/>
     * <p>
     * By default, only documents are considered valid. To use more fine-grained validation, like for example,
     * only allow document types "foo" & "bar", or allow folders as well, a list of allowedNodeTypes can be set during
     * construction of the dialog.
     * </p>
     *
     * @param node The node to be validated. If it is of nodeType["hippo:handle"], the first childNode with the same name
     *             will be used instead.
     * @return If the node type is considered valid.
     * @throws RepositoryException Something went wrong in the repository
     */
    protected boolean isValidNodeType(Node node) throws RepositoryException {
        boolean isDocument = false;
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            if (node.hasNode(node.getName())) {
                isDocument = true;
                node = node.getNode(node.getName());
            } else {
                return false; //deleted node
            }
        }

        if (settings.hasSelectableNodeTypes()) {
            for (final String allowedNodeType : settings.getSelectableNodeTypes()) {
                if (node.isNodeType(allowedNodeType)) {
                    return true;
                }
            }
            return false;
        }

        return isDocument;
    }

    public IRenderService getRenderer() {
        return renderer;
    }

    public final void onClose() {
        if (settings.isLastVisitedEnabled()) {
            saveLastModelVisited();
        }

        renderer.unbind();
        renderer = null;

        context.unregisterService(selectionModelObserver, IObserver.class.getName());
        selectionModelObserver = null;
        selectionModelReference = null;

        context.unregisterService(folderModelObserver, IObserver.class.getName());
        folderModelReference = null;
        folderModelObserver = null;

        control.stop();
        control = null;
    }

    public void detach() {
        if (selectedModel != null) {
            selectedModel.detach();
        }
        if (lastModelVisited != null) {
            lastModelVisited.detach();
        }
        if (baseModel != null) {
            baseModel.detach();
        }
    }

    /**
     * Check the IPreferencesStore for a last visited location (a node path) and if found, return it in a new
     * JcrNodeModel, otherwise return null.
     *
     * @return A new JcrNodeModel pointing to the last visited location (a node path) or null
     */
    protected IModel<Node> getLastVisitedFromPreferences() {
        final IPreferencesStore store = context.getService(IPreferencesStore.SERVICE_ID, IPreferencesStore.class);
        final String lastVisited = store.getString(settings.getLastVisitedKey(), LAST_VISITED);
        if (lastVisited != null) {
            return new JcrNodeModel(lastVisited);
        }
        return null;
    }

    /**
     * Save the last visited location in the preferences store. By default, it will try to use the parent of the nearest
     * "hippo:handle" node, other node-types can be specified by configuring a multi-value String property named
     * "last.visited.nodetypes".
     */
    private void saveLastModelVisited() {
        if (lastModelVisited == null) {
            return;
        }

        final Node node = lastModelVisited.getObject();
        if (node == null) {
            return;
        }

        try {
            final Node lastVisited = settings.hasLastVisitedNodeTypes()
                    ? findNodeByType(node, settings.getLastVisitedNodeTypes())
                    : findParentOfDocumentOrHandle(node);

            if (lastVisited != null) {
                storeLastVisited(node.getPath());
            }
        } catch (final RepositoryException e) {
            log.warn("Could not save last-node-path-visited in preferences store");
        }
    }

    private Node findParentOfDocumentOrHandle(Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            node = node.getParent();
        }

        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            if (!node.hasNode(node.getName())) {
                //deleted
                return null;
            }
            node = node.getParent();
        }
        return node;
    }

    private Node findNodeByType(Node node, final String[] nodeTypes) throws RepositoryException {
        final Node root = UserSession.get().getJcrSession().getRootNode();
        while (!node.isSame(root)) {
            for (final String nodeType : nodeTypes) {
                if (node.isNodeType(nodeType)) {
                    return node;
                }
            }
            node = node.getParent();
        }
        return null;
    }

    private void storeLastVisited(final String path) {
        final IPreferencesStore store = context.getService(IPreferencesStore.SERVICE_ID, IPreferencesStore.class);
        store.set(settings.getLastVisitedKey(), LAST_VISITED, path);
    }

    /**
     * Helper method to retrieve the current folder model
     *
     * @return The folder model
     */
    public IModel<Node> getFolderModel() {
        if (folderModelReference != null) {
            return folderModelReference.getModel();
        }
        return null;
    }

    public String getRootPath() {
        if (control != null) {
            final IClusterConfig clusterConfig = control.getClusterConfig();
            return clusterConfig.getString(ROOT_PATH);
        }
        return null;
    }

    public String[] getRootPaths() {
        if (control != null) {
            final IClusterConfig clusterConfig = control.getClusterConfig();
            return clusterConfig.getStringArray(ROOT_PATHS);
        }
        return null;
    }

    public boolean isStrictRootPath() {
        if (control != null) {
            final IClusterConfig clusterConfig = control.getClusterConfig();
            return clusterConfig.getBoolean(STRICT_ROOT_PATH);
        }
        return false;
    }
}
