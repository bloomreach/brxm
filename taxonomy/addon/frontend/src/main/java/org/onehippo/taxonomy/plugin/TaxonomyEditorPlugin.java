/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;
import javax.swing.tree.TreeNode;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.plugin.api.EditableCategory;
import org.onehippo.taxonomy.plugin.api.EditableCategoryInfo;
import org.onehippo.taxonomy.plugin.api.TaxonomyException;
import org.onehippo.taxonomy.plugin.model.CategoryModel;
import org.onehippo.taxonomy.plugin.model.JcrTaxonomy;
import org.onehippo.taxonomy.plugin.tree.CategoryNameComparator;
import org.onehippo.taxonomy.plugin.tree.CategoryNode;
import org.onehippo.taxonomy.plugin.tree.TaxonomyNode;
import org.onehippo.taxonomy.plugin.tree.TaxonomyTree;
import org.onehippo.taxonomy.plugin.tree.TaxonomyTreeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TaxonomyEditorPlugin used when editing taxonomy documents.
 *
 * @version $Id$
 */
public class TaxonomyEditorPlugin extends RenderPlugin<Node> {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyEditorPlugin.class);
    private static final long serialVersionUID = 1L;

    private List<LanguageSelection> availableLanguageSelections;
    private LanguageSelection currentLanguageSelection;
    private JcrTaxonomy taxonomy;
    private String key;
    private IModel<String[]> synonymModel;
    private Form<?> container;
    private MarkupContainer holder;
    private MarkupContainer toolbarHolder;
    private TaxonomyTree tree;
    private final boolean useUrlKeyEncoding;

    /**
     * Constructor which adds all the UI components.
     * The UI components include taxonomy tree, toolbar, and detail form container.
     * The detail form container holds all the category detail fields such as name, description and synonyms.
     */
    public TaxonomyEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final boolean editing = "edit".equals(config.getString("mode"));
        useUrlKeyEncoding = config.getAsBoolean("keys.urlencode", false);

        final ITaxonomyService service = getPluginContext()
                .getService(config.getString(ITaxonomyService.SERVICE_ID, ITaxonomyService.DEFAULT_SERVICE_TAXONOMY_ID), ITaxonomyService.class);

        taxonomy = newTaxonomy(getModel(), editing, service);

        availableLanguageSelections = getAvailableLanguageSelections();
        currentLanguageSelection = new LanguageSelection(getLocale(), getLocale());

        synonymModel = new IModel<String[]>() {
            private static final long serialVersionUID = 1L;

            public String[] getObject() {
                EditableCategoryInfo info = taxonomy.getCategoryByKey(key).getInfo(currentLanguageSelection.getLanguageCode());
                return info.getSynonyms();
            }

            public void setObject(String[] object) {
                EditableCategoryInfo info = taxonomy.getCategoryByKey(key).getInfo(currentLanguageSelection.getLanguageCode());
                try {
                    info.setSynonyms(object);
                } catch (TaxonomyException e) {
                    redraw();
                }
            }

            public void detach() {
            }

        };
        final IModel<Taxonomy> taxonomyModel = new Model<Taxonomy>(taxonomy);
        String currentLanguageCode = currentLanguageSelection.getLanguageCode();
        final Comparator<Category> categoryComparator = getCategoryComparator(config, currentLanguageCode);
        tree = new
                TaxonomyTree("tree", new TaxonomyTreeModel(taxonomyModel, currentLanguageCode, categoryComparator), currentLanguageCode) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isEnabled() {
                        return editing;
                    }

                    @Override
                    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode node) {
                        if (node instanceof CategoryNode) {
                            key = ((CategoryNode) node).getCategory().getKey();
                        } else {
                            key = null;
                        }
                        redraw();
                        super.onNodeLinkClicked(target, node);
                    }

                };
        tree.setOutputMarkupId(true);
        add(tree);
        if (editing) {
            TreeNode rootNode = (TreeNode) tree.getModelObject().getRoot();
            tree.getTreeState().selectNode(rootNode, true);
        }

        holder = new WebMarkupContainer("container-holder");
        holder.setOutputMarkupId(true);

        toolbarHolder = new WebMarkupContainer("toolbar-container-holder");
        toolbarHolder.setOutputMarkupId(true);
        AjaxLink<Void> addCategory = new AjaxLink<Void>("add-category") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return editing;
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                dialogService.show(new NewCategoryDialog(taxonomyModel) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean useKeyUrlEncoding() {
                        return useUrlKeyEncoding;
                    }

                    @Override
                    protected StringCodec getNodeNameCodec() {
                        final ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID, ISettingsService.class);
                        final StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
                        final StringCodec stringCodec = stringCodecFactory.getStringCodec("encoding.node");
                        if (stringCodec == null) {
                            // fallback to non-configured
                            return new StringCodecFactory.UriEncoding();
                        }
                        return stringCodec;
                    }

                    @Override
                    protected void onOk() {
                        EditableCategory category = taxonomy.getCategoryByKey(key);
                        TreeNode node;
                        if (category != null) {
                            node = new CategoryNode(new CategoryModel(taxonomyModel, key), currentLanguageSelection.getLanguageCode());
                        } else {
                            node = new TaxonomyNode(taxonomyModel, currentLanguageSelection.getLanguageCode());
                        }
                        try {
                            String newKey = getKey();
                            if (category != null) {
                                category.addCategory(newKey, getName(), currentLanguageSelection.getLanguageCode(), taxonomyModel);
                            } else {
                                taxonomy.addCategory(newKey, getName(), currentLanguageSelection.getLanguageCode());
                            }
                            TreeNode child = new CategoryNode(new CategoryModel(taxonomyModel, newKey), currentLanguageSelection.getLanguageCode());
                            tree.getTreeState().selectNode(child, true);
                            key = newKey;
                        } catch (TaxonomyException e) {
                            error(e.getMessage());
                        }
                        tree.getTreeState().expandNode(node);
                        tree.markNodeChildrenDirty(node);
                        redraw();
                    }
                });
            }

        };
        addCategory.add(new Image("add-category-icon", new PackageResourceReference(TaxonomyEditorPlugin.class,
                "res/new-category-16.png")));
        if (!editing) {
            addCategory.add(new AttributeAppender("class", new Model<String>("disabled"), " "));
        }
        toolbarHolder.add(addCategory);

        container = new Form("container") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return taxonomy.getCategoryByKey(key) != null;
            }
        };

        ChoiceRenderer<LanguageSelection> choiceRenderer = new ChoiceRenderer<LanguageSelection>("displayName", "languageCode");
        DropDownChoice<LanguageSelection> languageSelectionChoice =
                new DropDownChoice<LanguageSelection>("language", new PropertyModel<LanguageSelection>(this, "currentLanguageSelection"), availableLanguageSelections, choiceRenderer);
        languageSelectionChoice.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                redraw();
            }
        });
        languageSelectionChoice.setOutputMarkupId(true);
        languageSelectionChoice.setEnabled(!CollectionUtils.isEmpty(availableLanguageSelections));
        container.add(languageSelectionChoice);
        // show key value key:
        final Label label = new Label("widgetKey", new KeyModel());
        container.add(label);

        if (editing) {
            MarkupContainer name = new Fragment("name", "fragmentname", this);
            FormComponent<String> nameField = new TextField<String>("widget", new NameModel());
            nameField.add(new OnChangeAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    tree.markNodeDirty(getSelectedNode());
                }
            });
            name.add(nameField);
            container.add(name);

            container.add(new TextAreaWidget("description", new DescriptionModel()));
        } else {
            container.add(new Label("name", new NameModel()));
            TextField<String> myKey = new TextField<String>("key");
            myKey.setVisible(false);
            container.add(myKey);
            container.add(new MultiLineLabel("description", new DescriptionModel()));
        }

        container.add(new RefreshingView<String>("view") {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<IModel<String>> getItemModels() {
                return getSynonymList().iterator();
            }

            @Override
            protected void populateItem(final Item<String> item) {
                item.add(new AjaxLink("up") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isEnabled() {
                        return item.getIndex() > 0;
                    }

                    @Override
                    public boolean isVisible() {
                        return editing;
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        String[] synonyms = synonymModel.getObject();
                        int index = item.getIndex();
                        String tmp = synonyms[index];
                        synonyms[index] = synonyms[index - 1];
                        synonyms[index - 1] = tmp;
                        synonymModel.setObject(synonyms);
                        target.add(holder);
                    }
                });
                item.add(new AjaxLink("down") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isEnabled() {
                        String[] synonyms = synonymModel.getObject();
                        return item.getIndex() < synonyms.length - 1;
                    }

                    @Override
                    public boolean isVisible() {
                        return editing;
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        String[] synonyms = synonymModel.getObject();
                        int index = item.getIndex();
                        String tmp = synonyms[index];
                        synonyms[index] = synonyms[index + 1];
                        synonyms[index + 1] = tmp;
                        synonymModel.setObject(synonyms);
                        target.add(holder);
                    }
                });
                item.add(new AjaxLink("remove") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return editing;
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        String[] synonyms = synonymModel.getObject();
                        String[] syns = new String[synonyms.length - 1];
                        System.arraycopy(synonyms, 0, syns, 0, item.getIndex());
                        System.arraycopy(synonyms, item.getIndex() + 1, syns, item.getIndex(), synonyms.length
                                - item.getIndex() - 1);
                        synonymModel.setObject(syns);
                        target.add(holder);
                    }
                });
                if (editing) {
                    TextFieldWidget input = new TextFieldWidget("synonym", item.getModel());
                    FormComponent fc = (FormComponent) input.get("widget");
                    fc.add(StringValidator.minimumLength(1));
                    item.add(input);
                } else {
                    item.add(new Label("synonym", item.getModel()));
                }
            }
        });

        container.add(new AjaxLink("add") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return editing;
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                String[] synonyms = synonymModel.getObject();
                String[] newSyns = new String[synonyms.length + 1];
                System.arraycopy(synonyms, 0, newSyns, 0, synonyms.length);
                newSyns[synonyms.length] = "";
                synonymModel.setObject(newSyns);
                target.add(holder);
            }

        });

        holder.add(container);
        add(toolbarHolder);
        add(holder);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(new CssResourceReference(TaxonomyEditorPlugin.class, "res/style.css")));
    }

    /*
         * Copying from org.apache.commons.lang.LocaleUtils#toLocale(String str)
         * because this utility has been added since commons-lang-2.4, but
         * hippo-cms-engine:jar:2.22.02 is pulling commons-lang-2.1 transitively.
         * So, instead of touching the transitive dependency, copy this utility method
         * as deprecated. We will remove this later as soon as hippo-cms modules upgrade
         * the dependency on commons-lang.
         * @deprecated
         */
    private static Locale toLocale(String str) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len != 2 && len != 5 && len < 7) {
            throw new IllegalArgumentException("Invalid locale format: " + str);
        }
        char ch0 = str.charAt(0);
        char ch1 = str.charAt(1);
        if (ch0 < 'a' || ch0 > 'z' || ch1 < 'a' || ch1 > 'z') {
            throw new IllegalArgumentException("Invalid locale format: " + str);
        }
        if (len == 2) {
            return new Locale(str, "");
        } else {
            if (str.charAt(2) != '_') {
                throw new IllegalArgumentException("Invalid locale format: " + str);
            }
            char ch3 = str.charAt(3);
            if (ch3 == '_') {
                return new Locale(str.substring(0, 2), "", str.substring(4));
            }
            char ch4 = str.charAt(4);
            if (ch3 < 'A' || ch3 > 'Z' || ch4 < 'A' || ch4 > 'Z') {
                throw new IllegalArgumentException("Invalid locale format: " + str);
            }
            if (len == 5) {
                return new Locale(str.substring(0, 2), str.substring(3, 5));
            } else {
                if (str.charAt(5) != '_') {
                    throw new IllegalArgumentException("Invalid locale format: " + str);
                }
                return new Locale(str.substring(0, 2), str.substring(3, 5), str.substring(6));
            }
        }
    }

    /**
     * Factory method for wrapping a JCR node in a JcrTaxonomy object.  Override to customize
     * the taxonomy repository structure.
     */
    protected JcrTaxonomy newTaxonomy(final IModel<Node> model, final boolean editing, final ITaxonomyService service) {
        return new JcrTaxonomy(model, editing, service);
    }

    @Override
    protected void redraw() {
        AjaxRequestTarget target = getRequestCycle().find(AjaxRequestTarget.class);
        if (target != null) {
            target.add(holder);
        } else {
            super.redraw();
        }
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (target != null) {
            tree.updateTree(target);
        }
        super.render(target);
    }

    @Override
    public void onModelChanged() {
        redraw();
    }

    /**
     * Returns the current editable category instance
     * which is being edited.
     *
     * @return
     */
    protected EditableCategory getCategory() {
        return taxonomy.getCategoryByKey(key);
    }

    TreeNode getSelectedNode() {
        Collection<Object> selected = tree.getTreeState().getSelectedNodes();
        if (selected.size() == 0) {
            return null;
        }
        return (TreeNode) selected.iterator().next();
    }

    private List<IModel<String>> getSynonymList() {
        String[] synonyms = synonymModel.getObject();
        List<IModel<String>> list = new ArrayList<IModel<String>>(synonyms.length);
        for (int i = 0; i < synonyms.length; i++) {
            final int j = i;
            list.add(new IModel<String>() {
                private static final long serialVersionUID = 1L;

                public String getObject() {
                    String[] synonyms = synonymModel.getObject();
                    return synonyms[j];
                }

                public void setObject(String object) {
                    String[] synonyms = synonymModel.getObject();
                    synonyms[j] = object;
                    synonymModel.setObject(synonyms);
                }

                public void detach() {
                }

            });
        }
        return list;
    }

    private List<LanguageSelection> getAvailableLanguageSelections() {
        List<LanguageSelection> languageSelections = new ArrayList<LanguageSelection>();

        for (String locale : taxonomy.getLocales()) {
            try {
                Locale localeObj = toLocale(locale);
                languageSelections.add(new LanguageSelection(localeObj, getLocale()));
            } catch (Exception e) {
                log.warn("Invalid locale for the taxonomy: {}", locale);
            }
        }

        if(languageSelections.isEmpty()) {
            LanguageSelection defaultLanguageSelection = new LanguageSelection(getLocale(), getLocale());
            languageSelections.add(defaultLanguageSelection);
        }

        return languageSelections;
    }

    public LanguageSelection getCurrentLanguageSelection() {
        return currentLanguageSelection;
    }

    public void setCurrentLanguageSelection(LanguageSelection currentLanguageSelection) {
        this.currentLanguageSelection = currentLanguageSelection;
    }

    /**
     * Returns the detail form container which holds all the category detail fields such as name, description and synonyms.
     * <p/>
     * If you want to add custom UI components for your custom category fields, you might want to override this plugin
     * and invoke this method in the constructor to add the custom UI components.
     * </P>
     *
     * @return
     */
    protected Form<?> getContainerForm() {
        return container;
    }

    /**
     * Return <code>Category</code> comparator to be used when sorting sibling category nodes.
     * @param config
     * @param locale
     * @return
     */
    protected Comparator<Category> getCategoryComparator(final IPluginConfig config, final String locale) {
        Comparator<Category> categoryComparator = null;

        String sortOptions = "name";

        IPluginConfig params = config.getPluginConfig("cluster.options");
        if (params != null) {
            sortOptions = params.getString("category.sort.options", sortOptions);
        }

        if (StringUtils.equalsIgnoreCase("name", sortOptions)) {
            categoryComparator = new CategoryNameComparator(locale);
        }

        return categoryComparator;
    }

    private final class DescriptionModel implements IModel<String> {
        private static final long serialVersionUID = 1L;

        public String getObject() {
            EditableCategory category = getCategory();
            if (category != null) {
                return category.getInfo(currentLanguageSelection.getLanguageCode()).getDescription();
            }
            return null;
        }

        public void setObject(String object) {
            EditableCategoryInfo info = getCategory().getInfo(currentLanguageSelection.getLanguageCode());
            try {
                info.setDescription(object);
            } catch (TaxonomyException e) {
                error(e.getMessage());
                redraw();
            }
        }

        public void detach() {
        }
    }

    private final class NameModel implements IModel<String> {
        private static final long serialVersionUID = 1L;

        public String getObject() {
            EditableCategory category = getCategory();
            if (category != null) {
                return category.getInfo(currentLanguageSelection.getLanguageCode()).getName();
            }
            return null;
        }

        public void setObject(String object) {
            EditableCategory category = taxonomy.getCategoryByKey(key);
            EditableCategoryInfo info = category.getInfo(currentLanguageSelection.getLanguageCode());
            try {
                info.setName(object);
            } catch (TaxonomyException e) {
                error(e.getMessage());
                redraw();
            }
        }

        public void detach() {
        }
    }

    private final class KeyModel implements IModel<String> {
        private static final long serialVersionUID = 1L;

        public String getObject() {
            return key;
        }

        public void setObject(String object) {
            // do nothing

        }

        public void detach() {
        }
    }

    protected final class LanguageSelection implements Serializable {

        private static final long serialVersionUID = 1L;
        private String languageCode;
        private String displayName;

        /**
         * Constructor
         *
         * @param selectionLocale the locale for the actual language selection item
         * @param uiLocale        the locale by which the language name is determined
         */
        public LanguageSelection(Locale selectionLocale, Locale uiLocale) {
            this(selectionLocale.getLanguage(), selectionLocale.getDisplayLanguage(uiLocale));
        }

        public LanguageSelection(String languageCode, String displayName) {
            this.languageCode = languageCode;
            this.displayName = displayName;
        }

        public String getLanguageCode() {
            return languageCode;
        }

        public void setLanguageCode(String languageCode) {
            this.languageCode = languageCode;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof LanguageSelection)) {
                return false;
            }

            if (languageCode == null && ((LanguageSelection) o).getLanguageCode() == null) {
                return true;
            }

            if (languageCode != null && languageCode.equals(((LanguageSelection) o).getLanguageCode())) {
                return true;
            }

            return false;
        }

        @Override
        public int hashCode() {
            if (languageCode != null) {
                return languageCode.hashCode();
            }

            return super.hashCode();
        }

        @Override
        public String toString() {
            return super.toString() + " [ " + languageCode + ", " + displayName + " }";
        }
    }
}
