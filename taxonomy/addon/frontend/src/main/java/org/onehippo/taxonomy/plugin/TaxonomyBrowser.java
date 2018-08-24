/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.tree.TreeNode;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.plugin.api.TaxonomyHelper;
import org.onehippo.taxonomy.plugin.model.CategoryModel;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.onehippo.taxonomy.plugin.model.TaxonomyModel;
import org.onehippo.taxonomy.plugin.tree.CategoryNameComparator;
import org.onehippo.taxonomy.plugin.tree.CategoryNode;
import org.onehippo.taxonomy.plugin.tree.TaxonomyNode;
import org.onehippo.taxonomy.plugin.tree.TaxonomyTree;
import org.onehippo.taxonomy.plugin.tree.TaxonomyTreeModel;
import org.onehippo.taxonomy.util.TaxonomyUtil;

/**
 * TaxonomyBrowser panel which is rendered in the right panel in the taxonomy picker dialog.
 * @version $Id$
 */
public class TaxonomyBrowser extends Panel {

    TaxonomyModel taxonomyModel;
    WebMarkupContainer container;
    private final MarkupContainer emptyDetails;
    private Locale preferredLocale;
    private String currentCategoryKey;
    private boolean taxonomyRootSelected = true;
    private boolean detailsReadOnly;

    /**
     * Constructor which organizes the UI components in this panel.
     * @deprecated use the constructor with the Locale object for the preferredLocale
     */
    @Deprecated
    public TaxonomyBrowser(String id, IModel<Classification> model, final TaxonomyModel taxonomyModel, String preferredLocale) {
        this(id, model, taxonomyModel, TaxonomyUtil.toLocale(preferredLocale), false, null);
    }

    /**
     * Constructor which organizes the UI components in this panel.
     * @deprecated use the constructor with the Locale object for the preferredLocale
     */
    @Deprecated
    public TaxonomyBrowser(String id, IModel<Classification> model, final TaxonomyModel taxonomyModel,
                           String preferredLocale, final boolean detailsReadOnly, final ITreeNodeIconProvider iconProvider) {
        this(id, model, taxonomyModel, TaxonomyUtil.toLocale(preferredLocale), detailsReadOnly, iconProvider);
    }

    /**
     * Constructor which organizes the UI components in this panel.
     */
    public TaxonomyBrowser(String id, IModel<Classification> model, final TaxonomyModel taxonomyModel, final Locale preferredLocale) {
        this(id, model, taxonomyModel, preferredLocale, false, null);
    }

