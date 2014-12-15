/*
 *  Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
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
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.diff.LCS;
import org.hippoecm.frontend.plugins.standards.diff.LCS.Change;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.render.RenderPlugin;
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

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TaxonomyPickerPlugin.class);

    private class CategoryListView extends RefreshingView<String> {

        private static final long serialVersionUID = 1L;

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
                    return new Model<String>(upstream.next());
                }

                public void remove() {
                    upstream.remove();
                }
            };
        }

        @Override
        protected void populateItem(Item<String> item) {
            Taxonomy taxonomy = getTaxonomy();
            if (taxonomy != null) {
                Category category = taxonomy.getCategoryByKey(item.getModelObject());
                if (category != null) {
                    item.add(new Label("key", new Model(TaxonomyHelper.getCategoryName(category, getPreferredLocale()))));
                } else {
                    item.add(new Label("key", new ResourceModel("invalid.taxonomy.category.key")));
                }
            } else {
                item.add(new Label("key", new ResourceModel("invalid.taxonomy.key")));
            }

        }
    }

    private class CategoryCompareView extends ListView<Change<String>> {

        private static final long serialVersionUID = 1L;

        public CategoryCompareView(String id, IModel<List<Change<String>>> changeModel) {
            super(id, changeModel);
        }

        @Override
        protected void populateItem(ListItem<Change<String>> item) {
            Taxonomy taxonomy = getTaxonomy();
            Change<String> change = item.getModelObject();
            Label label;
            if (taxonomy != null) {
                Category category = taxonomy.getCategoryByKey(change.getValue());
                if (category != null) {
                    item.add(label = new Label("key", new Model(TaxonomyHelper.getCategoryName(category, getPreferredLocale()))));
                } else {
                    item.add(label = new Label("key", new ResourceModel("invalid.taxonomy.category.key")));
                }
            } else {
                item.add(label = new Label("key", new ResourceModel("invalid.taxonomy.key")));
            }
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

    private ClassificationDao dao;

    public TaxonomyPickerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        String captionKey = config.getString("captionKey", "title");
        add(new Label("title", new ResourceModel(captionKey)));

        dao = context.getService(config.getString(ClassificationDao.SERVICE_ID), ClassificationDao.class);
        if (dao == null) {
            log.warn("No DAO found to retrieve classification, using service id: {}",
                    config.getString(ClassificationDao.SERVICE_ID));
        }

        final Mode mode = Mode.fromString(config.getString("mode", "view"));
        if (dao != null && mode == Mode.EDIT) {
            add(new CategoryListView("keys"));
            final ClassificationModel model = new ClassificationModel(dao, getModel());
            IDialogFactory dialogFactory = new IDialogFactory() {

                private static final long serialVersionUID = 1L;

                public AbstractDialog<Classification> createDialog() {
                    return createPickerDialog(model, getPreferredLocale());
                }
            };
            add(new DialogLink("edit", new ResourceModel("edit"), dialogFactory, getDialogService())).setEnabled(getTaxonomy() != null);
        } else if (dao != null && mode == Mode.COMPARE && config.containsKey("model.compareTo")) {
            IModel<List<Change<String>>> changesModel = new LoadableDetachableModel<List<Change<String>>>() {
                private static final long serialVersionUID = 1L;

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
            add(new Label("edit",changesModel).setVisible(false));
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

    private Taxonomy getTaxonomy() {
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
}
