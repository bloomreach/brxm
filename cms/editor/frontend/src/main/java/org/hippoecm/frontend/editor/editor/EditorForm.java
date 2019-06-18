/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.impl.TemplateEngineFactory;
import org.hippoecm.frontend.editor.validator.IFeedbackLogger;
import org.hippoecm.frontend.editor.validator.JcrValidationService;
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
import org.hippoecm.frontend.validation.FeedbackPriority;
import org.hippoecm.frontend.validation.FeedbackScope;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ScopedFeedBackMessage;
import org.hippoecm.frontend.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorForm extends HippoForm<Node> implements IFeedbackMessageFilter, IFeedbackLogger {

    private static final Logger log = LoggerFactory.getLogger(EditorForm.class);

    private final IPluginContext context;
    private final IPluginConfig config;

    private IClusterControl cluster;
    private ModelReference modelService;
    private final ServiceTracker<IRenderService> fieldTracker;
    private final List<IRenderService> fields;
    private final TemplateEngineFactory engineFactory;
    private final ITemplateEngine engine;
    private final String engineId;

    private final ValidationService validation;

    public EditorForm(final String wicketId, final JcrNodeModel model, final IRenderService parent,
                      final IPluginContext context, final IPluginConfig config) {
        super(wicketId, model);

        this.context = context;
        this.config = config;

        validation = new ValidationService(context, config);
        validation.start(this);

        if (config.getString(RenderService.FEEDBACK) != null) {
            context.registerService(this, config.getString(RenderService.FEEDBACK));
        } else {
            log.info("No feedback id {} defined", RenderService.FEEDBACK);
        }

        setMultiPart(true);

        engineFactory = new TemplateEngineFactory(null);
        engine = engineFactory.getService(context);
        context.registerService(engineFactory, ITemplateEngine.class.getName());
        engineId = context.getReference(engineFactory).getServiceId();

        fields = new LinkedList<>();
        fieldTracker = new ServiceTracker<IRenderService>(IRenderService.class) {

            @Override
            public void onRemoveService(final IRenderService service, final String name) {
                replace(new EmptyPanel("template"));
                service.unbind();
                fields.remove(service);
            }

            @Override
            public void onServiceAdded(final IRenderService service, final String name) {
                service.bind(parent, "template");
                replace(service.getComponent());
                fields.add(service);
            }
        };
        context.registerTracker(fieldTracker, engineId + ".wicket.root");

        add(new EmptyPanel("template"));
        createTemplate();
    }

    @Override
    public void onValidateModelObjects() {
        // HippoForm#process() has cleared old feedbacks, but in case
        // the validation is invoked from Ajax, they should be cleared again
        clearFeedbackMessages();

        // do the validation
        try {
            validation.doValidate();
            final IValidationResult result = validation.getValidationResult();
            if (!result.isValid()) {
                log.debug("Invalid model {}", getModel());
            }
        } catch (final ValidationException e) {
            log.warn("Failed to validate " + getModel(), e);
        }
    }

    public void destroy() {
        if (cluster != null) {
            cluster.stop();
            modelService.destroy();
        }

        context.unregisterTracker(fieldTracker, engineId + ".wicket.root");
        context.unregisterService(engineFactory, ITemplateEngine.class.getName());
        validation.stop();
    }

    public boolean accept(final FeedbackMessage message) {
        final Component reporter = message.getReporter();
        if (reporter == null) {
            return false;
        }
        boolean inContainerScope = reporter == this || this.contains(reporter, true);
        if (!inContainerScope) {
            return false;
        }
        if (message instanceof ScopedFeedBackMessage) {
            final ScopedFeedBackMessage scopedMessage = (ScopedFeedBackMessage) message;
            return scopedMessage.getFeedbackScope().equals(FeedbackScope.DOCUMENT);
        }
        return false;
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

    public void render(final PluginRequestTarget target) {
        for (final IRenderService child : fields) {
            child.render(target);
        }
    }

    @Override
    // the same logic as in org.apache.wicket.Component.warn
    public void warn(final IModel<String> message, final FeedbackScope scope) {
        getFeedbackMessages().add(new ScopedFeedBackMessage(this, message.getObject(), FeedbackMessage.WARNING, scope));
        addStateChange();
    }

    @Override
    // the same logic as in org.apache.wicket.Component.error
    public void error(final IModel<String> message, final FeedbackScope scope) {
        getFeedbackMessages().add(new ScopedFeedBackMessage(this, message.getObject(), FeedbackMessage.ERROR, scope));
        addStateChange();
    }

    @Override
    public void error(final IModel<String> message, final FeedbackScope scope, final FeedbackPriority priority) {
        // the same logic as in org.apache.wicket.Component.error
        final ScopedFeedBackMessage feedback =
                new ScopedFeedBackMessage(this, message.getObject(), FeedbackMessage.ERROR, scope);
        feedback.setFeedbackPriority(priority);

        getFeedbackMessages().add(feedback);
        addStateChange();
    }

    @Override
    protected void onDetach() {
        final IModel<Node> model = this.getModel();
        if (model != null) {
            model.detach();
        }
        engineFactory.detach();
        if (modelService != null) {
            modelService.detach();
        }
        super.onDetach();
    }

    protected void createTemplate() {
        final JcrNodeModel model = (JcrNodeModel) getModel();
        if (model != null && model.getNode() != null) {
            try {
                final ITypeDescriptor type = engine.getType(model);

                final IClusterConfig template = engine.getTemplate(type, IEditor.Mode.EDIT);
                final IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig("cluster.options"));
                parameters.put(RenderService.WICKET_ID, engineId + ".wicket.root");
                parameters.put(ITemplateEngine.ENGINE, engineId);
                parameters.put(ITemplateEngine.MODE, IEditor.Mode.EDIT.toString());

                cluster = context.newCluster(template, parameters);

                final String modelId = cluster.getClusterConfig().getString(RenderService.MODEL_ID);
                modelService = new ModelReference<>(modelId, model);
                modelService.init(context);

                cluster.start();
            } catch (final TemplateEngineException ex) {
                log.error("Unable to open editor", ex);
            }
        }
    }

    private class ValidationService extends JcrValidationService {

        public ValidationService(final IPluginContext context, final IPluginConfig config) {
            super(context, config);
        }

        private void doValidate() throws ValidationException {
            super.validate();
        }

        @Override
        public void validate() throws ValidationException {
            EditorForm.this.onValidateModelObjects();
        }
    }
}
