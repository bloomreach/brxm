/*
 *  Copyright 2009-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.attributes.TitleAttribute;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.plugins.field.AbstractFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.FieldPluginHelper;
import org.hippoecm.frontend.editor.plugins.field.ValidationModel;
import org.hippoecm.frontend.editor.plugins.fieldhint.FieldHint;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.diff.LCS;
import org.hippoecm.frontend.plugins.standards.diff.LCS.Change;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ValidatorUtils;
import org.hippoecm.frontend.validation.ViolationUtils;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.plugin.api.TaxonomyHelper;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.onehippo.taxonomy.plugin.model.ClassificationDao;
import org.onehippo.taxonomy.plugin.model.ClassificationModel;
import org.onehippo.taxonomy.util.TaxonomyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.frontend.validation.ViolationUtils.getFirstFieldViolation;

/**
 * Plugin that edits the classification for a document.  The storage implementation is delegated to a
 * {@link ClassificationDao}, so this plugin is unaware of any of the taxonomy node types.
 */
public class TaxonomyPickerPlugin extends RenderPlugin<Node> {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyPickerPlugin.class);

    private static final CssResourceReference CSS = new CssResourceReference(TaxonomyPickerPlugin.class, "style.css");

    private static final String INVALID_TAXONOMY_KEY = "invalid.taxonomy.key";
    private static final String INVALID_TAXONOMY_CATEGORY_KEY = "invalid.taxonomy.category.key";

    private final Mode mode;
    private final ClassificationDao dao;
    private final FieldPluginHelper helper;

    public TaxonomyPickerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        setOutputMarkupId(true);

        mode = Mode.fromString(config.getString("mode", "view"));
        dao = getService(ClassificationDao.SERVICE_ID, ClassificationDao.class);
        if (dao == null) {
            log.warn("No DAO found to retrieve classification for service id {}",
                    config.getString(ClassificationDao.SERVICE_ID));
        }

        ValidationModel validationModel = null;
        if (config.containsKey(IValidationService.VALIDATE_ID)) {
            validationModel = new ValidationModel(context, config);
        }

        final IFieldDescriptor fieldDescriptor = getTaxonomyFieldDescriptor();
        helper = new FieldPluginHelper(context, config, fieldDescriptor, getDocumentTypeDescriptor(), validationModel);

        final Label requiredMarker = new Label("required", "*");
        if (fieldDescriptor == null || !ValidatorUtils.hasRequiredValidator(fieldDescriptor.getValidators())) {
            requiredMarker.setVisible(false);
        }
        add(requiredMarker);

        add(new Label("title", helper.getCaptionModel(this)));
        add(new FieldHint("hint-panel", helper.getHintModel(this)));

        if (dao != null && mode == Mode.EDIT) {
            add(new CategoryListView("keys"));
            final ClassificationModel model = new ClassificationModel(dao, getModel());
            final IDialogFactory dialogFactory = () -> {
                final Locale locale = getPreferredLocale();
                return createTaxonomyPickerDialog(model, locale);
            };
            final DialogLink dialogLink = new DialogLink("edit", new ResourceModel("edit"), dialogFactory, getDialogService());
            final Component ajaxLink = dialogLink.get("dialog-link");
            if (ajaxLink != null) {
                ajaxLink.add(ClassAttribute.append("btn btn-default btn-sm"));
            }
            add(dialogLink);
            setEnabled(getTaxonomy() != null);
        } else if (dao != null && mode == Mode.COMPARE && config.containsKey("model.compareTo")) {
            final IModel<List<Change<String>>> changesModel = new LoadableDetachableModel<List<Change<String>>>() {

                @Override
                protected List<Change<String>> load() {
                    @SuppressWarnings("unchecked")
                    final IModelReference<Node> baseRef = getService("model.compareTo", IModelReference.class);
                    if (baseRef != null) {
                        final IModel<Node> baseModel = baseRef.getModel();
                        if (baseModel != null) {
                            final List<String> currentKeys = dao.getClassification(getModel()).getKeys();
                            final List<String> baseKeys = dao.getClassification(baseModel).getKeys();
                            return LCS.getChangeSet(
                                baseKeys.toArray(new String[0]),
                                currentKeys.toArray(new String[0])
                            );
                        }
                    }
                    return Collections.emptyList();
                }
            };

            add(new CategoryCompareView("keys", changesModel));
            add(new Label("edit", changesModel).setVisible(false));
        } else {
            add(new CategoryListView("keys"));
            add(new Label("edit").setVisible(false));
        }

        final IModel<CanonicalCategory> canonicalNameModel = new LoadableDetachableModel<CanonicalCategory>() {
            @Override
            protected CanonicalCategory load() {
                final Taxonomy taxonomy = getTaxonomy();
                if (taxonomy != null && dao != null) {
                    final Classification classification = dao.getClassification(TaxonomyPickerPlugin.this.getModel());
                    return new CanonicalCategory(taxonomy, classification.getCanonical(), getPreferredLocale());
                } else {
                    return null;
                }
            }
        };
        add(new Label("canon", new StringResourceModel("canonical", this).setModel(canonicalNameModel)) {

            @Override
            public boolean isVisible() {
                final CanonicalCategory canonicalCategory = canonicalNameModel.getObject();
                return canonicalCategory != null && canonicalCategory.getName() != null;
            }

            @Override
            protected void onDetach() {
                canonicalNameModel.detach();
                super.onDetach();
            }
        });
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(CSS));
    }

    @Override
    public void onModelChanged() {
        redraw();
    }

    @Override
    public void render(final PluginRequestTarget target) {
        if (target != null && isActive() && IEditor.Mode.EDIT == mode) {
            final IFieldDescriptor field = helper.getField();
            final IModel<IValidationResult> validationModel = helper.getValidationModel();
            final Optional<ViolationUtils.ViolationMessage> violation = getFirstFieldViolation(field, validationModel);

            if (violation.isPresent()) {
                final String selector = String.format("$('#%s')", getMarkupId());
                target.appendJavaScript(ViolationUtils.getFieldViolationScript(selector, violation.get()));
            }
        }

        super.render(target);
    }

    @Override
    protected void onDetach() {
        helper.detach();
        super.onDetach();
    }

    /**
     * @deprecated This method is deprecated in favor of {@link #createTaxonomyPickerDialog} and will be removed in
     * version 12.00 and onward.
     */
    @Deprecated
    protected AbstractDialog<Classification> createPickerDialog(final ClassificationModel model, final String preferredLocale) {
        return null;
    }

    /**
     * Creates and returns taxonomy picker dialog instance.
     * <p>
     * If you want to provide a custom taxonomy picker plugin, you might want to
     * override this method.
     * </p>
     * @deprecated use {@link #createTaxonomyPickerDialog(ClassificationModel, Locale)} instead
     */
    @Deprecated
    protected Dialog<Classification> createTaxonomyPickerDialog(final ClassificationModel model,
                                                                final String preferredLocale) {
        return new TaxonomyPickerDialog(getPluginContext(), getPluginConfig(), model, preferredLocale);
    }

    /**
     * Creates and returns taxonomy picker dialog instance.
     * <p>
     * If you want to provide a custom taxonomy picker plugin, you might want to
     * override this method.
     * </p>
     */
    protected Dialog<Classification> createTaxonomyPickerDialog(final ClassificationModel model,
                                                                final Locale preferredLocale) {
        return new TaxonomyPickerDialog(getPluginContext(), getPluginConfig(), model, preferredLocale);
    }

    /**
     * Returns the translation locale of the document if exists.
     * Otherwise, returns the user's UI locale as a fallback.
     */
     protected Locale getPreferredLocale() {
        final Node node = getModel().getObject();
        try {
            if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)
                    && node.hasProperty(HippoTranslationNodeType.LOCALE)) {
                return TaxonomyUtil.toLocale(node.getProperty(HippoTranslationNodeType.LOCALE).getString());
            }
        } catch (final RepositoryException e) {
            log.error("Failed to detect " + HippoTranslationNodeType.LOCALE + " to choose the preferred locale", e);
        }

        return getLocale();
    }

    /**
     * Get the document type descriptor object that this field is part of
     */
    protected ITypeDescriptor getDocumentTypeDescriptor() {
        final ITemplateEngine templateEngine = getService(ITemplateEngine.ENGINE, ITemplateEngine.class);
        if (templateEngine != null) {
            try {
                return templateEngine.getType(getModel());
            } catch (final TemplateEngineException e) {
                log.error("Cannot determine type for taxonomy field", e);
            }
        } else {
            log.error("Cannot find template engine, plugin config is {}", getPluginConfig());
        }
        return null;
    }

    /**
     * Get the field descriptor object for this field
     */
    protected IFieldDescriptor getTaxonomyFieldDescriptor() {
        // get field in current document type, which is either directly configured field or the hippotaxonomy:classifiable
        final ITypeDescriptor docType = getDocumentTypeDescriptor();
        if (docType != null) {
            final String fieldName = getPluginConfig().getString(AbstractFieldPlugin.FIELD, "keys");
            final IFieldDescriptor field = docType.getField(fieldName);

            if (field != null) {
                return field;
            }

            log.warn("Cannot find taxonomy field '{}' for type {}", fieldName, docType.getName());
        }

        return null;
    }

    protected Taxonomy getTaxonomy() {
        final ITaxonomyService service = getService(ITaxonomyService.SERVICE_ID,
                ITaxonomyService.DEFAULT_SERVICE_TAXONOMY_ID, ITaxonomyService.class);
        final String taxonomyName = getPluginConfig().getString(ITaxonomyService.TAXONOMY_NAME);

        if (StringUtils.isBlank(taxonomyName)) {
            log.info("No configured/chosen taxonomy name. Found '{}'", taxonomyName);
            return null;
        }

        if (service != null) {
            return service.getTaxonomy(taxonomyName);
        } else {
            log.warn("Taxonomy service not found.");
            return null;
        }
    }

    /**
     * Checks if the taxonomy field has any violations attached to it.
     *
     * @param validationResult The IValidationResult that contains all violations that occurred for this editor
     * @return true if there are no violations present or non of the validation belong to the current field
     *
     * @deprecated This is handled by calling {@link ViolationUtils#getFirstFieldViolation} and checking if a violation
     *             is present
     */
    @Deprecated
    protected boolean isTaxonomyFieldValid(final IValidationResult validationResult) {
        final IFieldDescriptor field = helper.getField();
        return !getFirstFieldViolation(field, Model.of(validationResult)).isPresent() && isContainerValid();
    }

    private boolean isContainerValid() {
        final IFeedbackMessageFilter filter = new ContainerFeedbackMessageFilter(this);
        return !getSession().getFeedbackMessages().hasMessage(filter);
    }

    private <T extends IClusterable> T getService(final String serviceConfigKey, final Class<T> clazz) {
        return getService(serviceConfigKey, null, clazz);
    }

    private <T extends IClusterable> T getService(final String serviceConfigKey, final String defaultServiceId,
                                                  final Class<T> clazz) {
        final IPluginConfig config = getPluginConfig();
        final String serviceId = config.getString(serviceConfigKey, defaultServiceId);
        if (serviceId == null) {
            log.debug("ServiceId not found for key {}, config={}",
                    serviceConfigKey, config);
            return null;
        }

        final IPluginContext context = getPluginContext();
        final T service = context.getService(serviceId, clazz);
        if (service == null) {
            log.debug("Service {} not found for id {}", clazz.getName(), serviceId);
        }
        return service;
    }

    private IModel<String> getCategoryTextModel(final String categoryKey) {
        final Taxonomy taxonomy = getTaxonomy();
        if (taxonomy != null) {
            final Category category = taxonomy.getCategoryByKey(categoryKey);
            if (category != null) {
                return Model.of(TaxonomyHelper.getCategoryName(category, getPreferredLocale()));
            }
            return new ResourceModel(INVALID_TAXONOMY_CATEGORY_KEY);
        }
        return new ResourceModel(INVALID_TAXONOMY_KEY);
    }

    private void addControlsToListItem(final ListItem<?> item) {
        final boolean isEditMode = (mode == Mode.EDIT);

        final Classification classification = dao.getClassification(TaxonomyPickerPlugin.this.getModel());
        final int itemCount = classification.getKeyCount();
        final int itemIndex = item.getIndex();

        final WebMarkupContainer controls = new WebMarkupContainer("controls");
        controls.setVisible(isEditMode);

        final MarkupContainer upLink = new AjaxLink("up") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final String curKey = (String) item.getModelObject();
                if (classification.containsKey(curKey)) {
                    final int curIndex = classification.indexOfKey(curKey);
                    classification.removeKey(curKey);
                    classification.addKey(curIndex - 1, curKey);
                    dao.save(classification);
                    target.add(TaxonomyPickerPlugin.this);
                }
            }
        };
        upLink.setEnabled(isEditMode && itemIndex > 0);
        upLink.setVisible(isEditMode);
        final HippoIcon upIcon = HippoIcon.fromSprite("up-icon", Icon.ARROW_UP);
        upLink.add(upIcon);
        controls.add(upLink);

        final MarkupContainer downLink = new AjaxLink("down") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final String curKey = (String) item.getModelObject();
                if (classification.containsKey(curKey)) {
                    final int curIndex = classification.indexOfKey(curKey);
                    classification.removeKey(curKey);
                    classification.addKey(curIndex + 1, curKey);
                    dao.save(classification);
                    target.add(TaxonomyPickerPlugin.this);
                }
            }
        };
        downLink.setEnabled(isEditMode && itemIndex < itemCount - 1);
        downLink.setVisible(isEditMode);
        final HippoIcon downIcon = HippoIcon.fromSprite("down-icon", Icon.ARROW_DOWN);
        downLink.add(downIcon);
        controls.add(downLink);

        final MarkupContainer removeLink = new AjaxLink("remove") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final String curKey = (String) item.getModelObject();
                if (classification.containsKey(curKey)) {
                    classification.removeKey(curKey);
                    if (classification.isCanonised()) {
                        // change canonical key if current is the one that is removed
                        if (curKey.equals(classification.getCanonical())) {
                            classification.setCanonical(null);
                            final List<String> allKeys = classification.getKeys();
                            if (!allKeys.isEmpty()) {
                                classification.setCanonical(allKeys.get(0));
                            }
                        }
                    }
                    dao.save(classification);
                    target.add(TaxonomyPickerPlugin.this);
                }
            }
        };
        removeLink.setEnabled(isEditMode);
        removeLink.setVisible(isEditMode);
        final HippoIcon removeIcon = HippoIcon.fromSprite("remove-icon", Icon.TIMES);
        removeLink.add(removeIcon);
        controls.add(removeLink);

        item.add(controls);
    }

    private class CategoryListView extends RefreshingView<String> {

        CategoryListView(final String id) {
            super(id);
        }

        @Override
        protected Iterator<IModel<String>> getItemModels() {
            if (dao == null) {
                return Collections.emptyIterator();
            }

            final Classification classification = dao.getClassification(TaxonomyPickerPlugin.this.getModel());
            final Iterator<String> upstream = classification.getKeys().iterator();
            return new Iterator<IModel<String>>() {

                public boolean hasNext() {
                    return upstream.hasNext();
                }

                public IModel<String> next() {
                    return new Model<>(upstream.next());
                }

                public void remove() {
                    upstream.remove();
                }
            };
        }

        @Override
        protected void populateItem(final Item<String> item) {
            final String categoryKey = item.getModelObject();
            final IModel<String> categoryTextModel = getCategoryTextModel(categoryKey);

            final Label label = new Label("key", categoryTextModel);
            item.add(label);
            label.add(TitleAttribute.set(categoryTextModel));

            addControlsToListItem(item);
        }
    }

    private class CategoryCompareView extends ListView<Change<String>> {

        CategoryCompareView(final String id, final IModel<List<Change<String>>> changeModel) {
            super(id, changeModel);
        }

        @Override
        protected void populateItem(final ListItem<Change<String>> item) {
            final Change<String> change = item.getModelObject();
            final String categoryKey = change.getValue();
            final IModel<String> categoryTextModel = getCategoryTextModel(categoryKey);

            final Label label = new Label("key", categoryTextModel);
            item.add(label);
            label.add(TitleAttribute.set(categoryTextModel));
            switch (change.getType()) {
                case ADDED:
                    label.add(ClassAttribute.append("hippo-diff-added"));
                    break;
                case REMOVED:
                    label.add(ClassAttribute.append("hippo-diff-removed"));
                    break;
            }

            addControlsToListItem(item);
        }
    }
}
