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
package org.hippoecm.frontend.plugin.workflow;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.impl.PluginConfig;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.config.ConfigValue;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.render.ModelReference;
import org.hippoecm.frontend.service.topic.Message;
import org.hippoecm.frontend.service.topic.MessageListener;
import org.hippoecm.frontend.service.topic.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowPlugin implements Plugin, MessageListener, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(WorkflowPlugin.class);

    public static final String CATEGORIES = "workflow.categories";
    public static final String WORKFLOW_ID = "workflow.display";

    private PluginContext context;
    private Map<String, ParameterValue> properties;
    private List<String> categories;
    private String factoryId;
    private Map<WorkflowsModel, Plugin> workflows;
    private Map<WorkflowsModel, TopicService> models;
    private TopicService topic;
    private int wflCount;

    public WorkflowPlugin() {
        workflows = new HashMap<WorkflowsModel, Plugin>();
        models = new HashMap<WorkflowsModel, TopicService>();
        wflCount = 0;
        topic = null;
    }

    public void start(PluginContext context) {
        this.context = context;
        properties = context.getProperties();

        if (properties.get(Plugin.FACTORY_ID) != null) {
            factoryId = properties.get(Plugin.FACTORY_ID).getStrings().get(0);
        }

        if (properties.get(CATEGORIES) != null) {
            categories = properties.get(CATEGORIES).getStrings();
            if (log.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("workflow showing categories");
                for (String category : categories)
                    sb.append(category);
                log.debug(new String(sb));
            }
        } else {
            log.warn("No categories ({}) defined for {}", CATEGORIES, factoryId);
        }

        if (properties.get(RenderPlugin.MODEL_ID) != null) {
            topic = new TopicService(properties.get(RenderPlugin.MODEL_ID).getStrings().get(0));
            topic.addListener(this);
            topic.init(context);
        } else {
            log.warn("");
        }
    }

    public void stop() {
        if (topic != null) {
            topic.destroy();
            topic = null;
        }
        closeWorkflows();
    }

    public void select(JcrNodeModel model) {
        closeWorkflows();
        try {
            WorkflowsModel workflowsModel = new WorkflowsModel(model, categories);
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

    public void onMessage(Message message) {
        switch (message.getType()) {
        case ModelReference.SET_MODEL:
            select((JcrNodeModel) ((ModelReference.ModelMessage) message).getModel());
            break;
        }
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

        PluginConfig config = new PluginConfig();
        config.put(Plugin.SERVICE_ID, properties.get(Plugin.SERVICE_ID));
        config.put(RenderPlugin.DIALOG_ID, properties.get(RenderPlugin.DIALOG_ID));

        config.put(Plugin.CLASSNAME, new ConfigValue(model.getWorkflowName()));

        String workflowId = properties.get(WORKFLOW_ID).getStrings().get(0);
        String modelId = workflowId + wflCount + ".model";
        config.put(RenderPlugin.MODEL_ID, new ConfigValue(modelId));

        TopicService modelTopic = new TopicService(modelId);
        modelTopic.addListener(new MessageListener() {
            private static final long serialVersionUID = 1L;

            public void onMessage(Message message) {
                switch (message.getType()) {
                case ModelReference.GET_MODEL:
                    message.getSource().onPublish(new ModelReference.ModelMessage(ModelReference.SET_MODEL, model));
                    break;
                }
            }
        });
        modelTopic.init(context);
        models.put(model, modelTopic);

        String decoratorId = workflowId + wflCount + ".decorator";
        config.put(RenderPlugin.DECORATOR_ID, new ConfigValue(decoratorId));

        Plugin plugin = context.start(config);
        if (plugin != null) {
            workflows.put(model, plugin);
        }
    }

    private void closeWorkflows() {
        for (Map.Entry<WorkflowsModel, Plugin> entry : workflows.entrySet()) {
            entry.getValue().stop();

            TopicService topic = models.get(entry.getKey());
            topic.destroy();
            models.remove(entry.getKey());
        }
        workflows = new HashMap<WorkflowsModel, Plugin>();
    }

}
