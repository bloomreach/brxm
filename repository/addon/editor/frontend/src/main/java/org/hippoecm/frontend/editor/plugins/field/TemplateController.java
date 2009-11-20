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
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateController<C extends IModel> implements IClusterable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    private IPluginContext context;
    @SuppressWarnings("unused")
    private IPluginConfig config;
    private ITemplateFactory<C> factory;
    private IModel<IValidationResult> validationModel;
    private Map<C, FieldItemRenderer<C>> childTemplates;

    public TemplateController(IPluginContext context, IPluginConfig config, IModel<IValidationResult> validationModel,
            ITemplateFactory<C> factory) {
        this.context = context;
        this.config = config;
        this.validationModel = validationModel;
        this.factory = factory;
        childTemplates = new HashMap<C, FieldItemRenderer<C>>();
    }

    public void start(AbstractProvider<C> provider) {
        Iterator<C> iter = provider.iterator(0, provider.size());
        while (iter.hasNext()) {
            C model = iter.next();
            addModel(model, provider.getFieldElement(model));
        }
    }

    public void stop() {
        for (Map.Entry<C, FieldItemRenderer<C>> entry : childTemplates.entrySet()) {
            FieldItemRenderer<C> renderer = entry.getValue();
            renderer.destroy();
        }
        childTemplates.clear();
    }

    public FieldItemRenderer<C> findItemRenderer(IRenderService renderer) {
        for (Map.Entry<C, FieldItemRenderer<C>> entry : childTemplates.entrySet()) {
            String renderId = entry.getValue().getRendererId();
            if (renderer == context.getService(renderId, IRenderService.class)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void addModel(final C model, ModelPathElement element) {
        try {
            IClusterControl control = factory.getTemplate(model);
            childTemplates.put(model, new FieldItemRenderer<C>(context, model, validationModel, control, element));
        } catch (TemplateEngineException ex) {
            log.error("Failed to open editor for new model", ex);
        }
    }

}