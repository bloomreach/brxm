/*
 *  Copyright 2009 Hippo.
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
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateController<C extends IModel> implements IClusterable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TemplateController.class);
    
    private static class Renderer<C extends IModel> implements IClusterable {
        private static final long serialVersionUID = 1L;

        IClusterControl clusterControl;
        ModelReference<C> modelRef;

        Renderer(ModelReference<C> model, IClusterControl control) {
            this.clusterControl = control;
            this.modelRef = model;
        }
    }

    private IPluginContext context;
    @SuppressWarnings("unused")
    private IPluginConfig config;
    private ITemplateFactory<C> factory;
    private Map<C, TemplateController.Renderer<C>> childTemplates;

    public TemplateController(IPluginContext context, IPluginConfig config, ITemplateFactory<C> factory) {
        this.context = context;
        this.config = config;
        this.factory = factory;
        childTemplates = new HashMap<C, TemplateController.Renderer<C>>();
    }

    public void start(AbstractProvider<C> provider) {
        Iterator<C> iter = provider.iterator(0, provider.size());
        while (iter.hasNext()) {
            addModel(iter.next());
        }
    }

    public void stop() {
        for (Map.Entry<C, TemplateController.Renderer<C>> entry : childTemplates.entrySet()) {
            entry.getValue().modelRef.destroy();
            entry.getValue().clusterControl.stop();
        }
        childTemplates.clear();
    }

    public C findModel(IRenderService renderer) {
        for (Map.Entry<C, TemplateController.Renderer<C>> entry : childTemplates.entrySet()) {
            String renderId = entry.getValue().clusterControl.getClusterConfig().getString("wicket.id");
            if (renderer == context.getService(renderId, IRenderService.class)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void addModel(final C model) {
        try {
            IClusterControl control = factory.getTemplate(model);
            String modelId = control.getClusterConfig().getString(RenderService.MODEL_ID);
            ModelReference<C> modelService = new ModelReference<C>(modelId, model);

            modelService.init(context);
            control.start();
            childTemplates.put(model, new Renderer<C>(modelService, control));
        } catch (TemplateEngineException ex) {
            log.error("Failed to open editor for new model", ex);
        }
    }

}