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
package org.hippoecm.frontend.editor.viewer;

import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.impl.TemplateEngine;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeStore;
import org.hippoecm.frontend.plugins.standardworkflow.types.JcrTypeStore;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;

public class ViewerPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private ModelService modelService;
    private IPluginControl template;
    private String engineId;
    private ITypeStore typeStore;
    private TemplateEngine engine;

    public ViewerPlugin(IPluginContext context, IPluginConfig properties) {
        super(context, properties);

        typeStore = new JcrTypeStore(RemodelWorkflow.VERSION_CURRENT);
        engine = new TemplateEngine(context, typeStore);
        context.registerService(engine, ITemplateEngine.class.getName());
        engineId = context.getReference(engine).getServiceId();
        engine.setId(engineId);

        addExtensionPoint("template");

        modelChanged();
    }

    @Override
    public void onDetach() {
        typeStore.detach();
        super.onDetach();
    }

    @Override
    public void onModelChanged() {
        if (template != null) {
            modelService.destroy();
            template.stopPlugin();
        }
        createTemplate();
        redraw();
    }

    protected void createTemplate() {
        JcrNodeModel model = (JcrNodeModel) getModel();
        ITypeDescriptor type = engine.getType(model);
        IPluginContext context = getPluginContext();

        if (type != null) {
            IClusterConfig clusterConfig = engine.getTemplate(type, "view");
            if (clusterConfig != null) {
                clusterConfig.put(RenderService.WICKET_ID, getPluginConfig().getString("template"));
                String modelId = clusterConfig.getString(RenderService.MODEL_ID);
                modelService = new ModelService(modelId, model);
                modelService.init(context);
                template = context.start(clusterConfig);
            }
        }
    }

}
