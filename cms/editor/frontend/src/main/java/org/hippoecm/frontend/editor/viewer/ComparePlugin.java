/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.impl.TemplateEngineFactory;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
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

        add(CssClass.append("hippo-compare-plugin"));
    }

    @Override
    protected void onStart() {
        Task onStartTask = null;

        try {
            if (HDC.isStarted()) {
                onStartTask = HDC.getCurrentTask().startSubtask("ComparePlugin.onStart");
                addModelInfoToDiagnosticsTaskAttributes(HDC.getCurrentTask());
            }

            super.onStart();
            modelChanged();
        } finally {
            if (onStartTask != null) {
                onStartTask.stop();
            }
        }
    }

    @Override
    public void onModelChanged() {
        Task onModelChangedTask = null;

        try {
            if (HDC.isStarted()) {
                onModelChangedTask = HDC.getCurrentTask().startSubtask("ComparePlugin.onModelChanged");
                addModelInfoToDiagnosticsTaskAttributes(HDC.getCurrentTask());
            }

            if (cluster != null) {
                modelService.destroy();
                modelService = null;
                cluster.stop();
                cluster = null;
            }
            createTemplate();
            redraw();
        } finally {
            if (onModelChangedTask != null) {
                onModelChangedTask.stop();
            }
        }
    }

    protected void createTemplate() {
        final IModel model = getDefaultModel();
        if (model != null && model.getObject() != null) {
            try {
                ITypeDescriptor type = engine.getType(model);
                IPluginContext context = getPluginContext();

                IClusterConfig template = engine.getTemplate(type, IEditor.Mode.COMPARE);
                IPluginConfig parameters = new JavaPluginConfig();
                parameters.put(RenderService.WICKET_ID, getPluginConfig().getString("template"));
                parameters.put(ITemplateEngine.ENGINE, engineId);

                final Mode editorMode = Mode.fromString(getPluginConfig().getString("mode"), Mode.VIEW);
                if (editorMode == Mode.COMPARE && template.getReferences().contains("model.compareTo")) {
                    parameters.put(ITemplateEngine.MODE, Mode.COMPARE.toString());
                    parameters.put("model.compareTo", getPluginConfig().get("model.compareTo"));
                } else {
                    parameters.put(ITemplateEngine.MODE, Mode.VIEW.toString());
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

    private void addModelInfoToDiagnosticsTaskAttributes(final Task task) {
        final JcrNodeModel model = (JcrNodeModel) getDefaultModel();

        if (model != null) {
            try {
                final Node node = model.getNode();
                if (node != null) {
                    task.setAttribute("editorModelType", node.getPrimaryNodeType().getName());
                    task.setAttribute("editorModelPath", node.getPath());
                }
            } catch (Exception e) {
                log.error("Failed to get model info of the EditorForm.", e);
            }
        }
    }
}
