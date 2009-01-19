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
package org.hippoecm.frontend.plugin.workflow;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowPlugin implements IPlugin, IModelListener, IJcrNodeModelListener, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(WorkflowPlugin.class);

    private class Cluster implements IClusterable {
        private static final long serialVersionUID = 1L;

        ModelService modelService;
        IClusterControl control;

        Cluster(IClusterConfig clusterConfig, IModel model) {
            control = context.newCluster(clusterConfig, null);

            String modelId = control.getClusterConfig().getString("wicket.model"); 
            modelService = new ModelService(modelId, model);
            modelService.init(context);

            control.start();

            modelService.resetModel();
        }

        void stop() {
            control.stop();
            modelService.destroy();
        }
    }

    public static final String CATEGORIES = "workflow.categories";

    private IPluginContext context;
    private IPluginConfig config;
    private String[] categories;
    private JcrNodeModel model;
    private List<Cluster> workflows;

    public WorkflowPlugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        model = new JcrNodeModel((Node) null);
        workflows = new LinkedList<Cluster>();

        if (config.get(CATEGORIES) != null) {
            categories = config.getStringArray(CATEGORIES);
            if (log.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("workflow showing categories");
                for (String category : categories)
                    sb.append(" " + category);
                log.debug(new String(sb));
            }
        } else {
            log.warn("No categories ({}) defined", CATEGORIES);
        }

        if (config.getString(RenderService.MODEL_ID) != null) {
            context.registerService(this, config.getString(RenderService.MODEL_ID));
            IModelService modelService = context.getService(config.getString(RenderService.MODEL_ID),
                    IModelService.class);
            if (modelService != null) {
                updateModel(modelService.getModel());
            }
        } else {
            log.warn("");
        }

        context.registerService(this, IJcrService.class.getName());
    }

    // implement IModelListener
    public void updateModel(IModel imodel) {
        closeWorkflows();
        if (imodel == null || ((JcrNodeModel) imodel).getNode() == null) {
            return;
        }
        model = (JcrNodeModel) imodel;
        try {
            List<String> cats = new LinkedList<String>();
            for (String category : categories) {
                cats.add(category);
            }
            WorkflowsModel workflowsModel = new WorkflowsModel(model, cats);
            if (log.isDebugEnabled()) {
                try {
                    log.debug("obtained workflows on " + model.getNode().getPath() + " counted "
                            + workflowsModel.size() + " unique renderers");
                } catch (RepositoryException ex) {
                    log.debug("debug message failed ", ex);
                }
            }

            Iterator<WorkflowsModel> iter = workflowsModel.iterator(0, workflowsModel.size());
            while (iter.hasNext()) {
                showWorkflow(iter.next());
            }
        } catch (RepositoryException ex) {
            log.error("could not setup workflow model", ex);
        }
    }

    protected JcrNodeModel getNodeModel() {
        return model;
    }

    private void showWorkflow(final WorkflowsModel model) {
        if (log.isDebugEnabled()) {
            try {
                log.debug("workflow on " + model.getNodeModel().getNode().getPath() + " accoring to renderer "
                        + model.getWorkflowName());
            } catch (RepositoryException ex) {
                log.debug("debug message failed ", ex);
            }
        }

        IPluginConfig wflConfig = new JavaPluginConfig(config.getPluginConfig("workflow.options"));
        String className = model.getWorkflowName();
        wflConfig.put(IPlugin.CLASSNAME, className);
        wflConfig.put(RenderService.MODEL_ID, "${wicket.model}");
        configureWorkflow(wflConfig, model);

        JavaClusterConfig clusterConfig = new JavaClusterConfig();
        clusterConfig.addReference(RenderService.MODEL_ID);
        clusterConfig.addPlugin(wflConfig);

        workflows.add(new Cluster(clusterConfig, model));
    }

    protected void configureWorkflow(IPluginConfig wflConfig, WorkflowsModel model) {
    }

    private void closeWorkflows() {
        for (Cluster entry : workflows) {
            entry.stop();
        }
        workflows = new LinkedList<Cluster>();
    }

    public void onFlush(JcrNodeModel nodeModel) {
        if (model.getItemModel().hasAncestor(nodeModel.getItemModel())) {
            updateModel(model);
        }
    }

    public void detach() {
        model.detach();
    }
}
