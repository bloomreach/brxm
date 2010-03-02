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
package org.hippoecm.frontend.editor.plugins.linkpicker;

import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerDialog extends AbstractDialog<String> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LinkPickerDialog.class);

    private List<String> nodetypes;

    protected final IPluginContext context;
    protected final IPluginConfig config;
    protected IRenderService dialogRenderer;
    private IObserver modelObserver;
    private IClusterControl control;
    private IModel<Node> selectedNode;

    public LinkPickerDialog(IPluginContext context, IPluginConfig config, IModel<String> model, List<String> nodetypes) {
        super(model);

        this.context = context;
        this.config = config;
        this.nodetypes = nodetypes;

        setSelectedModel(getInitialNode());

        setOutputMarkupId(true);

        add(createContentPanel("content"));
    }

    protected IModel<Node> getInitialNode() {
        try {
            String uuid = getModelObject();
            if (uuid != null && !"".equals(uuid)) {
                return new JcrNodeModel(((UserSession) Session.get()).getJcrSession().getNodeByUUID(uuid));
            }
        } catch(RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public IModel<String> getTitle() {
        return new StringResourceModel("link-picker", this, null);
    }

    protected boolean isValidSelection(IModel<Node> targetModel) {
        boolean isLinkable;
        boolean validType = false;

        if (targetModel == null || targetModel.getObject() == null) {
            return false;
        }

        try {
            Node targetNode = targetModel.getObject();

            Node testNode = targetNode;
            if (targetNode.isNodeType(HippoNodeType.NT_HANDLE) && targetNode.hasNode(targetNode.getName())) {
                testNode = targetNode.getNode(targetNode.getName());
            }

            if (nodetypes.size() == 0) {
                validType = true;
            }
            for (int i = 0; i < nodetypes.size(); i++) {
                if (testNode.isNodeType(nodetypes.get(i))) {
                    validType = true;
                    break;
                }
            }

            // do not enable linking to not referenceable nodes
            isLinkable = targetNode.isNodeType("mix:referenceable");
            // do not enable linking to hippo documents below hippo handle
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
        //Get PluginConfigService
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);

        //Lookup clusterConfig from IPluginContext
        IClusterConfig template = pluginConfigService.getCluster(config.getString("cluster.name"));
        IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig("cluster.options"));
        control = context.newCluster(template, parameters);

        control.start();

        IClusterConfig clusterConfig = control.getClusterConfig();
        //save modelServiceId and dialogServiceId in cluster config
        String modelServiceId = clusterConfig.getString("wicket.model");
        final IModelReference modelRef = context.getService(modelServiceId, IModelReference.class);
        IModel<Node> initial = getInitialNode();
        if (initial != null) {
            modelRef.setModel(initial);
        }
        context.registerService(modelObserver = new IObserver() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return modelRef;
            }

            public void onEvent(Iterator events) {
                setSelectedModel(modelRef.getModel());
            }
            
        }, IObserver.class.getName());

        dialogRenderer = context.getService(clusterConfig.getString("wicket.id"), IRenderService.class);
        dialogRenderer.bind(null, contentId);
        return dialogRenderer.getComponent();
    }

    @Override
    public final void onClose() {
        dialogRenderer.unbind();
        dialogRenderer = null;
        context.unregisterService(modelObserver, IObserver.class.getName());
        control.stop();
        control = null;
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (dialogRenderer != null) {
            dialogRenderer.render(target);
        }
        super.render(target);
    }

    private void setSelectedModel(IModel<Node> model) {
        if (isValidSelection(model)) {
            selectedNode = model;
            setOkEnabled(true);
        } else {
            setOkEnabled(false);
        }
    }
    
    @Override
    public void onOk() {
        if (selectedNode == null) {
            error("No node selected");
            return;
        }
        saveNode(selectedNode.getObject());
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        if (selectedNode != null) {
            selectedNode.detach();
        }
    }
    
    protected void saveNode(Node node) {
        try {
            getModel().setObject(node.getUUID());
        } catch (RepositoryException ex) {
            error(ex.getMessage());
        }
    }

}
