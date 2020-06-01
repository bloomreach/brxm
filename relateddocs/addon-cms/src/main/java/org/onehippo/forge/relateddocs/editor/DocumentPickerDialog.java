/*
 * Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.relateddocs.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
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
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.forge.relateddocs.RelatedDoc;
import org.onehippo.forge.relateddocs.RelatedDocCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentPickerDialog extends Dialog<Node> {

    private static final Logger log = LoggerFactory.getLogger(DocumentPickerDialog.class);
    protected static final String CLUSTER_OPTIONS = "cluster.options";
    private static final String PICKER_CLUSTER_NAME_OPTION = "pickerClusterName";
    private static final String PICKER_CLUSTER_NAME_DEFAULT = "cms-pickers/documents";

    private List<String> nodetypes = new ArrayList<>();

    protected final IPluginContext context;
    protected final IPluginConfig config;
    protected IRenderService dialogRenderer;
    private IClusterControl control;
    private IModel<Node> selectedNode;
    private final RelatedDocCollection collection;
    private String editedDocumentId;

    private IObserver selectionModelObserver;
    private IModelReference<Node> selectionModelReference;

    private IModelReference<Node> folderModelReference;
    private IObserver folderModelObserver;

    public DocumentPickerDialog(final IPluginContext context, final IPluginConfig config, final IModel<Node> model,
                                final RelatedDocCollection collection) {
        super(model);
        setTitle(new StringResourceModel("document.picker.dialog.title", this, null));
        setSize(DialogConstants.LARGE);

        this.collection = collection;

        this.context = context;
        this.config = config;

        if (config.containsKey(CLUSTER_OPTIONS) && config.getPluginConfig(CLUSTER_OPTIONS) != null) {
            final IPluginConfig clusterOptionConfig = config.getPluginConfig(CLUSTER_OPTIONS);
            if (clusterOptionConfig.containsKey("nodetypes")) {
                String[] nodeTypes = clusterOptionConfig.getStringArray("nodetypes");
                this.nodetypes.addAll(Arrays.asList(nodeTypes));
            }
            if (nodetypes.isEmpty()) {
                log.debug("No configuration specified for filtering on nodetypes. No filtering will take place.");
            }
        }

        setOkEnabled(false);
        try {
            editedDocumentId = findHandleId(model);
            if (selectedNode != null) {
                // if dialog contains selected node and node is not equal to model
                // (note: this can only happen if dialog adds support for "remembering" last selected node,
                // currently, this will not happen because above support is missing):
                if (!selectedNode.getObject().getIdentifier().equals(editedDocumentId)) {
                    selectedNode = new JcrNodeModel(((UserSession) Session.get()).getJcrSession().getNodeByIdentifier(editedDocumentId));
                    setOkEnabled(true);
                }
            } else if (editedDocumentId != null) {
                // make our current document as selected node, so we cannot add it as a reference to itself
                selectedNode = new JcrNodeModel(((UserSession) Session.get()).getJcrSession().getNodeByIdentifier(editedDocumentId));
            }

        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }

        setOutputMarkupId(true);

        add(createContentPanel("content"));
    }

    protected String findHandleId(final IModel<Node> model) throws RepositoryException {
        if (model == null) {
            return null;
        }
        String uuid = model.getObject().getIdentifier();
        if (uuid != null) {
            Node ourNode = ((UserSession) Session.get()).getJcrSession().getNodeByIdentifier(uuid);
            // find handle:
            while (ourNode != null && !ourNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                ourNode = ourNode.getParent();
            }
            if (ourNode != null) {
                uuid = ourNode.getIdentifier();
            }
        }
        return uuid;
    }

    protected boolean isValidSelection(final IModel targetModel) {
        boolean isLinkable;
        boolean validType = false;

        if (targetModel == null || targetModel.getObject() == null) {
            return false;
        }

        try {
            final Node targetNode = (Node) targetModel.getObject();

            Node testNode = targetNode;
            if (targetNode.isNodeType(HippoNodeType.NT_HANDLE) && targetNode.hasNode(targetNode.getName())) {
                testNode = targetNode.getNode(targetNode.getName());
            }

            if (nodetypes == null || nodetypes.isEmpty()) {
                validType = true;
            }
            if (nodetypes != null) {
                for (String nodetype : nodetypes) {
                    if (testNode.isNodeType(nodetype)) {
                        validType = true;
                        break;
                    }
                }
            }

            isLinkable = targetNode.isNodeType(JcrConstants.MIX_REFERENCEABLE);
            isLinkable = isLinkable
                    && !(targetNode.isNodeType(HippoNodeType.NT_DOCUMENT)
                    && targetNode.getParent().isNodeType(HippoNodeType.NT_HANDLE));

        } catch (RepositoryException e) {
            log.error(e.getMessage());
            error("Failed to determine validity of selection");
            isLinkable = false;
        }

        return validType && isLinkable;
    }

    protected Component createContentPanel(final String contentId) {
        final IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        final String pickerClusterName = config.getString(PICKER_CLUSTER_NAME_OPTION, PICKER_CLUSTER_NAME_DEFAULT);
        final IClusterConfig template = pluginConfigService.getCluster(pickerClusterName);
        control = context.newCluster(template, null);

        control.start();

        final IClusterConfig clusterConfig = control.getClusterConfig();
        final String selectionModelServiceId = clusterConfig.getString("wicket.model");
        selectionModelReference = context.getService(selectionModelServiceId, IModelReference.class);

        context.registerService(selectionModelObserver = new IObserver() {

            public IObservable getObservable() {
                return selectionModelReference;
            }

            public void onEvent(Iterator events) {
                setSelectedModel(selectionModelReference.getModel());
            }

        }, IObserver.class.getName());

        final String folderModelServiceId = clusterConfig.getString("model.folder");
        if (folderModelServiceId != null) {
            folderModelReference = context.getService(folderModelServiceId, IModelReference.class);
            context.registerService(folderModelObserver = new IObserver() {

                public IObservable getObservable() {
                    return folderModelReference;
                }

                public void onEvent(Iterator events) {
                    setSelectedModel(folderModelReference.getModel());
                }
            }, IObserver.class.getName());
        }

        dialogRenderer = context.getService(clusterConfig.getString("wicket.id"), IRenderService.class);
        dialogRenderer.bind(null, contentId);
        return dialogRenderer.getComponent();
    }

    protected void setSelectedModel(IModel<Node> model) {
        if (isValidSelection(model)) {
            selectedNode = model;
            setOkEnabled(true);
        } else {
            setOkEnabled(false);
        }
    }

    @Override
    public final void onClose() {
        this.nodetypes = null;
        dialogRenderer.unbind();
        dialogRenderer = null;
        control.stop();

        context.unregisterService(selectionModelObserver, IObserver.class.getName());
        selectionModelObserver = null;
        selectionModelReference = null;

        context.unregisterService(folderModelObserver, IObserver.class.getName());
        folderModelReference = null;
        folderModelObserver = null;
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (dialogRenderer != null) {
            dialogRenderer.render(target);
        }
    }

    @Override
    public void onOk() {
        if (selectedNode == null) {
            error("No node selected");
            return;
        }

        JcrNodeModel selectedNodeModel = (JcrNodeModel) selectedNode;

        try {
            if (editedDocumentId.equalsIgnoreCase(selectedNodeModel.getNode().getIdentifier())) {
                error("You cannot add the same document as the related document");
                return;
            }
        } catch (RepositoryException e) {
            log.error("Unable to get the UUID for the selected document", e);
        }

        collection.add(new RelatedDoc(selectedNodeModel));

    }

    @Override
    protected void onDetach() {
        super.onDetach();

        if (selectedNode != null) {
            selectedNode.detach();
        }
    }
}

