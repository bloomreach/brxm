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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.Observer;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFieldPlugin<P extends Item, C extends IModel> extends ListViewPlugin<P> implements
        ITemplateFactory<C> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static abstract class ValidationFilter extends Model<String> {
        private static final long serialVersionUID = 1L;

        private boolean valid = true;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public abstract void onValidation(IValidationResult result);

        @Override
        public String getObject() {
            if (valid) {
                return "";
            } else {
                return "invalid";
            }
        }
    }

    static final Logger log = LoggerFactory.getLogger(AbstractFieldPlugin.class);

    public static final String FIELD = "field";
    public static final String TYPE = "type";

    protected String mode;
    protected AbstractProvider<C> provider;

    private FieldPluginHelper helper;
    private TemplateController<C> controller;

    private boolean managedValidation = false;
    private Map<Object, ValidationFilter> listeners = new HashMap<Object, ValidationFilter>();

    private boolean restartTemplates = true;

    protected AbstractFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        helper = new FieldPluginHelper(context, config);
        if (helper.getValidationModel() != null && helper.getValidationModel() instanceof IObservable) {
            context.registerService(new Observer((IObservable) helper.getValidationModel()) {
                private static final long serialVersionUID = 1L;

                public void onEvent(Iterator events) {
                    for (ValidationFilter listener : new ArrayList<ValidationFilter>(listeners.values())) {
                        listener.onValidation(helper.getValidationModel().getObject());
                    }
                }

            }, IObserver.class.getName());

        }
        controller = new TemplateController<C>(context, config, helper.getValidationModel(), this);

        mode = config.getString(ITemplateEngine.MODE);
        if (mode == null) {
            log.error("No edit mode specified");
        }

        provider = getProvider();

        IFieldDescriptor field = helper.getField();
        if (field != null && !doesTemplateSupportValidation()) {
            final ValidationFilter holder = new ValidationFilter() {
                private static final long serialVersionUID = 1L;

                @Override
                public void onValidation(IValidationResult validation) {
                    boolean valid = true;
                    if (!validation.isValid()) {
                        IFieldDescriptor field = getFieldHelper().getField();
                        for (Violation violation : validation.getViolations()) {
                            Set<ModelPath> paths = violation.getDependentPaths();
                            for (ModelPath path : paths) {
                                if (path.getElements().length > 0) {
                                    ModelPathElement first = path.getElements()[0];
                                    if (first.getField().equals(field)) {
                                        valid = false;
                                    }
                                    break;
                                }
                            }
                            if (!valid) {
                                break;
                            }
                        }
                    }
                    if (valid != isValid()) {
                        redraw();
                        setValid(valid);
                    }
                }

            };
            IModel<IValidationResult> validationModel = helper.getValidationModel();
            if (validationModel != null && validationModel.getObject() != null) {
                holder.setValid(validationModel.getObject().isValid());
            }
            addValidationFilter(this, holder);

            managedValidation = true;
            if (!field.isMultiple()) {
                add(new CssClassAppender(holder));
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
        helper.detach();
        super.onDetach();
    }

    protected void resetValidation() {
        for (ValidationFilter listener : listeners.values()) {
            listener.setValid(true);
        }
    }
    
    @Override
    protected void redraw() {
        super.redraw();
        if (!restartTemplates) {
            restartTemplates = true;
            controller.stop();
        }
    }

    @Override
    protected void onBeforeRender() {
        if (restartTemplates) {
            provider = getProvider();
            if (provider != null) {
                setVisible(true);
                controller.start(provider);
            } else {
                setVisible(false);
            }
            restartTemplates = false;
        }
        super.onBeforeRender();
    }

    private AbstractProvider<C> getProvider() {
        IFieldDescriptor field = helper.getField();
        if (field != null) {
            ITemplateEngine engine = getTemplateEngine();
            if (engine != null) {
                IModel<P> model = getModel();
                ITypeDescriptor subType = field.getTypeDescriptor();
                AbstractProvider<C> provider = newProvider(field, subType, model);
                if (ITemplateEngine.EDIT_MODE.equals(mode) && provider.size() == 0) {
                    provider.addNew();
                }
                return provider;
            } else {
                log.warn("No engine found to display new model");
            }
        }
        return null;
    }

    /**
     * Factory method for provider of models that will be used to instantiate templates.
     * This method may be called from the base class constructor.
     * 
     * @param descriptor
     * @param type
     * @param parentModel
     * @return
     */
    protected abstract AbstractProvider<C> newProvider(IFieldDescriptor descriptor, ITypeDescriptor type,
            IModel<P> parentModel);

    protected boolean canAddItem() {
        IFieldDescriptor field = getFieldHelper().getField();
        return ITemplateEngine.EDIT_MODE.equals(mode) && (field != null)
                && (field.isMultiple() || provider.size() == 0);
    }

    protected boolean canRemoveItem() {
        IFieldDescriptor field = helper.getField();
        if (!ITemplateEngine.EDIT_MODE.equals(mode) || (field == null))
            return false;
        if (!field.isMultiple()) {
            return false;
        }
        if (field.getValidators().contains("required") && provider.size() == 1) {
            return false;
        }
        return true;
    }

    protected boolean canReorderItems() {
        IFieldDescriptor field = helper.getField();
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
        redraw();
    }

    private void addValidationFilter(Object key, ValidationFilter listener) {
        listeners.put(key, listener);
    }

    private void removeValidationFilter(Object key) {
        listeners.remove(key);
    }

    @Override
    protected void onAddRenderService(final org.apache.wicket.markup.repeater.Item<IRenderService> item,
            IRenderService renderer) {
        super.onAddRenderService(item, renderer);

        final FieldItemRenderer<C> itemRenderer = getController().findItemRenderer(renderer);
        if (managedValidation && getFieldHelper().getField().isMultiple()) {
            item.setOutputMarkupId(true);
            ValidationFilter listener = new ValidationFilter() {
                private static final long serialVersionUID = 1L;

                @Override
                public void onValidation(IValidationResult result) {
                    boolean valid = itemRenderer.isValid();
                    if (valid != this.isValid()) {
                        AjaxRequestTarget target = AjaxRequestTarget.get();
                        if (target != null) {
                            target.addComponent(item);
                        }
                        setValid(valid);
                    }
                }
            };
            listener.setValid(itemRenderer.isValid());
            addValidationFilter(item, listener);
            item.add(new CssClassAppender(listener));
        }
    }

    @Override
    protected void onRemoveRenderService(org.apache.wicket.markup.repeater.Item<IRenderService> item,
            IRenderService renderer) {
        removeValidationFilter(item);
        super.onRemoveRenderService(item, renderer);
    }

    protected FieldPluginHelper getFieldHelper() {
        return helper;
    }

    protected IModel<String> getCaptionModel() {
        IFieldDescriptor field = getFieldHelper().getField();
        String caption = getPluginConfig().getString("caption", "unknown");
        String captionKey = field != null ? field.getName() : caption;
        return new StringResourceModel(captionKey, this, null, caption);
    }

    protected TemplateController<C> getController() {
        return controller;
    }

    protected ITemplateEngine getTemplateEngine() {
        return getPluginContext()
                .getService(getPluginConfig().getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
    }

    protected boolean doesTemplateSupportValidation() {
        ITemplateEngine engine = getTemplateEngine();
        IFieldDescriptor field = helper.getField();
        try {
            IClusterConfig template = engine.getTemplate(field.getTypeDescriptor(), mode);
            return (template.getReferences().contains(IValidationService.VALIDATE_ID));
        } catch (TemplateEngineException e) {
            return false;
        }
    }

    public IClusterControl getTemplate(C model) throws TemplateEngineException {
        ITemplateEngine engine = getTemplateEngine();
        IFieldDescriptor field = helper.getField();
        IClusterConfig template = engine.getTemplate(field.getTypeDescriptor(), mode);

        IPluginConfig parameters = new JavaPluginConfig(getPluginConfig().getPluginConfig("cluster.options"));
        parameters.put(ITemplateEngine.ENGINE, getPluginConfig().getString(ITemplateEngine.ENGINE));
        parameters.put(RenderService.WICKET_ID, getItemId());
        parameters.put(ITemplateEngine.MODE, mode);

        return getPluginContext().newCluster(template, parameters);
    }

}
