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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.IValidateService;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowPlugin implements IPlugin, IModelListener, IJcrNodeModelListener, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(WorkflowPlugin.class);

    public static final String CATEGORIES = "workflow.categories";

    private IPluginContext context;
    private IPluginConfig config;
    private String[] categories;
    private String serviceId;
    private JcrNodeModel model;
    private Map<String, IPluginControl> workflows;
    private Map<String, ModelService> models;
    private int wflCount;

    public WorkflowPlugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        model = new JcrNodeModel((Node) null);
        workflows = new HashMap<String, IPluginControl>();
        models = new HashMap<String, ModelService>();
        wflCount = 0;

        if (config.get(CATEGORIES) != null) {
            categories = config.getStringArray(CATEGORIES);
            if (log.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("workflow showing categories");
                for (String category : categories)
                    sb.append(" "+category);
                log.debug(new String(sb));
            }
        } else {
            log.warn("No categories ({}) defined", CATEGORIES);
        }

        if (config.getString(RenderService.MODEL_ID) != null) {
            context.registerService(this, config.getString(RenderService.MODEL_ID));
        } else {
            log.warn("");
        }

        context.registerService(this, IJcrService.class.getName());

        serviceId = context.getReference(this).getServiceId();
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

        String workflowId = serviceId + "." + (wflCount++);
        String modelId = workflowId + ".model";

        IPluginConfig wflConfig = new JavaPluginConfig();
        wflConfig.put(RenderService.WICKET_ID, config.get(RenderService.WICKET_ID));
        wflConfig.put(RenderService.MODEL_ID, modelId);
        wflConfig.put(RenderService.FEEDBACK, config.get(RenderService.FEEDBACK));
        wflConfig = configureWorkflow(wflConfig, model);

        JavaClusterConfig clusterConfig = new JavaClusterConfig();
        clusterConfig.addPlugin(wflConfig);

        ModelService modelService = new ModelService(modelId, model);
        modelService.init(context);
        models.put(workflowId, modelService);

        IPluginControl plugin = context.start(clusterConfig);

        modelService.resetModel();

        // look up render service
        String controlId = context.getReference(plugin).getServiceId();
        IRenderService renderer = context.getService(controlId, IRenderService.class);

        // register as the factory for the render service
        context.registerService(this, context.getReference(renderer).getServiceId());

        workflows.put(controlId, plugin);
    }

    protected IPluginConfig configureWorkflow(IPluginConfig wflConfig, WorkflowsModel model) {
        String className = model.getWorkflowName();
        wflConfig.put(IPlugin.CLASSNAME, className);
        wflConfig.put(IEditService.EDITOR_ID, config.get(IEditService.EDITOR_ID));
        wflConfig.put(IBrowseService.BROWSER_ID, config.get(IBrowseService.BROWSER_ID));
        wflConfig.put(IValidateService.VALIDATE_ID, config.get(IValidateService.VALIDATE_ID));
        wflConfig.put("translator.id", config.get("translator.id"));
        return wflConfig;
    }

    private void closeWorkflows() {
        for (Map.Entry<String, IPluginControl> entry : workflows.entrySet()) {
            entry.getValue().stopPlugin();
        }
        workflows = new HashMap<String, IPluginControl>();

        for (Map.Entry<String, ModelService> entry : models.entrySet()) {
            ModelService modelService = entry.getValue();
            modelService.destroy();
        }
        models = new HashMap<String, ModelService>();
    }

    public void onFlush(JcrNodeModel nodeModel) {
        if (model.getItemModel().hasAncestor(nodeModel.getItemModel())) {
            updateModel(model);
        }
    }

    public void detach() {
        config.detach();
        model.detach();
    }
}
