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

package org.hippoecm.frontend.plugins.xinha.dialog;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugins.xinha.XinhaPlugin;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.preferences.IPreferencesStore;
import org.hippoecm.repository.HippoStdNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBrowserDialog<T extends DocumentLink> extends AbstractXinhaDialog<T> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(AbstractBrowserDialog.class);

    private static final String LAST_VISITED = "last.visited";
    private static final String LAST_VISITED_NODETYPES_ALLOWED = "last.visited.nodetypes.allowed";
    private static final String[] DEFAULT_LAST_VISITED_NODETYPES_ALLOWED = new String[] { HippoStdNodeType.NT_FOLDER };

    protected final IPluginContext context;
    protected final IPluginConfig config;
    private IModelReference<Node> modelReference;
    private IObserver modelRefObserver;
    private IClusterControl control;
    protected IRenderService dialogRenderer;

    private IModel<Node> lastModelVisited;

    public AbstractBrowserDialog(IPluginContext context, IPluginConfig config, IModel<T> model) {
        super(model);

        this.context = context;
        this.config = config;

        add(createContentPanel("content"));
    }

    protected Component createContentPanel(String contentId) {
        //Get PluginConfigService
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);

        //Lookup clusterConfig from IPluginContext
        IClusterConfig cluster = pluginConfigService.getCluster(config.getString("cluster.name"));
        control = context.newCluster(cluster, config.getPluginConfig("cluster.options"));
        IClusterConfig decorated = control.getClusterConfig();

        //save modelServiceId and dialogServiceId in cluster config
        String modelServiceId = decorated.getString("wicket.model");
        IModel<Node> model = getModelObject().getNodeModel();

        if (model == null) {
            IPreferencesStore store = context.getService(IPreferencesStore.SERVICE_ID, IPreferencesStore.class);
            String lastVisited = store.getString(config.getName(), LAST_VISITED);
            if (lastVisited != null) {
                model = new JcrNodeModel(lastVisited);
            }
        }
        lastModelVisited = model;

        control.start();

        modelReference = context
                .getService(control.getClusterConfig().getString("wicket.model"), IModelReference.class);
        if (lastModelVisited != null) {
            modelReference.setModel(lastModelVisited);
        } else {
            IModel<Node> newModel = modelReference.getModel();
            if (newModel != null) {
                getModelObject().setNodeModel((JcrNodeModel) newModel);
                checkState();
            }
        }

        context.registerService(modelRefObserver = new IObserver() {

            public IObservable getObservable() {
                return modelReference;
            }

            public void onEvent(Iterator events) {
                IModel<Node> newModel = modelReference.getModel();
                if (newModel != null) {
                    JcrNodeModel currentModel = getModelObject().getNodeModel();
                    if (!newModel.equals(currentModel)) {
                        getModelObject().setNodeModel((JcrNodeModel) newModel);
                        checkState();
                    }
                }
                lastModelVisited = newModel;
            }

        }, IObserver.class.getName());

        dialogRenderer = context.getService(decorated.getString("wicket.id"), IRenderService.class);
        dialogRenderer.bind((IRenderService) getComponent().findParent(XinhaPlugin.class), contentId);
        return dialogRenderer.getComponent();
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (dialogRenderer != null) {
            dialogRenderer.render(target);
        }
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=845,height=485");
    }

    @Override
    void onCloseInternal() {
        savePreferences();
        dialogRenderer.unbind();
        dialogRenderer = null;
        context.unregisterService(modelRefObserver, IObserver.class.getName());
        modelReference = null;
        modelRefObserver = null;
        control.stop();
    }

    private void savePreferences() {
        if (lastModelVisited instanceof JcrNodeModel) {
            JcrNodeModel nodeModel = (JcrNodeModel) lastModelVisited;
            Node node = nodeModel.getNode();
            if (node != null) {
                String[] allowedTypes = config.containsKey(LAST_VISITED_NODETYPES_ALLOWED) ? config
                        .getStringArray(LAST_VISITED_NODETYPES_ALLOWED) : DEFAULT_LAST_VISITED_NODETYPES_ALLOWED;
                if (allowedTypes != null) {
                    for (String nodeType : allowedTypes) {
                        try {
                            Node testNode = node;
                            while (!testNode.getPath().equals("/")) { //TODO: Can do nicer
                                if (testNode.isNodeType(nodeType)) {
                                    IPreferencesStore store = context.getService(IPreferencesStore.SERVICE_ID,
                                            IPreferencesStore.class);
                                    store.set(config.getName(), LAST_VISITED, testNode.getPath());
                                    break;
                                }
                                testNode = testNode.getParent();
                            }
                        } catch (RepositoryException e) {
                            log.warn("An error occured while checking for nodetype[" + nodeType + "] on node["
                                    + nodeModel.getItemModel().getPath() + "]", e);
                        }
                    }
                }
            }
        }
    }

}
