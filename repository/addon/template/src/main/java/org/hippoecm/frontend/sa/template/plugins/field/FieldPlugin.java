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
package org.hippoecm.frontend.sa.template.plugins.field;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugins.standardworkflow.types.FieldDescriptor;
import org.hippoecm.frontend.sa.plugins.standardworkflow.types.TypeDescriptor;
import org.hippoecm.frontend.sa.template.ITemplateEngine;
import org.hippoecm.frontend.sa.template.model.AbstractProvider;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FieldPlugin<P extends IModel, C extends IModel> extends ListViewPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(FieldPlugin.class);

    public static final String FIELD = "field";

    protected String mode;
    protected FieldDescriptor field;
    protected AbstractProvider<C> provider;

    private String fieldName;
    private TemplateController controller;

    protected FieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        controller = new TemplateController();

        mode = config.getString(ITemplateEngine.MODE);
        if (mode == null) {
            log.error("No edit mode specified");
        }

        fieldName = config.getString(FieldPlugin.FIELD);
        if (fieldName == null) {
            log.error("No field was specified in the configuration");
        }
    }

    @Override
    protected void onDetach() {
        if (provider != null) {
            provider.detach();
        }
        super.onDetach();
    }

    protected void updateProvider() {
        ITemplateEngine engine = getTemplateEngine();
        if (engine != null) {
            P model = (P) getModel();
            TypeDescriptor type = engine.getType(model);
            if (type != null) {
                field = type.getField(fieldName);
                if (field != null) {
                    TypeDescriptor subType = engine.getType(field.getType());
                    provider = newProvider(field, subType, model);
                    if (provider != null) {
                        controller.stop();
                        controller.start(provider);
                    }
                } else {
                    log.warn("Unknown field {} in type {}", fieldName, type.getName());
                }
            } else {
                log.warn("Unable to obtain type descriptor for {}", model);
            }
        } else {
            log.warn("No engine found to display new model");
        }
    }

    protected abstract AbstractProvider<C> newProvider(FieldDescriptor descriptor, TypeDescriptor type, P parentModel);

    public void onAddItem(AjaxRequestTarget target) {
        provider.addNew();

        controller.update();

        // refresh
        modelChanged();
    }

    public void onRemoveItem(C childModel, AjaxRequestTarget target) {
        provider.remove(childModel);

        controller.update();

        // refresh
        modelChanged();
    }

    public void onMoveItemUp(C model, AjaxRequestTarget target) {
        provider.moveUp(model);

        // FIXME: support reordering
        // controller.update();
        modelChanged();
    }

    protected ITemplateEngine getTemplateEngine() {
        return getPluginContext()
                .getService(getPluginConfig().getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
    }

    protected void configureTemplate(IClusterConfig config, C model) {
        final IPluginConfig myConfig = getPluginConfig();

        for (String property : config.getOverrides()) {
            Object value = myConfig.get("template." + property);
            if (value != null) {
                config.put(property, value);
            }
        }

        config.put(RenderService.WICKET_ID, myConfig.getString(ITEM));
        config.put(RenderService.DIALOG_ID, myConfig.getString(RenderService.DIALOG_ID));
    }

    private class TemplateController implements IClusterable {
        private static final long serialVersionUID = 1L;

        private Map<C, IPluginControl> plugins;
        private Map<C, ModelService> models;

        TemplateController() {
            plugins = new HashMap<C, IPluginControl>();
            models = new HashMap<C, ModelService>();
        }

        void start(AbstractProvider<C> provider) {
            Iterator<C> iter = provider.iterator(0, provider.size());
            while (iter.hasNext()) {
                addModel(iter.next());
            }
        }

        void update() {
            Set<C> current = Collections.unmodifiableSet(plugins.keySet());
            Iterator<C> iter = provider.iterator(0, provider.size());
            Set<C> newModels = new HashSet<C>();
            while (iter.hasNext()) {
                C model = iter.next();
                if (!current.contains(model)) {
                    addModel(model);
                }
                newModels.add(model);
            }
            for (C model : current) {
                if (!newModels.contains(model)) {
                    removeModel(model);
                }
            }
        }

        void stop() {
            for (C model : Collections.unmodifiableSet(plugins.keySet())) {
                removeModel(model);
            }
        }

        private void addModel(final C model) {
            ITemplateEngine engine = getTemplateEngine();
            IClusterConfig config = engine.getTemplate(engine.getType(field.getType()), mode);
            FieldPlugin.this.configureTemplate(config, model);

            String modelId = config.getString(RenderService.MODEL_ID);
            ModelService modelService = new ModelService(modelId, model);
            models.put(model, modelService);

            IPluginContext context = getPluginContext();
            modelService.init(context);
            IPluginControl plugin = context.start(config);
            plugins.put(model, plugin);
        }

        private void removeModel(C model) {
            IPluginControl plugin = plugins.remove(model);
            if (plugin != null) {
                plugin.stopPlugin();
            }
            ModelService modelService = models.remove(model);
            if (modelService != null) {
                modelService.destroy();
            }
        }
    }

}
