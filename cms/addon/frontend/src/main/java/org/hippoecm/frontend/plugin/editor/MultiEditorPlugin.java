/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.plugin.editor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginConfig;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.render.ModelMessage;
import org.hippoecm.frontend.service.render.ModelReference;
import org.hippoecm.frontend.service.topic.Message;
import org.hippoecm.frontend.service.topic.MessageListener;
import org.hippoecm.frontend.service.topic.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiEditorPlugin implements Plugin, IEditService, IFactoryService, MessageListener, Serializable {
    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(MultiEditorPlugin.class);

    public static final String EDITOR_ID = "editor";

    private PluginContext context;
    private Map<String, Object> properties;
    private String factoryId;
    private Map<IModel, EditorPlugin> editors;
    private TopicService topic;
    private int editCount;

    public MultiEditorPlugin() {
        editors = new HashMap<IModel, EditorPlugin>();
        editCount = 0;
    }

    public void start(PluginContext context) {
        this.context = context;
        properties = context.getProperties();

        topic = new TopicService((String) properties.get("editor.model"));
        topic.addListener(this);
        topic.init(context);

        factoryId = (String) properties.get(Plugin.FACTORY_ID);
        if (factoryId != null) {
            context.registerService(this, factoryId);
        }
    }

    public void stop() {
        if (factoryId != null) {
            context.unregisterService(this, factoryId);
        }

        topic.destroy();
        for (Map.Entry<IModel, EditorPlugin> entry : editors.entrySet()) {
            entry.getValue().stop();
            editors.remove(entry.getKey());
        }
    }

    public void edit(final IModel model) {
        EditorPlugin plugin;
        if (!editors.containsKey(model)) {
            PluginConfig config = new PluginConfig();
            config.put(Plugin.SERVICE_ID, properties.get(Plugin.SERVICE_ID));
            config.put(RenderPlugin.DIALOG_ID, properties.get(RenderPlugin.DIALOG_ID));

            config.put(Plugin.FACTORY_ID, factoryId);
            config.put(Plugin.CLASSNAME, EditorPlugin.class.getName());

            String modelId = (String) properties.get(EDITOR_ID) + editCount + ".model";
            config.put(RenderPlugin.MODEL_ID, modelId);

            String decoratorId = (String) properties.get(EDITOR_ID) + editCount + ".decorator";
            config.put(RenderPlugin.DECORATOR_ID, decoratorId);

            plugin = (EditorPlugin) context.start(config);
            plugin.edit(model);

            editors.put(model, plugin);

            editCount++;
        } else {
            plugin = editors.get(model);
        }
        plugin.focus(null);
    }

    public void onMessage(Message message) {
        switch (message.getType()) {
        case ModelReference.SET_MODEL:
            edit(((ModelMessage) message).getModel());
            break;
        }
    }

    public void delete(Serializable service) {
        if (editors.containsValue(service)) {
            for (Map.Entry<IModel, EditorPlugin> entry : editors.entrySet()) {
                if (entry.getValue().equals(service)) {
                    editors.remove(entry.getKey());
                    EditorPlugin plugin = (EditorPlugin) service;
                    plugin.stop();
                    return;
                }
            }
            log.warn("editor " + service + " was not created by this plugin");
        } else {
            log.error("unknown editor " + service + " delete is ignored");
        }
    }

}
