/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.compare.IComparer;
import org.hippoecm.frontend.editor.compare.NodeComparer;
import org.hippoecm.frontend.editor.compare.ObjectComparer;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.Observer;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.service.IEditor;
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

public abstract class AbstractFieldPlugin<P extends Item, C extends IModel> extends ListViewPlugin<Node> implements
        ITemplateFactory<C> {

    private static final String CLUSTER_OPTIONS = "cluster.options";
    private static final String MAX_ITEMS = "maxitems";

    private static final int DEFAULT_MAX_ITEMS = 0;
    private static final String HIPPO_TYPES = "hippo:types";

    private final int maxItems;
    private IPluginConfig parameters;

    abstract static class ValidationFilter extends Model<String> {
        private static final String INVALID = "invalid";

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
            return valid ? "" : INVALID;
        }
    }

    static final Logger log = LoggerFactory.getLogger(AbstractFieldPlugin.class);

    public static final String FIELD = "field";
    public static final String TYPE = "type";

    protected IEditor.Mode mode;
    private boolean restartTemplates = true;

    // view and edit modes
    protected AbstractProvider<P, C> provider;
    protected FieldPluginHelper helper;
    private TemplateController<P, C> templateController;
    private boolean managedValidation = false;
    private Map<Object, ValidationFilter> listeners = new HashMap<>();
    private ValidationFilter filter;

    // compare mode
    private IModel<Node> compareTo;
    protected AbstractProvider<P, C> oldProvider;
    protected AbstractProvider<P, C> newProvider;
    private ComparingController<P, C> comparingController;

    // each validator service id for a started clusters must be unique
    int validatorCount = 0;

    protected AbstractFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        this.parameters = new JavaPluginConfig(config.getPluginConfig(CLUSTER_OPTIONS));
        this.maxItems = config.getInt(MAX_ITEMS, DEFAULT_MAX_ITEMS);

        helper = new FieldPluginHelper(context, config);
        if (helper.getValidationModel() != null && helper.getValidationModel() instanceof IObservable) {
            context.registerService(new Observer<IObservable>((IObservable) helper.getValidationModel()) {
                public void onEvent(Iterator events) {
                    for (ValidationFilter listener : new ArrayList<>(listeners.values())) {
                        IValidationResult validationResult = helper.getValidationModel().getObject();
                        if (validationResult != null) {
                            listener.onValidation(validationResult);
                        }
                    }
                }

            }, IObserver.class.getName());

        }
        mode = IEditor.Mode.fromString(config.getString(ITemplateEngine.MODE), IEditor.Mode.VIEW);
        if (mode == IEditor.Mode.COMPARE) {
            if (config.containsKey("model.compareTo")) {
                @SuppressWarnings("unchecked")
                IModelReference<Node> compareToModelRef = context.getService(config.getString("model.compareTo"),
                        IModelReference.class);
                if (compareToModelRef != null) {
                    // TODO: add observer
                    compareTo = compareToModelRef.getModel();
                    if (compareTo == null) {
                        log.warn("compareTo model is null, falling back to view mode");
                    }
                } else {
                    log.warn("No compareTo.model configured, falling back to view mode");
                }
            } else {
                log.warn("No compareTo model reference for field plugin in editor cluster that implements compare mode, field " + config.getString("field"));
            }
            if (compareTo == null) {
                mode = IEditor.Mode.VIEW;
            }
        }

        if (IEditor.Mode.COMPARE == mode) {
            comparingController = new ComparingController<>(context, config, this, getComparer(), getItemId());
        } else {
            IModel<IValidationResult> validationModel = null;
            if (IEditor.Mode.EDIT == mode) {
                validationModel = helper.getValidationModel();
            }
            templateController = new TemplateController<>(context, config, validationModel, this,
                    getItemId());

            provider = getProvider(getModel());

            IFieldDescriptor field = helper.getField();
            if (field != null && !doesTemplateSupportValidation()) {
                filter = new ValidationFilter() {
                    @Override
                    public void onValidation(IValidationResult validation) {
                        // nothing; is updated on render
                    }

                };
                if (validationModel != null && validationModel.getObject() != null) {
                    IValidationResult validationResult = validationModel.getObject();
                    filter.setValid(isFieldValid(validationResult));
                }

                managedValidation = true;
                if (!field.isMultiple()) {
                    add(new CssClassAppender(filter));
                }
            }
        }
    }

    @Override
    public void render(final PluginRequestTarget target) {
        if (isActive()) {
            if (IEditor.Mode.EDIT == mode && filter != null) {
                IModel<IValidationResult> validationModel = helper.getValidationModel();
                if (validationModel != null && validationModel.getObject() != null) {
                    filter.setValid(isFieldValid(validationModel.getObject()));
                }
                if (target != null) {
                    final String element = "$('#" + getMarkupId() + "')";
                    if (filter.isValid()) {
                        target.appendJavaScript(element + ".removeClass('" + ValidationFilter.INVALID + "');");
                    } else {
                        target.appendJavaScript(element + ".addClass('" + ValidationFilter.INVALID + "');");
                    }
                }
            }
        }
        super.render(target);
    }

    /**
     * Checks if a field has any violations attached to it.
     *
     * @param validation The IValidationResult that contains all violations that occurred for this editor
     * @return true if there are no violations present or non of the validation belong to the current field
     */
    private boolean isFieldValid(final IValidationResult validation) {
        if (!validation.isValid()) {
            IFieldDescriptor field = getFieldHelper().getField();
            if (field == null) {
                return false;
            }
            for (Violation violation : validation.getViolations()) {
                Set<ModelPath> paths = violation.getDependentPaths();
                for (ModelPath path : paths) {
                    if (path.getElements().length > 0) {
                        ModelPathElement first = path.getElements()[0];
                        if (first.getField().equals(field)) {
                            return false;
                        }
                    }
                }
            }
        }
        IFeedbackMessageFilter filter = new ContainerFeedbackMessageFilter(this);
        return !getSession().getFeedbackMessages().hasMessage(filter);
    }

    protected IComparer<?> getComparer() {
        IComparer comparer;
        IFieldDescriptor field = helper.getField();
        if (field != null) {
            ITypeDescriptor type = field.getTypeDescriptor();
            if (type.isNode()) {
                comparer = new NodeComparer(type, helper.getTemplateEngine());
            } else {
                comparer = new ObjectComparer();
            }
        } else {
            comparer = new ObjectComparer();
        }
        return comparer;
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
        if (oldProvider != null) {
            oldProvider.detach();
        }
        if (newProvider != null) {
            newProvider.detach();
        }
        helper.detach();
        if (templateController != null) {
            templateController.detach();
        }
        if (comparingController != null) {
            comparingController.detach();
        }
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
            if (templateController != null) {
                templateController.stop();
            } else {
                comparingController.stop();
            }
        }
    }

    @Override
    protected void onBeforeRender() {
        if (restartTemplates) {
            if (templateController != null) {
                provider = getProvider(getModel());
                if (provider != null) {
                    setVisible(true);
                    templateController.start(provider);
                } else {
                    setVisible(false);
                }
            } else if (comparingController != null) {
                oldProvider = getProvider(compareTo);
                newProvider = getProvider(getModel());
                comparingController.start(oldProvider, newProvider);
            }
            restartTemplates = false;
        }
        super.onBeforeRender();
    }

    private AbstractProvider<P, C> getProvider(IModel<Node> model) {
        IFieldDescriptor field = helper.getField();
        if (field != null) {
            ITemplateEngine engine = getTemplateEngine();
            if (engine != null) {
                ITypeDescriptor subType = field.getTypeDescriptor();
                AbstractProvider<P, C> provider = newProvider(field, subType, model);

                if (IEditor.Mode.EDIT == mode && provider.size() == 0
                        && (!field.isMultiple() || field.getValidators().contains("required"))
                        && !field.getValidators().contains("optional")
                        && isNotAbstractNodeType(subType.getType())) {
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
     * Factory method for provider of models that will be used to instantiate templates. This method may be called from
     * the base class constructor.
     *
     * @param descriptor
     * @param type
     * @param parentModel
     * @return
     */
    protected abstract AbstractProvider<P, C> newProvider(IFieldDescriptor descriptor, ITypeDescriptor type,
                                                          IModel<Node> parentModel);

    protected boolean canAddItem() {
        IFieldDescriptor field = getFieldHelper().getField();
        if (IEditor.Mode.EDIT == mode && field != null) {
            if (field.isMultiple()) {
                if (getMaxItems() > 0) {
                    return getNumberOfItems() < getMaxItems();
            }
                return true;
            }

            return getNumberOfItems() == 0;
        }

        return false;
    }

    protected boolean canRemoveItem() {
        IFieldDescriptor field = helper.getField();
        if (IEditor.Mode.EDIT != mode || field == null) {
            return false;
        }
        if (!field.isMultiple() && !field.getValidators().contains("optional")) {
            return false;
        }
        if (field.getValidators().contains("required") && provider.size() == 1) {
            return false;
        }
        return true;
    }

    protected boolean canReorderItems() {
        IFieldDescriptor field = helper.getField();
        if (IEditor.Mode.EDIT != mode || field == null || !field.isMultiple() || !field.isOrdered()) {
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

        final FieldItem<C> itemRenderer = templateController != null ? templateController.getFieldItem(renderer) : null;
        final C itemModel = itemRenderer != null ? itemRenderer.getModel() : null;

        switch (mode) {
            case VIEW:
                populateViewItem(item, itemModel);
                break;
            case EDIT:
                final IFieldDescriptor field = getFieldHelper().getField();
                if (managedValidation && field != null && field.isMultiple() && itemRenderer != null) {
                    item.setOutputMarkupId(true);
                    ValidationFilter listener = new ValidationFilter() {
                        @Override
                        public void onValidation(IValidationResult result) {
                            boolean valid = itemRenderer.isValid();
                            if (valid != this.isValid()) {
                                AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                                if (target != null) {
                                    target.add(item);
                                }
                                setValid(valid);
                            }
                        }
                    };
                    listener.setValid(itemRenderer.isValid());
                    addValidationFilter(item, listener);
                    item.add(new CssClassAppender(listener));
                }
                populateEditItem(item, itemModel);
                break;
            case COMPARE:
                final ComparingController<P, C>.ItemEntry itemEntry = comparingController != null
                        ? comparingController.getFieldItem(renderer) : null;
                final C newModel = (itemEntry != null) ? itemEntry.newModel : null;
                final C oldModel = (itemEntry != null) ? itemEntry.oldModel : null;
                populateCompareItem(item, newModel, oldModel);
                break;
        }
    }

    @Override
    protected final void onRemoveRenderService(org.apache.wicket.markup.repeater.Item<IRenderService> item,
                                               IRenderService renderer) {
        removeValidationFilter(item);
        super.onRemoveRenderService(item, renderer);
    }

    protected void populateEditItem(org.apache.wicket.markup.repeater.Item<IRenderService> item, C model) {
    }

    protected void populateViewItem(org.apache.wicket.markup.repeater.Item<IRenderService> item, C model) {
    }

    protected void populateCompareItem(org.apache.wicket.markup.repeater.Item<IRenderService> item, C newModel, C oldModel) {
    }

    protected FieldPluginHelper getFieldHelper() {
        return helper;
    }

    protected ITemplateEngine getTemplateEngine() {
        return getPluginContext()
                .getService(getPluginConfig().getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
    }

    protected boolean doesTemplateSupportValidation() {
        ITemplateEngine engine = getTemplateEngine();
        IFieldDescriptor field = helper.getField();
        if (field != null) {
            try {
                IClusterConfig template = engine.getTemplate(field.getTypeDescriptor(), mode);
                return (template.getReferences().contains(IValidationService.VALIDATE_ID));
            } catch (TemplateEngineException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean canCompare(final IModel<?> old, final IModel<?> model) {
        ITemplateEngine engine = getTemplateEngine();
        try {
            final ITypeDescriptor oldType = engine.getType(old);
            final ITypeDescriptor newType = engine.getType(model);
            return oldType.equals(newType);
        } catch (TemplateEngineException e) {
            return false;
        }

    }

    public IClusterControl newTemplate(String id, IEditor.Mode mode, IModel<?> model) throws TemplateEngineException {
        if (mode == null) {
            mode = this.mode;
        }
        ITemplateEngine engine = getTemplateEngine();
        IFieldDescriptor field = helper.getField();
        if (field == null) {
            throw new TemplateEngineException("No field available to locate template");
        }
        IClusterConfig template;
        try {
            template = engine.getTemplate(field.getTypeDescriptor(), mode);
        } catch (TemplateEngineException ex) {
            if (IEditor.Mode.COMPARE == mode) {
                template = engine.getTemplate(field.getTypeDescriptor(), IEditor.Mode.VIEW);
            } else {
                throw ex;
            }
        }

        this.parameters.put(ITemplateEngine.ENGINE, getPluginConfig().getString(ITemplateEngine.ENGINE));
        this.parameters.put(RenderService.WICKET_ID, id);
        this.parameters.put(ITemplateEngine.MODE, mode.toString());
        this.parameters.put(IValidationService.VALIDATE_ID, getPluginContext().getReference(this).getServiceId() + ".validator." + (validatorCount++));

        return getPluginContext().newCluster(template, parameters);
    }

    protected Component createNrItemsLabel() {
        if ((IEditor.Mode.EDIT == mode) && (getMaxItems() > 0)) {
            final IModel propertyModel = new StringResourceModel("nrItemsLabel", this)
                    .setModel(new Model<AbstractFieldPlugin>(this));
            return new Label("nrItemsLabel", propertyModel).setOutputMarkupId(true);
        }
        return new Label("nrItemsLabel").setVisible(false);
    }

    public int getMaxItems() {
        return maxItems;
    }

    public int getNumberOfItems() {
        return provider.size();
    }

    private boolean isNotAbstractNodeType(final String type) {
        try {
            final NodeTypeManager nodeTypeManager = getSession().getJcrSession().getWorkspace().getNodeTypeManager();
            if (nodeTypeManager.hasNodeType(type)) {
                return !nodeTypeManager.getNodeType(type).isAbstract();
            }
        } catch (RepositoryException e) {
            log.error("Exception determining whether type " + type + " is abstract", e);
        }
        return true;
    }
}
