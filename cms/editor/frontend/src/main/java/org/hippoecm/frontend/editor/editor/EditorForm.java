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
package org.hippoecm.frontend.editor.editor;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.hippoecm.frontend.PluginRequestTarget;
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
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorForm extends Form<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(EditorForm.class);

    private IPluginContext context;
    private IPluginConfig config;

    private IClusterControl cluster;
    private ModelReference modelService;
    private ServiceTracker<IRenderService> fieldTracker;
    private List<IRenderService> fields;
    private TemplateEngineFactory engineFactory;
    private ITemplateEngine engine;
    private String engineId;

    public EditorForm(String wicketId, JcrNodeModel model, final IRenderService parent, IPluginContext context,
            IPluginConfig config) {
        super(wicketId, model);

        this.context = context;
        this.config = config;

        add(new EmptyPanel("template"));

        setMultiPart(true);

        engineFactory = new TemplateEngineFactory(null);
        engine = engineFactory.getService(context);
        context.registerService(engineFactory, ITemplateEngine.class.getName());
        engineId = context.getReference(engineFactory).getServiceId();

        fields = new LinkedList<IRenderService>();
        fieldTracker = new ServiceTracker<IRenderService>(IRenderService.class) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onRemoveService(IRenderService service, String name) {
                replace(new EmptyPanel("template"));
                service.unbind();
                fields.remove(service);
            }

            @Override
            public void onServiceAdded(IRenderService service, String name) {
                service.bind(parent, "template");
                replace(service.getComponent());
                fields.add(service);
            }
        };
        context.registerTracker(fieldTracker, engineId + ".wicket.root");

        createTemplate();
    }

    public void destroy() {
        context.unregisterService(engineFactory, ITemplateEngine.class.getName());
        context.unregisterTracker(fieldTracker, engineId + ".wicket.root");
        if (cluster != null) {
            cluster.stop();
            modelService.destroy();
        }
    }

    @Override
    protected void onSubmit() {
        super.onSubmit();

        // do the validation
        IValidationService validator = context.getService(config.getString(IValidationService.VALIDATE_ID),
                IValidationService.class);
        if (validator != null) {
            try {
                validator.validate();
                IValidationResult result = validator.getValidationResult();
                if (!result.isValid()) {
                    log.debug("Invalid model {}", getModel());
                }
            } catch (ValidationException e) {
                log.warn("Failed to validate " + getModel());
            }
        } else {
            log.info("No validator configured");
        }
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        if (cluster != null) {
            cluster.stop();
            modelService.destroy();
        }
        createTemplate();
    }

    public void render(PluginRequestTarget target) {
        for (IRenderService child : fields) {
            child.render(target);
        }
    }

    @Override
    protected void onDetach() {
        engineFactory.detach();
        super.onDetach();
    }

    protected void createTemplate() {
        JcrNodeModel model = (JcrNodeModel) getModel();
        if (model != null && model.getNode() != null) {
            try {
                ITypeDescriptor type = engine.getType(model);

                IClusterConfig template = engine.getTemplate(type, IEditor.Mode.EDIT);
                IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig("cluster.options"));
                parameters.put(RenderService.WICKET_ID, engineId + ".wicket.root");
                parameters.put(ITemplateEngine.ENGINE, engineId);
                parameters.put(ITemplateEngine.MODE, IEditor.Mode.EDIT.toString());

                cluster = context.newCluster(template, parameters);

                String modelId = cluster.getClusterConfig().getString(RenderService.MODEL_ID);
                modelService = new ModelReference(modelId, model);
                modelService.init(context);

                cluster.start();
            } catch (TemplateEngineException ex) {
                log.error("Unable to open editor", ex);
            }
        }
    }

}
