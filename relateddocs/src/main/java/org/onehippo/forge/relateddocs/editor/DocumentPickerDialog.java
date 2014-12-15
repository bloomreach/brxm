/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialog;
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

/**
 * @author vijaykiran
 */
public class DocumentPickerDialog extends AbstractDialog<Node> {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(LinkPickerDialog.class);
    protected static final String CLUSTER_OPTIONS = "cluster.options";

    private List<String> nodetypes = new ArrayList<String>();

    protected final IPluginContext context;
    protected final IPluginConfig config;
    protected IRenderService dialogRenderer;
    private IClusterControl control;
    private IModel<Node> selectedNode;
    private final RelatedDocCollection collection;
    private String uuid;

    private IObserver selectionModelObserver;
    private IModelReference<Node> selectionModelReference;

    private IModelReference<Node> folderModelReference;
    private IObserver folderModelObserver;

    public DocumentPickerDialog(IPluginContext context, IPluginConfig config, IModel<Node> model, RelatedDocCollection collection) {
        super(model);

        this.collection = collection;

        this.context = context;
        this.config = config;

        if (config.containsKey(CLUSTER_OPTIONS) && config.getPluginConfig(CLUSTER_OPTIONS) != null) {
            final IPluginConfig clusterOptionConfig = config.getPluginConfig(CLUSTER_OPTIONS);
            if (clusterOptionConfig.containsKey("nodetypes")) {
                String[] nodeTypes = clusterOptionConfig.getStringArray("nodetypes");
                this.nodetypes.addAll(Arrays.asList(nodeTypes));
            }
            if (nodetypes.size() == 0) {
                log.debug("No configuration specified for filtering on nodetypes. No filtering will take place.");
            }
        }

        setOkEnabled(false);
        try {
            uuid = model.getObject().getIdentifier();
            if (uuid != null && !"".equals(uuid)) {
                selectedNode = new JcrNodeModel(((UserSession) Session.get()).getJcrSession().getNodeByIdentifier(uuid));
                setOkEnabled(true);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }

        setOutputMarkupId(true);

        add(createContentPanel("content"));
    }

    public IModel getTitle() {
        return new StringResourceModel("document.picker.dialog.title", this, null);
    }

    protected boolean isValidSelection(IModel targetModel) {
        boolean isLinkable;
        boolean validType = false;

        if (targetModel == null || targetModel.getObject() == null) {
            return false;
        }

        try {
            Node targetNode = (Node) targetModel.getObject();

            Node testNode = targetNode;
            if (targetNode.isNodeType(HippoNodeType.NT_HANDLE) && targetNode.hasNode(targetNode.getName())) {
                testNode = targetNode.getNode(targetNode.getName());
            }

            if (nodetypes == null || nodetypes.size() == 0) {
                validType = true;
            }
            if (nodetypes != null) {
                for (int i = 0; i < nodetypes.size(); i++) {
                    if (testNode.isNodeType(nodetypes.get(i))) {
                        validType = true;
                        break;
                    }
                }
            }

            isLinkable = targetNode.isNodeType("mix:referenceable");
            isLinkable = isLinkable
                    && !(targetNode.isNodeType(HippoNodeType.NT_DOCUMENT) && targetNode.getParent().isNodeType(
                    HippoNodeType.NT_HANDLE));
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            error("Failed to determine validity of selection");
            isLinkable = false;
        }

        return validType && isLinkable;
    }

    protected Component createContentPanel(String contentId) {
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig template = pluginConfigService.getCluster("cms-pickers/documents");
        //TODO: is this ok? IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig(CLUSTER_OPTIONS));
        control = context.newCluster(template, null);

        control.start();

        IClusterConfig clusterConfig = control.getClusterConfig();

        final String selectionModelServiceId = clusterConfig.getString("wicket.model");
        selectionModelReference = context.getService(selectionModelServiceId, IModelReference.class);
        context.registerService(selectionModelObserver = new IObserver() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return selectionModelReference;
            }

            public void onEvent(Iterator events) {
                setSelectedModel((JcrNodeModel) selectionModelReference.getModel());
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
            if (uuid.equalsIgnoreCase(selectedNodeModel.getNode().getIdentifier())) {
                error("You cannot add the same document as the related document!!");
                return;
            }
        } catch (RepositoryException re) {
            log.error("Unable to get the UUID for the selected document", re);
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

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=850,height=445");
    }

}

