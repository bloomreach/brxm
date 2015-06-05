/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
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
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.plugins.field.AbstractFieldPlugin;
import org.hippoecm.frontend.editor.plugins.fieldhint.FieldHint;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.diff.LCS;
import org.hippoecm.frontend.plugins.standards.diff.LCS.Change;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ModelPath;
import org.hippoecm.frontend.validation.ModelPathElement;
import org.hippoecm.frontend.validation.Violation;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.plugin.api.TaxonomyHelper;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.onehippo.taxonomy.plugin.model.ClassificationDao;
import org.onehippo.taxonomy.plugin.model.ClassificationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that edits the classification for a document.  The storage implementation is delegated to a
 * {@link ClassificationDao}, so this plugin is unaware of any of the taxonomy node types.
 */
public class TaxonomyPickerPlugin extends RenderPlugin<Node> {

    public static final String HIPPOTRANSLATION_TRANSLATED = "hippotranslation:translated";
    public static final String HIPPOTRANSLATION_LOCALE = "hippotranslation:locale";

    static final Logger log = LoggerFactory.getLogger(TaxonomyPickerPlugin.class);

    private class CategoryListView extends RefreshingView<String> {

        public CategoryListView(String id) {
            super(id);
        }

        @Override
        protected Iterator<IModel<String>> getItemModels() {
            if (dao == null) {
                List<IModel<String>> models = Collections.emptyList();
                return models.iterator();
            }
            Classification classification = dao.getClassification(TaxonomyPickerPlugin.this.getModelObject());
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
        protected void populateItem(Item<String> item) {
            Taxonomy taxonomy = getTaxonomy();
            IModel<?> categoryTextModel;

            if (taxonomy != null) {
                Category category = taxonomy.getCategoryByKey(item.getModelObject());
                if (category != null) {
                    categoryTextModel = new Model(TaxonomyHelper.getCategoryName(category, getPreferredLocale()));
                } else {
                    categoryTextModel = new ResourceModel("invalid.taxonomy.category.key");
                }
            } else {
                categoryTextModel = new ResourceModel("invalid.taxonomy.key");
            }

            Label label = new Label("key", categoryTextModel);
            item.add(label);
            label.add(new AttributeModifier("title", categoryTextModel));
        }
    }

    private class CategoryCompareView extends ListView<Change<String>> {

        public CategoryCompareView(String id, IModel<List<Change<String>>> changeModel) {
            super(id, changeModel);
        }

        @Override
        protected void populateItem(ListItem<Change<String>> item) {
            Taxonomy taxonomy = getTaxonomy();
            Change<String> change = item.getModelObject();
            IModel<?> categoryTextModel;

            if (taxonomy != null) {
                Category category = taxonomy.getCategoryByKey(change.getValue());
                if (category != null) {
                    categoryTextModel = new Model(TaxonomyHelper.getCategoryName(category, getPreferredLocale()));
                } else {
                    categoryTextModel = new ResourceModel("invalid.taxonomy.category.key");
                }
            } else {
                categoryTextModel = new ResourceModel("invalid.taxonomy.key");
            }

            Label label = new Label("key", categoryTextModel);
            item.add(label);
            label.add(new AttributeModifier("title", categoryTextModel));

            switch (change.getType()) {
                case ADDED:
                    label.add(new AttributeAppender("class", new Model("hippo-diff-added"), " "));
                    break;
                case REMOVED:
                    label.add(new AttributeAppender("class", new Model("hippo-diff-removed"), " "));
                    break;
            }
        }
    }

    private final Mode mode;
    private ClassificationDao dao;

    public TaxonomyPickerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final String captionKey = config.getString("captionKey", "title");
        add(new Label("title", new ResourceModel(captionKey)));

        final Label requiredMarker = new Label("required", "*");
        final IFieldDescriptor fieldDescriptor = getTaxonomyFieldDescriptor();
        if ((fieldDescriptor == null) || (!fieldDescriptor.getValidators().contains("required"))) {
            requiredMarker.setVisible(false);
        }
        add(requiredMarker);

        add(new FieldHint("hint-panel", config.getString("hint")));

        dao = context.getService(config.getString(ClassificationDao.SERVICE_ID), ClassificationDao.class);
        if (dao == null) {
            log.warn("No DAO found to retrieve classification, using service id: {}",
                    config.getString(ClassificationDao.SERVICE_ID));
        }

