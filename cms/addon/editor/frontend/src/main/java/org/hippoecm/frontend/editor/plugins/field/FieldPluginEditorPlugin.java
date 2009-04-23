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
package org.hippoecm.frontend.editor.plugins.field;

import java.util.List;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.builder.FieldEditor;
import org.hippoecm.frontend.editor.builder.RenderPluginEditorPlugin;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigListener;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldPluginEditorPlugin extends RenderPluginEditorPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FieldPluginEditorPlugin.class);

    private static final String TEMPLATE_PARAMETER_EDITOR = "templateParameterEditor";
    private static final String FIELD_PLUGIN_EDITOR = "fieldPluginEditor";
    private static final String FIELD_EDITOR = "fieldEditor";

    static class PropertyEditor extends RenderService {
        private static final long serialVersionUID = 1L;

        private ITypeDescriptor type;
        private IPluginConfig edited;
        private boolean shown = true;

        public PropertyEditor(IPluginContext context, IPluginConfig properties, ITemplateEngine engine,
                IPluginConfig edited, ITypeDescriptor type, boolean edit) {
            super(context, properties);

            this.type = type;
            this.edited = edited;

            // Field editor
            IFieldDescriptor descriptor = getFieldDescriptor();
            if (descriptor != null) {
                add(new FieldEditor(FIELD_EDITOR, type, new Model(getFieldDescriptor()), edit));
                add(new FieldPluginEditor(FIELD_PLUGIN_EDITOR, new Model(edited), edit));

                String subType = descriptor.getType();
                IClusterConfig target = engine.getTemplate(engine.getType(subType), "edit");
                add(new TemplateParameterEditor(TEMPLATE_PARAMETER_EDITOR, getClusterParameters(edit), target, edit));
            } else {
                add(new EmptyPanel(FIELD_EDITOR));
                add(new EmptyPanel(FIELD_PLUGIN_EDITOR));
                add(new EmptyPanel(TEMPLATE_PARAMETER_EDITOR));
            }
        }

        void show(boolean show) {
            if (show != shown) {
                if (shown) {
                    getPluginContext().unregisterService(this, getPluginConfig().getString("wicket.id"));
                } else {
                    getPluginContext().registerService(this, getPluginConfig().getString("wicket.id"));
                }
                shown = show;
            }
        }

        private IModel getClusterParameters(boolean edit) {
            if (edit && edited.getPluginConfig("cluster.options") == null) {
                edited.put("cluster.options", new JavaPluginConfig());
            }
            return new Model(edited.getPluginConfig("cluster.options"));
        }

        private IFieldDescriptor getFieldDescriptor() {
            if (type != null) {
                return type.getField(edited.getString("field"));
            }
            return null;
        }

    }

    private PropertyEditor helper;
    private boolean edit;

    public FieldPluginEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        edit = config.getBoolean("builder.mode");

        IPluginConfig editedConfig = getEditedPluginConfig();
        editedConfig.addPluginConfigListener(new IPluginConfigListener() {
            private static final long serialVersionUID = 1L;

            public void onPluginConfigChanged() {
                onModelChanged();
            }
        });

        ITypeDescriptor type = getTypeModel();
        IPluginConfig helperConfig = new JavaPluginConfig(config);
        helperConfig.put("wicket.id", config.getString("wicket.helper.id"));
        helper = new PropertyEditor(getPluginContext(), helperConfig, getTemplateEngine(), getEditedPluginConfig(),
                type, edit);

        final String pluginId = config.getString("plugin.id");
        final IModelReference helperModelRef = context.getService(config.getString("model.plugin"),
                IModelReference.class);
        if (helperModelRef != null) {
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return helperModelRef;
                }

                public void onEvent(IEvent event) {
                    helper.show(helperModelRef.getModel() != null
                            && pluginId.equals(helperModelRef.getModel().getObject()));
                }

            }, IObserver.class.getName());
            helper.show(helperModelRef.getModel() != null
                    && pluginId.equals(helperModelRef.getModel().getObject()));
        } else {
            log.error("No model.plugin model reference found to select active plugin");
        }
    }

    private ITemplateEngine getTemplateEngine() {
        return getPluginContext().getService(getEffectivePluginConfig().getString("engine"), ITemplateEngine.class);
    }

    private IPluginConfig getEditedPluginConfig() {
        IClusterConfig clusterConfig = (IClusterConfig) getModelObject();
        List<IPluginConfig> plugins = clusterConfig.getPlugins();
        for (IPluginConfig plugin : plugins) {
            if (plugin.getName().equals(getPluginConfig().getString("plugin.id"))) {
                return plugin;
            }
        }
        throw new RuntimeException("Could not find plugin in cluster");
    }

    private ITypeDescriptor getTypeModel() {
        IPluginContext context = getPluginContext();
        IPluginConfig config = getEffectivePluginConfig();

        IModelReference reference = context.getService(config.getString("wicket.model"), IModelReference.class);
        ITemplateEngine engine = getTemplateEngine();
        return engine.getType(reference.getModel());
    }

}