    /**
     * Constructor which organizes the UI components in this panel.
     */
    public TaxonomyBrowser(String id, IModel<Classification> model, final TaxonomyModel taxonomyModel,
                           final Locale preferredLocale, final boolean detailsReadOnly, final ITreeNodeIconProvider iconProvider) {
        super(id, model);
        this.taxonomyModel = taxonomyModel;
        setPreferredLocale(preferredLocale);
        this.detailsReadOnly = detailsReadOnly;

        emptyDetails = new EmptyDetails("details", "emptyDetails", this);

        final Locale treeLocale = getPreferredLocaleObject();
        final Comparator<Category> categoryComparator = getCategoryComparator(taxonomyModel.getPluginConfig(), treeLocale);
        final TaxonomyTreeModel treeModel = new TaxonomyTreeModel(taxonomyModel, treeLocale, categoryComparator);
        final TaxonomyTree tree = new TaxonomyTree("tree", treeModel, treeLocale, iconProvider) {

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode node) {
                if (node instanceof CategoryNode) {
                    final Category category = ((CategoryNode) node).getCategory();
                    taxonomyRootSelected = false;
                    currentCategoryKey = category.getKey();
                    container.addOrReplace(newDetails("details", new CategoryModel(taxonomyModel, currentCategoryKey)));
                } else if (node instanceof TaxonomyNode) {
                    taxonomyRootSelected = true;
                    currentCategoryKey = null;
                    container.addOrReplace(emptyDetails);
                }
                target.add(container);
                super.onNodeLinkClicked(target, node);
            }
        };
        add(tree);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);

        ListView<String> lv = new ListView<String>("list", getKeys()) {

            @Override
            protected void populateItem(ListItem<String> item) {
                final String key = item.getModelObject();
                Category category = taxonomyModel.getObject().getCategoryByKey(key);
                IModel<String> labelModel;
                if (category != null) {
                    String name = TaxonomyHelper.getCategoryName(category, getPreferredLocaleObject());
                    while (category.getParent() != null) {
                        category = category.getParent();
                        name = TaxonomyHelper.getCategoryName(category, getPreferredLocaleObject()) + " > " + name;
                    }
                    labelModel = new Model<>(name);
                } else {
                    labelModel = new ResourceModel("invalid.taxonomy.category");
                }
                item.add(new Label("label", labelModel));

                item.add(new AjaxLink<String>("remove", item.getModel()) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        List<String> keys = getKeys();
                        if (isCanonised()) {
                            // change canonical key if current is the one that is removed
                            if (getModelObject().equals(getCanonicalKey())) {
                                setCanonicalKey(null);
                                keys.remove(getModelObject());
                                if (keys.size() > 0) {
                                    final String siblingKey = keys.get(0);
                                    setCanonicalKey(siblingKey);
                                }
                            } else {
                                keys.remove(getModelObject());
                            }
                        } else {
                            keys.remove(getModelObject());
                        }
                        TaxonomyBrowser.this.modelChanged();
                        target.add(container);
                    }
                });
            }
        };
        container.add(lv);
        container.add(emptyDetails);

        Label selectedCategoriesLabel = new Label("selected-categories-label", new StringResourceModel("taxonomy-selected-header", this));
        selectedCategoriesLabel.setVisible(!detailsReadOnly);
        container.add(selectedCategoriesLabel);

        final IModel<CanonicalCategory> canonicalNameModel = new LoadableDetachableModel<CanonicalCategory>() {

            @Override
            protected CanonicalCategory load() {
                Classification classification = TaxonomyBrowser.this.getModelObject();
                return new CanonicalCategory(taxonomyModel.getObject(), classification.getCanonical(), getPreferredLocaleObject());
            }
        };
        container.add(new Label("canon", new StringResourceModel("canonical", this).setModel(canonicalNameModel)) {

            @Override
            public boolean isVisible() {
                return canonicalNameModel.getObject().getName() != null;
            }

            @Override
            protected void onDetach() {
                canonicalNameModel.detach();
                super.onDetach();
            }
        });
        add(container);
    }

    @Override
    protected void onDetach() {
        taxonomyModel.detach();
        super.onDetach();
    }

    @SuppressWarnings("unchecked")
    public IModel<Classification> getModel() {
        return (IModel<Classification>) getDefaultModel();
    }

    public Classification getModelObject() {
        IModel<Classification> model = getModel();
        if (model != null) {
            return model.getObject();
        }
        return null;
    }

    public String getCurrentCategoryKey() {
        return currentCategoryKey;
    }

    public boolean isTaxonomyRootSelected() {
        return taxonomyRootSelected;
    }

    /**
     * Adds labels from the category into the category detail fragment.
     * <p>
     * This method can be overridden if you want to customize the category detail fragment.
     * e.g., adding one or more fields in the category detail fragment.
     * </p>
     */
    protected void addCategoryDetailFields(MarkupContainer detailFragment, Category category) {
        CategoryInfo translation = category.getInfo(getPreferredLocaleObject());

        if (translation != null) {
            detailFragment.add(new Label("name", translation.getName()));
            detailFragment.add(new MultiLineLabel("description", translation.getDescription()));
            detailFragment.add(new Label("synonyms", arrayToString(translation.getSynonyms())));
        } else {
            detailFragment.add(new Label("name", category.getName()));
            detailFragment.add(new MultiLineLabel("description").setVisible(false));
            detailFragment.add(new Label("synonyms").setVisible(false));
        }
    }

    /**
     * Returns the preferred locale
     * @deprecated use {@link #getPreferredLocaleObject()} to get the language code
     */
    @Deprecated
    protected String getPreferredLocale() {
        return getPreferredLocaleObject().getLanguage();
    }

    protected Locale getPreferredLocaleObject() {
        return preferredLocale;
    }

    /**
     * @param locale if null, the Locale from the Wicket Session is used, as fallback
     */
    private void setPreferredLocale(final Locale locale) {
        if (locale != null) {
            this.preferredLocale = locale;
        } else {
            this.preferredLocale = getSession().getLocale();
        }
    }

    protected Details newDetails(String id, CategoryModel model) {
        return new Details(id, model);
    }

    protected String arrayToString(String[] array) {
        String r = "";
        for (String s : array) {
            if (!r.equals("")) {
                r += ", ";
            }
            r += s;
        }
        return r;
    }

    /**
     * Return <code>Category</code> comparator to be used when sorting sibling category nodes.
     * @deprecated use {@link #getCategoryComparator(IPluginConfig, Locale)} instead
     */
    @Deprecated
    protected Comparator<Category> getCategoryComparator(final IPluginConfig config, final String locale) {
        return getCategoryComparator(config, TaxonomyUtil.toLocale(locale));
    }

    /**
     * Return <code>Category</code> comparator to be used when sorting sibling category nodes.
     */
    protected Comparator<Category> getCategoryComparator(final IPluginConfig config, final Locale locale) {
        Comparator<Category> categoryComparator = null;
        final String sortOptions = config.getString("category.sort.options");

        if (StringUtils.equalsIgnoreCase("name", sortOptions)) {
            categoryComparator = new CategoryNameComparator(locale);
        }

        return categoryComparator;
    }

    private List<String> getKeys() {
        return getModelObject().getKeys();
    }

    private String getCanonicalKey() {
        return getModelObject().getCanonical();
    }

    private void setCanonicalKey(String key) {
        getModelObject().setCanonical(key);
    }

    private boolean isCanonised() {
        return getModelObject().isCanonised();
    }

    class EmptyDetails extends Fragment {
        public EmptyDetails(String id, String markupId, MarkupContainer markupProvider) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);
        }
    }

    protected class Details extends Panel {

        private String categoryKey;

        public Details(String id, CategoryModel model) {
            super(id);

            setOutputMarkupId(true);

            Category category = model.getObject();
            categoryKey = category.getKey();
            addCategoryDetailFields(this, category);

            add(new AjaxLink<Category>("add", model) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    String key = getModelObject().getKey();
                    List<String> keys = getKeys();
                    keys.add(key);
                    if (keys.size()==1 && isCanonised()) {
                        setCanonicalKey(key);
                    }
                    TaxonomyBrowser.this.modelChanged();
                    target.add(container);
                }

                @Override
                public boolean isVisible() {
                    String key = getModelObject().getKey();
                    return !detailsReadOnly && !getKeys().contains(key);
                }
            });

            add(new AjaxLink<Category>("makecanonical", model) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    String key = getModelObject().getKey();
                    setCanonicalKey(key);
                    TaxonomyBrowser.this.modelChanged();
                    target.add(container);
                }

                @Override
                public boolean isVisible() {
                    String key = getModelObject().getKey();
                    boolean canonical = getCanonicalKey()!=null && getCanonicalKey().equals(key);
                    boolean selected = getKeys().contains(key);
                    return !detailsReadOnly && selected && !canonical && isCanonised();
                }
            });
        }

        public String getCategoryKey() {
            return categoryKey;
        }
    }

}