        mode = Mode.fromString(config.getString("mode", "view"));
        if (dao != null && mode == Mode.EDIT) {
            add(new CategoryListView("keys"));
            final ClassificationModel model = new ClassificationModel(dao, getModel());
            IDialogFactory dialogFactory = new IDialogFactory() {

                public AbstractDialog<Classification> createDialog() {
                    return createPickerDialog(model, getPreferredLocale());
                }
            };
            add(new DialogLink("edit", new ResourceModel("edit"), dialogFactory, getDialogService())).setEnabled(getTaxonomy() != null);
        } else if (dao != null && mode == Mode.COMPARE && config.containsKey("model.compareTo")) {
            IModel<List<Change<String>>> changesModel = new LoadableDetachableModel<List<Change<String>>>() {

                @SuppressWarnings("unchecked")
                @Override
                protected List<Change<String>> load() {
                    if (dao != null) {
                        IModelReference<Node> baseRef = context.getService(config.getString("model.compareTo"),
                                IModelReference.class);
                        if (baseRef != null) {
                            IModel<Node> baseModel = baseRef.getModel();
                            if (baseModel != null) {
                                List<String> currentKeys = dao.getClassification(getModel().getObject()).getKeys();
                                List<String> baseKeys = dao.getClassification(baseModel.getObject()).getKeys();
                                return LCS.getChangeSet(baseKeys.toArray(new String[baseKeys.size()]), currentKeys
                                        .toArray(new String[currentKeys.size()]));
                            }
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
                Taxonomy taxonomy = getTaxonomy();
                if (taxonomy != null) {
                    Classification classification = dao.getClassification(TaxonomyPickerPlugin.this.getModelObject());
                    return new CanonicalCategory(taxonomy, classification.getCanonical(), getPreferredLocale());
                } else {
                    return null;
                }
            }
        };
        add(new Label("canon", new StringResourceModel("canonical", this, canonicalNameModel)) {

            @Override
            public boolean isVisible() {
                CanonicalCategory canonicalCategory = canonicalNameModel.getObject();
                return canonicalCategory != null && canonicalCategory.getName() != null;
            }

            @Override
            protected void onDetach() {
                canonicalNameModel.detach();
                super.onDetach();
            }
        });

        setOutputMarkupId(true);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(new CssResourceReference(TaxonomyPickerPlugin.class, "res/style.css")));
    }

    @Override
    public void onModelChanged() {
        redraw();
    }

    @Override
    public void render(final PluginRequestTarget target) {

        // in edit mode, check the validation service for any results for this field
        if ((target != null) && isActive()) {
            if (IEditor.Mode.EDIT == mode) {
                if (getPluginConfig().containsKey(IValidationService.VALIDATE_ID)) {

                    final String validatorId = getPluginConfig().getString(IValidationService.VALIDATE_ID);
                    final IValidationService validationService = getPluginContext().getService(validatorId, IValidationService.class);

                    if (validationService != null) {
                        if (!isTaxonomyFieldValid(validationService.getValidationResult())) {
                            target.appendJavaScript("Wicket.$('" + getMarkupId() + "').setAttribute('class', 'invalid');");
                        }
                    }
                    else {
                        log.debug("ValidationService for taxonomy field not found based on {} {}", IValidationService.VALIDATE_ID, validatorId);
                    }
                }
                else {
                    log.debug("Key {} not found in the configuration for taxonomy field, config={}", IValidationService.VALIDATE_ID, getPluginConfig());
                }
            }
        }

        super.render(target);
    }

    /**
     * Creates and returns taxonomy picker dialog instance.
     * <p>
     * If you want to provide a custom taxonomy picker plugin, you might want to
     * override this method.
     * </p>
     */
    protected AbstractDialog<Classification> createPickerDialog(ClassificationModel model, String preferredLocale) {
        return new TaxonomyPickerDialog(getPluginContext(), getPluginConfig(), model, preferredLocale);
    }

    /**
     * Returns the translation locale of the document if exists.
     * Otherwise, returns the user's UI locale as a fallback.
     */
    protected String getPreferredLocale() {
        Node node = getModel().getObject();

        try {
            if (node.isNodeType(HIPPOTRANSLATION_TRANSLATED)) {
                if (node.hasProperty(HIPPOTRANSLATION_LOCALE)) {
                    return node.getProperty(HIPPOTRANSLATION_LOCALE).getString();
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to detect hippotranslation:locale to choose the preferred locale", e);
        }

        return getLocale().getLanguage();
    }

    /**
     * Get the field descriptor object for this field
     */
    protected IFieldDescriptor getTaxonomyFieldDescriptor() {

        final ITemplateEngine templateEngine = getPluginContext().getService(getPluginConfig().getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
        if (templateEngine != null) {
            try {
                final String fieldName = getPluginConfig().getString(AbstractFieldPlugin.FIELD, "keys");

                // get field in current document type, which is either directly configured field or the hippotaxonomy:classifiable
                final ITypeDescriptor type = templateEngine.getType(getModel());
                final IFieldDescriptor field = type.getField(fieldName);
                if (field != null) {
                    return field;
                }
                log.warn("Cannot find taxonomy field '{}' for type {}", fieldName, type.getName());

            } catch (TemplateEngineException e) {
                log.error("Cannot determine type for taxonomy field", e);
            }
        } else {
            log.error("Cannot find template engine, plugin config is {}", getPluginConfig());
        }
        return null;
    }

    protected Taxonomy getTaxonomy() {
        IPluginConfig config = getPluginConfig();
        ITaxonomyService service = getPluginContext()
                .getService(config.getString(ITaxonomyService.SERVICE_ID, ITaxonomyService.DEFAULT_SERVICE_TAXONOMY_ID), ITaxonomyService.class);

        final String taxonomyName = config.getString(ITaxonomyService.TAXONOMY_NAME);

        if (StringUtils.isBlank(taxonomyName)) {
            log.info("No configured/chosen taxonomy name. Found '{}'", taxonomyName);
            return null;
        }

        return service.getTaxonomy(taxonomyName);
    }

    /**
     * Checks if the taxonomy field has any violations attached to it.
     *
     * @param validationResult The IValidationResult that contains all violations that occurred for this editor
     * @return true if there are no violations present or non of the validation belong to the current field
     */
    protected boolean isTaxonomyFieldValid(final IValidationResult validationResult) {

        if (!validationResult.isValid()) {
            final IFieldDescriptor field = getTaxonomyFieldDescriptor();
            if (field == null) {
                return true;
            }

            for (Violation violation : validationResult.getViolations()) {
                Set<ModelPath> paths = violation.getDependentPaths();
                for (ModelPath path : paths) {
                    if (path.getElements().length > 0) {
                        final ModelPathElement first = path.getElements()[0];
                        // matching on path (property name) since the violation comes from the classifiable mixin
                        // and the field can be configured at doc type level
                        if (first.getField().getPath().equals(field.getPath())) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}
