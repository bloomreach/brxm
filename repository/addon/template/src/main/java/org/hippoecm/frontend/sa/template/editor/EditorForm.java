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
package org.hippoecm.frontend.sa.template.editor;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.util.lang.Bytes;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.plugin.IPlugin;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.impl.RenderPlugin;
import org.hippoecm.frontend.sa.service.IRenderService;
import org.hippoecm.frontend.sa.service.PluginRequestTarget;
import org.hippoecm.frontend.sa.service.ServiceTracker;
import org.hippoecm.frontend.sa.template.ITemplateConfig;
import org.hippoecm.frontend.sa.template.ITemplateEngine;
import org.hippoecm.frontend.sa.template.ITemplateStore;
import org.hippoecm.frontend.sa.template.ITypeStore;
import org.hippoecm.frontend.sa.template.TypeDescriptor;
import org.hippoecm.frontend.sa.template.config.JcrTemplateStore;
import org.hippoecm.frontend.sa.template.config.JcrTypeStore;
import org.hippoecm.frontend.sa.template.impl.TemplateEngine;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;

public class EditorForm extends Form {
    private static final long serialVersionUID = 1L;

    private IPluginContext context;
    private IPluginConfig config;

    private IPlugin template;
    private ServiceTracker<IRenderService> fieldTracker;
    private TemplateEngine engine;
    private String engineId;

    public EditorForm(String wicketId, JcrNodeModel model, final IRenderService parent, IPluginContext context,
            IPluginConfig config) {
        super(wicketId, model);

        this.context = context;
        this.config = config;

        add(new EmptyPanel("template"));

        setMultiPart(true);
        // FIXME: make this configurable
        setMaxSize(Bytes.megabytes(5));

        engineId = config.getString(IPlugin.SERVICE_ID) + ".engine";
        ITypeStore typeStore = new JcrTypeStore(RemodelWorkflow.VERSION_CURRENT);
        ITemplateStore templateConfig = new JcrTemplateStore();
        engine = new TemplateEngine(context, engineId, typeStore, templateConfig);
        context.registerService(engine, engineId);

        fieldTracker = new ServiceTracker<IRenderService>(IRenderService.class);
        fieldTracker.addListener(new ServiceTracker.IListener<IRenderService>() {
            private static final long serialVersionUID = 1L;

            public void onRemoveService(String name, IRenderService service) {
                replace(new EmptyPanel("template"));
                service.unbind();
            }

            public void onServiceAdded(String name, IRenderService service) {
                service.bind(parent, "template");
                replace((Component) service);
            }

            public void onServiceChanged(String name, IRenderService service) {
            }
        });
        fieldTracker.open(context, engineId + ".wicket.root");

        template = createTemplate();
    }

    public void destroy() {
        context.unregisterService(engine, config.getString(ITemplateEngine.ENGINE));
        fieldTracker.close();
        if (template != null) {
            template.stop();
        }
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        if (template != null) {
            template.stop();
        }
        template = createTemplate();
    }

    public void render(PluginRequestTarget target) {
        for (IRenderService child : fieldTracker.getServices()) {
            child.render(target);
        }
    }

    protected IPlugin createTemplate() {
        JcrNodeModel model = (JcrNodeModel) getModel();
        TypeDescriptor type = engine.getType(model);

        if (type != null) {
            ITemplateConfig templateConfig = engine.getTemplate(type, ITemplateStore.EDIT_MODE);
            templateConfig.put(RenderPlugin.DIALOG_ID, config.getString(RenderPlugin.DIALOG_ID));
            templateConfig.put(RenderPlugin.WICKET_ID, engineId + ".wicket.root");
            return engine.start(templateConfig, model);
        } else {
            return null;
        }
    }

}
