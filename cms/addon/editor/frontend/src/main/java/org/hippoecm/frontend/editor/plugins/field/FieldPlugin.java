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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.model.AbstractProvider;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FieldPlugin<P extends IModel, C extends IModel> extends ListViewPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(FieldPlugin.class);

    public static final String FIELD = "field";
    public static final String TYPE = "type";

    protected String mode;
    protected IFieldDescriptor field;
    protected AbstractProvider<C> provider;
    protected String fieldName;
    protected String typeName;

    private TemplateController controller;

    protected FieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        controller = new TemplateController();

        mode = config.getString(ITemplateEngine.MODE);
        if (mode == null) {
            log.error("No edit mode specified");
        }

        typeName = config.getString(FieldPlugin.TYPE);

        fieldName = config.getString(FieldPlugin.FIELD);
        if (fieldName == null) {
            log.error("No field was specified in the configuration");
        } else {
            ITemplateEngine engine = getTemplateEngine();
            if (engine != null) {
                ITypeDescriptor type;
                try {
                    if (typeName == null) {
                        type = engine.getType(getDefaultModel());
                    } else {
                        type = engine.getType(typeName);
                    }
                    field = type.getField(fieldName);
                    if (field == null) {
                        log.error("Could not resolve field");
                    }
                } catch (TemplateEngineException tee) {
                    log.error("Could not resolve field", tee);
                }
            } else {
                log.error("No template engine available");
            }
        }
    }

    @Override
    protected String getItemId() {
        String serviceId = getPluginContext().getReference(this).getServiceId();
        return serviceId + ".item";
    }

    @Override
    protected void onDetach() {
        if (provider != null) {
            provider.detach();
        }
        if (field instanceof IDetachable) {
            ((IDetachable) field).detach();
        }
        super.onDetach();
    }

    protected void updateProvider() {
        if (field != null) {
            ITemplateEngine engine = getTemplateEngine();
            if (engine != null) {
                P model = (P) getDefaultModel();
                try {
                    ITypeDescriptor subType = engine.getType(field.getType());
                    provider = newProvider(field, subType, model);
                    if (provider != null) {
                        controller.stop();
                        controller.start(provider);
                    }
                } catch (TemplateEngineException ex) {
                    log.warn("Unable to obtain type descriptor for " + model, ex);
                }
            } else {
                log.warn("No engine found to display new model");
            }
        } else {
            setVisible(false);
        }
    }

    protected abstract AbstractProvider<C> newProvider(IFieldDescriptor descriptor, ITypeDescriptor type, P parentModel);

    protected boolean canRemoveItem() {
        if (!ITemplateEngine.EDIT_MODE.equals(mode) || (field == null) || !field.isMultiple()) {
            return false;
        }
        return true;
    }

    protected boolean canReorderItems() {
        if (!ITemplateEngine.EDIT_MODE.equals(mode) || field == null || !field.isMultiple() || !field.isOrdered()) {
            return false;
        }
        return true;
    }

    public void onAddItem(AjaxRequestTarget target) {
        provider.addNew();
    }

    public void onRemoveItem(C childModel, AjaxRequestTarget target) {
        provider.remove(childModel);
    }

    public void onMoveItemUp(C model, AjaxRequestTarget target) {
        provider.moveUp(model);
        // reorderings are not detected by the observation manager, so do a hard refresh
        updateProvider();
        redraw();
    }

    protected ITemplateEngine getTemplateEngine() {
        return getPluginContext()
                .getService(getPluginConfig().getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
    }

    protected IClusterControl getTemplate(C model) throws TemplateEngineException {
        ITemplateEngine engine = getTemplateEngine();
        IClusterConfig template = engine.getTemplate(engine.getType(field.getType()), mode);

        IPluginConfig parameters = new JavaPluginConfig(getPluginConfig().getPluginConfig("cluster.options"));
        parameters.put(ITemplateEngine.ENGINE, getPluginConfig().getString(ITemplateEngine.ENGINE));
        parameters.put(RenderService.WICKET_ID, getItemId());
        parameters.put(ITemplateEngine.MODE, mode);

        return getPluginContext().newCluster(template, parameters);
    }

    protected C findModel(IRenderService renderer) {
        return controller.findModel(renderer);
    }

    private static class Renderer implements IClusterable {
        IClusterControl clusterControl;
        ModelReference modelRef;

        Renderer(ModelReference model, IClusterControl control) {
            this.clusterControl = control;
            this.modelRef = model;
        }
    }

    private class TemplateController implements IClusterable {
        private static final long serialVersionUID = 1L;

        private Map<C, Renderer> childTemplates;

        TemplateController() {
            childTemplates = new HashMap<C, Renderer>();
        }

        void start(AbstractProvider<C> provider) {
            Iterator<C> iter = provider.iterator(0, provider.size());
            while (iter.hasNext()) {
                addModel(iter.next());
            }
        }

        void stop() {
            for (Map.Entry<C, Renderer> entry : childTemplates.entrySet()) {
                entry.getValue().modelRef.destroy();
                entry.getValue().clusterControl.stop();
            }
            childTemplates.clear();
        }

        C findModel(IRenderService renderer) {
            for (Map.Entry<C, Renderer> entry : childTemplates.entrySet()) {
                IPluginContext context = getPluginContext();
                String renderId = entry.getValue().clusterControl.getClusterConfig().getString("wicket.id");
                if (renderer == context.getService(renderId, IRenderService.class)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        private void addModel(final C model) {
            try {
                IClusterControl control = FieldPlugin.this.getTemplate(model);
                String modelId = control.getClusterConfig().getString(RenderService.MODEL_ID);
                ModelReference modelService = new ModelReference(modelId, model);

                modelService.init(getPluginContext());
                control.start();
                childTemplates.put(model, new Renderer(modelService, control));
            } catch (TemplateEngineException ex) {
                log.error("Failed to open editor for new model", ex);
            }
        }

    }

}
