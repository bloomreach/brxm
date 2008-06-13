/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.template.builder;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.ClusterConfigDecorator;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeStore;
import org.hippoecm.frontend.plugins.standardworkflow.types.JcrTypeStore;
import org.hippoecm.frontend.sa.template.ITemplateEngine;
import org.hippoecm.frontend.sa.template.impl.TemplateEngine;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewPlugin extends RenderPlugin implements IJcrNodeModelListener {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PreviewPlugin.class);

    private static int instanceCount = 0;

    private IPluginControl child;
    private ModelService modelService;
    private TemplateEngine engine;
    private String engineId;
    private int instanceId;

    public PreviewPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        synchronized (PreviewPlugin.class) {
            instanceId = instanceCount++;
        }
        addExtensionPoint("template");

        ITypeStore typeStore = new JcrTypeStore(RemodelWorkflow.VERSION_DRAFT);
        engine = new TemplateEngine(context, typeStore);
        context.registerService(engine, ITemplateEngine.class.getName());
        engineId = context.getReference(engine).getServiceId();
        engine.setId(engineId);

        // register for flush events
        context.registerService(this, IJcrService.class.getName());
        
        onModelChanged();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        try {
            Node templateTypeNode = ((JcrNodeModel) getModel()).getNode();
            JcrNodeModel templateModel = new JcrNodeModel(getTemplate(templateTypeNode));
            PreviewClusterConfig clusterConfig = new PreviewClusterConfig(getPluginContext(), templateModel);

            JcrNodeModel propertyModel = new JcrNodeModel(getPrototype(templateTypeNode));
            if (child != null) {
                child.stopPlugin();
                modelService.destroy();
            }
            String clusterId = PreviewPlugin.class.getName() + "." + instanceId;
            IClusterConfig cluster = new ClusterConfigDecorator(clusterConfig, clusterId);
            cluster.put(ITemplateEngine.ENGINE, engineId);
            cluster.put(ITemplateEngine.MODE, ITemplateEngine.EDIT_MODE);
            cluster.put(RenderPlugin.WICKET_ID, getPluginConfig().getString("template"));

            String modelId = cluster.getString(RenderService.MODEL_ID);
            modelService = new ModelService(modelId, propertyModel);
            modelService.init(getPluginContext());
            child = getPluginContext().start(cluster);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private Node getTemplate(Node templateTypeNode) throws RepositoryException {
        if (templateTypeNode.hasNode(HippoNodeType.HIPPO_TEMPLATE)) {
            Node node = templateTypeNode.getNode(HippoNodeType.HIPPO_TEMPLATE);
            NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TEMPLATE);
            while (nodes.hasNext()) {
                Node template = nodes.nextNode();
                if (template.isNodeType("frontend:plugincluster")) {
                    return template;
                }
            }
        }
        return null;
    }

    private Node getPrototype(Node templateTypeNode) throws RepositoryException {
        NodeIterator iter = templateTypeNode.getNode(HippoNodeType.HIPPO_PROTOTYPE).getNodes(
                HippoNodeType.HIPPO_PROTOTYPE);
        while (iter.hasNext()) {
            Node node = iter.nextNode();
            if (node.isNodeType(HippoNodeType.NT_REMODEL)) {
                if (node.getProperty(HippoNodeType.HIPPO_REMODEL).getString().equals("draft")) {
                    return node;
                }
            }
        }
        throw new ItemNotFoundException("draft version of prototype was not found");
    }

    public void onFlush(JcrNodeModel nodeModel) {
        if (nodeModel.equals(getModel())) {
            onModelChanged();
        }
    }

}
