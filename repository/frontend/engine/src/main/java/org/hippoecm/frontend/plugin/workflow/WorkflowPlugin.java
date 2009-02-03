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

import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowPlugin implements IPlugin, IObserver, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(WorkflowPlugin.class);

    private class Cluster implements IClusterable {
        private static final long serialVersionUID = 1L;

        ModelReference modelService;
        IClusterControl control;

        Cluster(IClusterConfig clusterConfig, IModel model) {
            control = context.newCluster(clusterConfig, null);

            String modelId = control.getClusterConfig().getString("wicket.model");
            modelService = new ModelReference(modelId, model);
            modelService.init(context);

            control.start();
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
    private final IModelReference modelReference;
    private JcrNodeModel model;
    private List<Cluster> workflows;

    public WorkflowPlugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

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
            modelReference = context.getService(config.getString(RenderService.MODEL_ID),
                    IModelReference.class);
            if (modelReference != null) {
                updateModel(modelReference.getModel());
                context.registerService(new IObserver() {
                    private static final long serialVersionUID = 1L;

                    public IObservable getObservable() {
                        return modelReference;
                    }

                    public void onEvent(IEvent event) {
                        if (event instanceof IModelReference.IModelChangeEvent) {
                            IModelReference.IModelChangeEvent<JcrNodeModel> mce = (IModelReference.IModelChangeEvent<JcrNodeModel>) event;
                            updateModel(mce.getNewModel());
                        }
                    }
                    
                }, IObserver.class.getName());
            }
        } else {
            modelReference = null;
            log.warn("No model configured");
        }
    }

    // implement IModelListener
    public void updateModel(IModel imodel) {
        closeWorkflows();

        if (imodel != model && (imodel == null || !imodel.equals(model))) {
            // unregister and re-register; observer model is changed 
            if (model != null) {
                context.unregisterService(this, IObserver.class.getName());
            }
            model = (JcrNodeModel) imodel;
            if (model != null) {
                context.registerService(this, IObserver.class.getName());
            }
        } else {
            model = (JcrNodeModel) imodel;
        }
        if (model == null || model.getNode() == null) {
            return;
        }

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

    public IObservable getObservable() {
        // FIXME: should this be an ancestor of the model?  (the handle if it exists?)
        return model;
    }

    public void onEvent(IEvent event) {
        updateModel(model);
    }

    public void detach() {
        if (model != null) {
            model.detach();
        }
    }

}
