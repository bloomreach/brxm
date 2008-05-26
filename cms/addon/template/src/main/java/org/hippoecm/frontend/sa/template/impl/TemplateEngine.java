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
package org.hippoecm.frontend.sa.template.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.plugin.IPlugin;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.sa.plugin.impl.RenderPlugin;
import org.hippoecm.frontend.sa.service.Message;
import org.hippoecm.frontend.sa.service.render.ModelReference;
import org.hippoecm.frontend.sa.service.render.ModelReference.ModelMessage;
import org.hippoecm.frontend.sa.service.topic.TopicService;
import org.hippoecm.frontend.sa.template.ITemplateConfig;
import org.hippoecm.frontend.sa.template.ITemplateEngine;
import org.hippoecm.frontend.sa.template.ITemplateStore;
import org.hippoecm.frontend.sa.template.ITypeStore;
import org.hippoecm.frontend.sa.template.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEngine implements ITemplateEngine {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private class TemplateConfigWrapper extends JavaPluginConfig implements ITemplateConfig {
        private static final long serialVersionUID = 1L;

        private ITemplateConfig upstream;
        private Map<String, String> variables;

        TemplateConfigWrapper(ITemplateConfig upstream) {
            this.upstream = upstream;

            variables = new HashMap<String, String>();
            String templateId = serviceId + "." + (templateCount++);
            variables.put(ITemplateEngine.TEMPLATE, templateId);

            put(ITemplateEngine.ENGINE, serviceId);
        }

        public List<IPluginConfig> getPlugins() {
            return upstream.getPlugins();
        }

        public List<String> getPropertyKeys() {
            return upstream.getPropertyKeys();
        }

        @Override
        public Object get(Object key) {
            Object obj = super.get(key);
            if (obj != null) {
                return obj;
            }

            obj = upstream.get(key);
            if ((obj != null) && (obj instanceof String)) {
                // Intercept values of the form "{" + variable + "}" + ...
                // These values are rewritten using the variables
                String value = (String) obj;
                if (value.charAt(0) == '{') {
                    String variable = value.substring(1, value.indexOf('}'));
                    Object origValue = variables.get(variable);
                    if (origValue != null) {
                        String result = origValue + value.substring(value.indexOf('}') + 1);
                        log.debug("Rewriting value {} to {}", value, result);
                        return result;
                    } else {
                        log.warn("Unknown variable {} used in key {}", variable, key);
                    }
                }
            }
            return obj;
        }

    }

    private IPluginContext context;
    private ITypeStore typeStore;
    private ITemplateStore templateStore;
    private String serviceId;
    private int templateCount = 0;

    public TemplateEngine(IPluginContext context, String serviceId, ITypeStore typeStore,
            ITemplateStore templateStore) {
        this.context = context;
        this.typeStore = typeStore;
        this.templateStore = templateStore;
        this.serviceId = serviceId;
    }

    public TypeDescriptor getType(String type) {
        return typeStore.getTypeDescriptor(type);
    }

    public TypeDescriptor getType(IModel model) {
        if (model instanceof JcrNodeModel) {
            try {
                JcrNodeModel nodeModel = (JcrNodeModel) model;
                return getType(nodeModel.getNode().getPrimaryNodeType().getName());
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("Unable to resolve type of {}", model);
        }
        return null;
    }

    public ITemplateConfig getTemplate(TypeDescriptor type, String mode) {
        return new TemplateConfigWrapper(templateStore.getTemplate(type, mode));
    }

    public IPlugin start(final ITemplateConfig template, final IModel model) {
        String modelId = template.getString(RenderPlugin.MODEL_ID);
        final TopicService topic = new TopicService(modelId) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onPublish(Message message) {
                switch (message.getType()) {
                case ModelReference.GET_MODEL:
                    publish(new ModelMessage(ModelReference.SET_MODEL, model));
                }
            }
        };
        topic.init(context);

        List<IPluginConfig> configs = template.getPlugins();
        final IPlugin[] plugins = new IPlugin[configs.size()];
        int i = 0;
        for (final IPluginConfig conf : configs) {
            plugins[i++] = context.start(new JavaPluginConfig() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object get(Object key) {
                    Object obj = conf.get(key);
                    if ((obj != null) && (obj instanceof String)) {
                        // values of the form scope + ":" + ... refer to keys in other scopes 
                        // Only the "template" scope is recognized.
                        String value = (String) obj;
                        if (value.indexOf(':') > 0) {
                            String scope = value.substring(0, value.indexOf(':'));
                            if ("template".equals(scope)) {
                                return template.get(value.substring(value.indexOf(':') + 1));
                            } else {
                                log.warn("Unknown scope {} used in key {}", scope, key);
                            }
                        }
                    }
                    return obj;
                }

                @Override
                public Object put(Object key, Object value) {
                    return conf.put(key, value);
                }
            });
        }

        return new IPlugin() {
            private static final long serialVersionUID = 1L;

            public void start(IPluginContext context) {
            }

            public void stop() {
                for (IPlugin plugin : plugins) {
                    plugin.stop();
                }
                topic.destroy();
            }
        };
    }
}
