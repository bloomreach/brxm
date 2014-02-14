/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.viewer;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.impl.TemplateEngineFactory;
import org.hippoecm.frontend.editor.resources.CmsEditorHeaderItem;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComparePlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ComparePlugin.class);

    private ModelReference modelService;
    private IClusterControl cluster;
    private String engineId;
    private ITemplateEngine engine;

    public ComparePlugin(IPluginContext context, IPluginConfig properties) {
        super(context, properties);

        TemplateEngineFactory factory = new TemplateEngineFactory(null);
        engine = factory.getService(context);
        context.registerService(factory, ITemplateEngine.class.getName());
        engineId = context.getReference(factory).getServiceId();

        addExtensionPoint("template");
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CmsEditorHeaderItem.get());
    }

    @Override
    protected void onStart() {
        super.onStart();
        modelChanged();
    }

    @Override
    public void onModelChanged() {
        if (cluster != null) {
            modelService.destroy();
            modelService = null;
            cluster.stop();
            cluster = null;
        }
        createTemplate();
        redraw();
    }

    protected void createTemplate() {
        IModel model = getDefaultModel();
        if (model != null && model.getObject() != null) {
            try {
                ITypeDescriptor type = engine.getType(model);
                IPluginContext context = getPluginContext();

                IClusterConfig template = engine.getTemplate(type, IEditor.Mode.COMPARE);
                IPluginConfig parameters = new JavaPluginConfig();
                parameters.put(RenderService.WICKET_ID, getPluginConfig().getString("template"));
                parameters.put(ITemplateEngine.ENGINE, engineId);

                String editorMode = getPluginConfig().getString("mode", "view");
                if (IEditor.Mode.fromString(editorMode) == IEditor.Mode.COMPARE &&
                        template.getReferences().contains("model.compareTo")) {
                    parameters.put(ITemplateEngine.MODE, "compare");
                    parameters.put("model.compareTo", getPluginConfig().get("model.compareTo"));
                } else {
                    parameters.put(ITemplateEngine.MODE, "view");
                }

                cluster = context.newCluster(template, parameters);
                String modelId = cluster.getClusterConfig().getString(RenderService.MODEL_ID);
                if (modelId != null) {
                    modelService = new ModelReference(modelId, model);
                    modelService.init(context);
                    cluster.start();
                } else {
                    log.warn("No model specified in template for type " + type.getName());
                }
            } catch (TemplateEngineException ex) {
                log.error("Unable to open template", ex);
            }
        }
    }

}
