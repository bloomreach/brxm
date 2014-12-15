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

import java.util.List;

import javax.swing.tree.TreeNode;

import org.apache.wicket.AttributeModifier;
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
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.plugin.api.TaxonomyHelper;
import org.onehippo.taxonomy.plugin.model.CategoryModel;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.onehippo.taxonomy.plugin.model.TaxonomyModel;
import org.onehippo.taxonomy.plugin.tree.CategoryNode;
import org.onehippo.taxonomy.plugin.tree.TaxonomyTree;
import org.onehippo.taxonomy.plugin.tree.TaxonomyTreeModel;

/**
 * TaxonomyBrowser panel which is rendered in the right panel in the taxonomy picker dialog.
 * @version $Id$
 */
public class TaxonomyBrowser extends Panel {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    TaxonomyModel taxonomyModel;
    WebMarkupContainer container;
    MarkupContainer details;
    private String preferredLocale;

    /**
     * Constructor which organizes the UI components in this panel.
     */
    public TaxonomyBrowser(String id, IModel<Classification> model, final TaxonomyModel taxonomyModel, String preferredLocale) {
        super(id, model);

        this.taxonomyModel = taxonomyModel;

        this.preferredLocale = preferredLocale;

        String treeLocale = getPreferredLocale();
        TaxonomyTree tree = new TaxonomyTree("tree", new TaxonomyTreeModel(taxonomyModel, treeLocale), treeLocale) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode node) {
                if (node instanceof CategoryNode) {
                    setDetails(((CategoryNode) node).getCategory());
                    target.add(container);
                }
                super.onNodeLinkClicked(target, node);
            }

            @Override
            protected void decorateNodeLink(final MarkupContainer nodeLink, final TreeNode node, final int level) {
                super.decorateNodeLink(nodeLink, node, level);
                nodeLink.add(new AttributeModifier("class", "node-link"));
            }
        };
        add(tree);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);

        ListView<String> lv = new ListView<String>("list", getKeys()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item) {
                final String key = item.getModelObject();
                Category category = taxonomyModel.getObject().getCategoryByKey(key);
                IModel<String> labelModel;
                if (category != null) {
                    String name = TaxonomyHelper.getCategoryName(category, getPreferredLocale());
                    while (category.getParent() != null) {
                        category = category.getParent();
                        name = TaxonomyHelper.getCategoryName(category, getPreferredLocale()) + " > " + name;
                    }
                    labelModel = new Model<String>(name);
                } else {
                    labelModel = new ResourceModel("invalid.taxonomy.category");
                }
                item.add(new Label("label", labelModel));

                item.add(new AjaxLink<String>("remove", item.getModel()) {
                    private static final long serialVersionUID = 1L;

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
                        target.add(container);
                    }
                });
            }
        };
        container.add(lv);
        container.add(details = new EmptyDetails("details", "emptyDetails", this));

        final IModel<CanonicalCategory> canonicalNameModel = new LoadableDetachableModel<CanonicalCategory>() {

            @Override
            protected CanonicalCategory load() {
                Classification classification = TaxonomyBrowser.this.getModelObject();
                return new CanonicalCategory(taxonomyModel.getObject(), classification.getCanonical(), getPreferredLocale());
            }
        };
        container.add(new Label("canon", new StringResourceModel("canonical", this, canonicalNameModel)) {

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

    /**
     * Adds labels from the category into the category detail fragment.
     * <p>
     * This method can be overriden if you want to customize the category detail fragment.
     * e.g., adding one or more fields in the category detail fragment.
     * </p>
     */
    protected void addCategoryDetailFields(MarkupContainer detailFragment, Category category) {
        CategoryInfo translation = category.getInfo(getPreferredLocale());

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
     */
    protected String getPreferredLocale() {
        return preferredLocale;
    }

    private void setDetails(Category taxonomyItem) {
        container.addOrReplace(details = newDetails("details", new CategoryModel(taxonomyModel, taxonomyItem.getKey())));
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
        private static final long serialVersionUID = 1L;

        public EmptyDetails(String id, String markupId, MarkupContainer markupProvider) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);
        }

    }

    protected class Details extends Panel {
        private static final long serialVersionUID = 1L;

        public Details(String id, CategoryModel model) {
            super(id);

            setOutputMarkupId(true);

            Category category = model.getObject();
            addCategoryDetailFields(this, category);

            add(new AjaxLink<Category>("add", model) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    String key = getModelObject().getKey();
                    List<String> keys = getKeys();
                    keys.add(key);
                    if (keys.size()==1 && isCanonised()) {
                        setCanonicalKey(key);
                    }
                    target.add(container);
                }

                @Override
                public boolean isVisible() {
                    String key = getModelObject().getKey();
                    return !getKeys().contains(key);
                }

            });

             add(new AjaxLink<Category>("makecanonical", model) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    String key = getModelObject().getKey();
                    setCanonicalKey(key);

                    target.add(container);
        }

                @Override
                public boolean isVisible() {
                    String key = getModelObject().getKey();
                    boolean canonical = getCanonicalKey()!=null && getCanonicalKey().equals(key);
                    boolean selected = getKeys().contains(key);
                    return selected && !canonical && isCanonised();
                }

            });
        }

    }

}
