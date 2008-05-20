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
package org.hippoecm.frontend.sa.plugin.workflow;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.sa.core.IPluginConfig;
import org.hippoecm.frontend.sa.core.Plugin;
import org.hippoecm.frontend.sa.core.PluginContext;
import org.hippoecm.frontend.sa.core.impl.PluginConfig;
import org.hippoecm.frontend.sa.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.Message;
import org.hippoecm.frontend.service.render.ModelReference;
import org.hippoecm.frontend.service.topic.MessageListener;
import org.hippoecm.frontend.service.topic.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowPlugin implements Plugin, MessageListener, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(WorkflowPlugin.class);

    public static final String CATEGORIES = "workflow.categories";
    public static final String WORKFLOW_ID = "workflow.display";
    public static final String VIEWER_ID = "workflow.viewer";

    private PluginContext context;
    private IPluginConfig config;
    private String[] categories;
    private String factoryId;
    private Map<String, Plugin> workflows;
    private Map<String, TopicService> models;
    private TopicService topic;
    private int wflCount;

    public WorkflowPlugin() {
        workflows = new HashMap<String, Plugin>();
        models = new HashMap<String, TopicService>();
        wflCount = 0;
        topic = null;
    }

    public void start(PluginContext context) {
        this.context = context;
        config = context.getProperties();

        if (config.get(CATEGORIES) != null) {
            categories = config.getStringArray(CATEGORIES);
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

        if (config.get(RenderPlugin.MODEL_ID) != null) {
            topic = new TopicService(config.getString(RenderPlugin.MODEL_ID));
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

        PluginConfig wflConfig = new PluginConfig();
        wflConfig.put(RenderPlugin.WICKET_ID, config.get(RenderPlugin.WICKET_ID));
        wflConfig.put(RenderPlugin.DIALOG_ID, config.get(RenderPlugin.DIALOG_ID));

        String workflowId = config.getString(WORKFLOW_ID) + wflCount;
        String className = model.getWorkflowName();
        // FIXME: temporary hack to have old and new arch work side-by-side
        if (className.startsWith("org.hippoecm.frontend")) {
            className = "org.hippoecm.frontend.sa" + className.substring("org.hippoecm.frontend".length());
        }
        wflConfig.put(Plugin.CLASSNAME, className);
        wflConfig.put(Plugin.SERVICE_ID, workflowId);
        wflConfig.put(VIEWER_ID, config.get(VIEWER_ID));

        String modelId = workflowId + ".model";
        wflConfig.put(RenderPlugin.MODEL_ID, modelId);

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
        models.put(workflowId, modelTopic);

        context.registerService(this, workflowId + ".factory");

        Plugin plugin = context.start(wflConfig);
        if (plugin != null) {
            workflows.put(workflowId, plugin);
        }
    }

    private void closeWorkflows() {
        for (Map.Entry<String, Plugin> entry : workflows.entrySet()) {
            String workflowId = entry.getKey();
            entry.getValue().stop();

            context.unregisterService(this, workflowId + ".factory");

            TopicService topic = models.get(workflowId);
            topic.destroy();
            models.remove(entry.getKey());
        }
        workflows = new HashMap<String, Plugin>();
    }

}
