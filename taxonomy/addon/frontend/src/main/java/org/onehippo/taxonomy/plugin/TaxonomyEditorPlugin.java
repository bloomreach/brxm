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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.swing.tree.TreeNode;

import com.google.common.base.Strings;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.validation.validator.StringValidator;

import org.hippoecm.addon.workflow.ConfirmDialog;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.tree.FolderTreePlugin;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;

import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyException;
import org.onehippo.taxonomy.plugin.api.EditableCategory;
import org.onehippo.taxonomy.plugin.api.EditableCategoryInfo;
import org.onehippo.taxonomy.plugin.model.CategoryModel;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.onehippo.taxonomy.plugin.model.JcrTaxonomy;
import org.onehippo.taxonomy.plugin.model.TaxonomyModel;
import org.onehippo.taxonomy.plugin.tree.AbstractNode;
import org.onehippo.taxonomy.plugin.tree.CategoryNameComparator;
import org.onehippo.taxonomy.plugin.tree.CategoryNode;
import org.onehippo.taxonomy.plugin.tree.TaxonomyNode;
import org.onehippo.taxonomy.plugin.tree.TaxonomyTree;
import org.onehippo.taxonomy.plugin.tree.TaxonomyTreeModel;
import org.onehippo.taxonomy.util.TaxonomyUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TaxonomyEditorPlugin used when editing taxonomy documents.
 */
public class TaxonomyEditorPlugin extends RenderPlugin<Node> {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyEditorPlugin.class);

    private static final CssResourceReference CSS = new CssResourceReference(TaxonomyEditorPlugin.class, "style.css");
    private static final String MENU_ACTION_STYLE_CLASS = "menu-action";
    private static final String DISABLED_ACTION_STYLE_CLASS = "taxonomy-disabled-action";
    private static final String DISABLED_MENU_ACTION_STYLE_CLASS = MENU_ACTION_STYLE_CLASS + " " + DISABLED_ACTION_STYLE_CLASS;

    private Locale currentLocaleSelection;
    private JcrTaxonomy taxonomy;
    private String key;
    private IModel<String[]> synonymModel;
    private Form<?> container;
    private MarkupContainer holder;
    private MarkupContainer toolbarHolder;
    private TaxonomyTreeModel treeModel;
    private TaxonomyTree tree;
    private final boolean useUrlKeyEncoding;

    /**
     * Constructor which adds all the UI components. The UI components include taxonomy tree, toolbar, and detail form
     * container. The detail form container holds all the category detail fields such as name, description and
     * synonyms.
     */
    public TaxonomyEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final boolean editing = "edit".equals(config.getString("mode"));
        useUrlKeyEncoding = config.getAsBoolean("keys.urlencode", false);

        final ITaxonomyService service = getPluginContext()
                .getService(config.getString(ITaxonomyService.SERVICE_ID, ITaxonomyService.DEFAULT_SERVICE_TAXONOMY_ID), ITaxonomyService.class);

        taxonomy = newTaxonomy(getModel(), editing, service);

        final List<Locale> availableLocaleSelections = getAvailableLocaleSelections();
        currentLocaleSelection = getLocale();

        synonymModel = new IModel<String[]>() {

            public String[] getObject() {
                EditableCategoryInfo info = taxonomy.getCategoryByKey(key).getInfo(currentLocaleSelection);
                return info.getSynonyms();
            }

            public void setObject(String[] object) {
                EditableCategoryInfo info = taxonomy.getCategoryByKey(key).getInfo(currentLocaleSelection);
                try {
                    info.setSynonyms(object);
                } catch (TaxonomyException e) {
                    redraw();
                }
            }

            public void detach() {
            }

        };

        final IModel<Taxonomy> taxonomyModel = Model.of(taxonomy);
        final Comparator<Category> categoryComparator = getCategoryComparator(config, currentLocaleSelection);
        treeModel = new TaxonomyTreeModel(taxonomyModel, currentLocaleSelection, categoryComparator);
        final ITreeNodeIconProvider treeNodeIconProvider = FolderTreePlugin.newTreeNodeIconProvider(context, config);
        tree = new TaxonomyTree("tree", treeModel, currentLocaleSelection, treeNodeIconProvider) {

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode node) {
                if (node instanceof CategoryNode) {
                    final Category category = ((CategoryNode) node).getCategory();
                    key = category.getKey();
                    if (editing) {
                        updateToolbarForCategory(category);
                    }
                    redraw();
                } else if (node instanceof TaxonomyNode) {
                    key = null;
                    if (editing) {
                        updateToolbarForCategory(null);
                    }
                    redraw();
                } else {
                    log.error("Unexpected tree node: " + node);
                }
                super.onNodeLinkClicked(target, node);
            }
        };
        tree.setOutputMarkupId(true);
        add(tree);

        holder = new WebMarkupContainer("container-holder");
        holder.setOutputMarkupId(true);

        toolbarHolder = new WebMarkupContainer("toolbar-container-holder");
        toolbarHolder.setOutputMarkupId(true);

        if (editing) {
            toolbarHolder.add(new AddButton(taxonomyModel, categoryComparator));
            toolbarHolder.add(new MoveButton(context, config));
            toolbarHolder.add(new RemoveButton());
            toolbarHolder.add(new MoveUpButton(categoryComparator));
            toolbarHolder.add(new MoveDownButton(categoryComparator));

            updateToolbarForCategory(null);
        } else {
            toolbarHolder.setVisible(false);
        }

        container = new Form("container") {

            @Override
            public boolean isVisible() {
                return taxonomy.getCategoryByKey(key) != null;
            }
        };

        ChoiceRenderer<Locale> choiceRenderer = new ChoiceRenderer<>("displayName", "toString");
        DropDownChoice<Locale> languageSelectionChoice =
                new DropDownChoice<>("locales", new PropertyModel<>(this, "currentLocaleSelection"), availableLocaleSelections, choiceRenderer);
        languageSelectionChoice.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                redraw();
            }
        });
        languageSelectionChoice.setOutputMarkupId(true);
        languageSelectionChoice.setEnabled(!CollectionUtils.isEmpty(availableLocaleSelections));
        container.add(languageSelectionChoice);
        // show key value key:
        final Label label = new Label("widgetKey", new KeyModel());
        container.add(label);

        if (editing) {
            MarkupContainer name = new Fragment("name", "fragmentname", this);
            FormComponent<String> nameField = new TextField<>("widget", new NameModel());
            nameField.add(new OnChangeAjaxBehavior() {
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
            TextField<String> myKey = new TextField<>("key");
            myKey.setVisible(false);
            container.add(myKey);
            container.add(new MultiLineLabel("description", new DescriptionModel()));
        }

        container.add(new RefreshingView<String>("view") {
            @Override
            protected Iterator<IModel<String>> getItemModels() {
                return getSynonymList().iterator();
            }

            @Override
            protected void populateItem(final Item<String> item) {
                final WebMarkupContainer controls = new WebMarkupContainer("controls");
                controls.setVisible(editing);
                item.add(controls);

                final AjaxLink upControl = new AjaxLink("up") {
                    @Override
                    public boolean isEnabled() {
                        return item.getIndex() > 0;
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
                };
                upControl.add(HippoIcon.fromSprite("up-icon", Icon.ARROW_UP));
                controls.add(upControl);

                final AjaxLink downControl = new AjaxLink("down") {
                    @Override
                    public boolean isEnabled() {
                        String[] synonyms = synonymModel.getObject();
                        return item.getIndex() < synonyms.length - 1;
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
                };
                downControl.add(HippoIcon.fromSprite("down-icon", Icon.ARROW_DOWN));
                controls.add(downControl);

                final AjaxLink removeControl = new AjaxLink("remove") {
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
                };
                removeControl.add(HippoIcon.fromSprite("remove-icon", Icon.TIMES));
                controls.add(removeControl);

                if (editing) {
                    final TextFieldWidget input = new TextFieldWidget("synonym", item.getModel());
                    input.getFormComponent().add(StringValidator.minimumLength(1));
                    item.add(input);
                } else {
                    item.add(new Label("synonym", item.getModel()));
                }
            }
        });

        final AjaxLink addSynonymLink = new AjaxLink("add") {
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
        };
        addSynonymLink.add(new Label("add-label", getString("add-label")));
        addSynonymLink.add(HippoIcon.fromSprite("add-icon", Icon.PLUS));
        container.add(addSynonymLink);

        holder.add(container);
        add(toolbarHolder);
        add(holder);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(CSS));
    }

    /**
     * Factory method for wrapping a JCR node in a JcrTaxonomy object.  Override to customize the taxonomy repository
     * structure.
     */
    protected JcrTaxonomy newTaxonomy(final IModel<Node> model, final boolean editing, final ITaxonomyService service) {
        return new JcrTaxonomy(model, editing, service);
    }

    @Override
    protected void redraw() {
        AjaxRequestTarget target = getRequestCycle().find(AjaxRequestTarget.class);
        if (target != null) {
            if (toolbarHolder.size() > 0) {
                target.add(toolbarHolder);
            }
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

    /**
     * Returns the current editable category instance which is being edited.
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
        List<IModel<String>> list = new ArrayList<>(synonyms.length);
        for (int i = 0; i < synonyms.length; i++) {
            final int j = i;
            list.add(new IModel<String>() {

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

    private List<Locale> getAvailableLocaleSelections() {
        return taxonomy.getLocaleObjects();
    }

    /**
     * @deprecated use {@link #getCurrentLocaleSelection()} instead.
     */
    @Deprecated
    public LanguageSelection getCurrentLanguageSelection() {
        return new LanguageSelection(currentLocaleSelection, currentLocaleSelection);
    }

    /**
     * @deprecated use {@link #setCurrentLocaleSelection(Locale)} instead.
     */
    @Deprecated
    public void setCurrentLanguageSelection(LanguageSelection currentLanguageSelection) {
        this.currentLocaleSelection = TaxonomyUtil.toLocale(currentLanguageSelection.getLanguageCode());
    }

    public Locale getCurrentLocaleSelection() {
        return currentLocaleSelection;
    }

    public void setCurrentLocaleSelection(final Locale currentLocaleSelection) {
        this.currentLocaleSelection = currentLocaleSelection;
    }

    /**
     * Returns the detail form container which holds all the category detail fields such as name, description and
     * synonyms.
     * <p>
     * If you want to add custom UI components for your custom category fields, you might want to override this plugin
     * and invoke this method in the constructor to add the custom UI components. </p>
     */
    protected Form<?> getContainerForm() {
        return container;
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

    private final class DescriptionModel implements IModel<String> {

        public String getObject() {
            EditableCategory category = getCategory();
            if (category != null) {
                return category.getInfo(currentLocaleSelection).getDescription();
            }
            return null;
        }

        public void setObject(String object) {
            EditableCategoryInfo info = getCategory().getInfo(currentLocaleSelection);
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

        public String getObject() {
            EditableCategory category = getCategory();
            if (category != null) {
                return category.getInfo(currentLocaleSelection).getName();
            }
            return null;
        }

        public void setObject(String object) {
            EditableCategory category = taxonomy.getCategoryByKey(key);
            EditableCategoryInfo info = category.getInfo(currentLocaleSelection);
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

        public String getObject() {
            return key;
        }

        public void setObject(String object) {
            // do nothing

        }

        public void detach() {
        }
    }

    /**
     * @deprecated use {@link Locale} instead.
     */
    @Deprecated
    protected final class LanguageSelection implements Serializable {

        private String languageCode;
        private String displayName;

        /**
         * Constructor
         *
         * @param selectionLocale the locale for the actual language selection item
         * @param uiLocale        the locale by which the language name is determined
         */

        public LanguageSelection(Locale selectionLocale, Locale uiLocale) {
            this(selectionLocale.toString(), getDisplayLanguage(selectionLocale, uiLocale));
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

    private static String getDisplayLanguage(final Locale selectionLocale, final Locale uiLocale) {
        final String displayName = selectionLocale.getDisplayName(uiLocale);
        if (Strings.isNullOrEmpty(displayName)) {
            return selectionLocale.getDisplayLanguage(uiLocale);
        }
        return displayName;
    }

    private void updateToolbarForCategory(final Category category) {
        setMenuActionEnabled(AddButton.ID,      true);
        setMenuActionEnabled(RemoveButton.ID,   category != null);
        setMenuActionEnabled(MoveButton.ID,     category != null);
        setMenuActionEnabled(MoveUpButton.ID,   category instanceof EditableCategory && ((EditableCategory) category).canMoveUp());
        setMenuActionEnabled(MoveDownButton.ID, category instanceof EditableCategory && ((EditableCategory) category).canMoveDown());
    }

    protected void setMenuActionEnabled(final String actionId, boolean enabled) {
        @SuppressWarnings("unchecked")
        final AjaxLink<Void> menuAction = (AjaxLink<Void>) toolbarHolder.get(actionId);

        if (enabled) {
            menuAction.add(new AttributeModifier("class", Model.of(MENU_ACTION_STYLE_CLASS)));
        } else {
            menuAction.add(new AttributeModifier("class", Model.of(DISABLED_MENU_ACTION_STYLE_CLASS)));
        }
    }

    protected Set<String> getClassifiedDocumentHandlesByCategoryKey(final String key, final int maxItems) {
        Set<String> handleNodePaths = new LinkedHashSet<>();

        try {
            String stmt = "//element(*, hippotaxonomy:classifiable)[@hippotaxonomy:keys = '" + key + "']/..";
            @SuppressWarnings("deprecation")
            Query query = getModelObject().getSession().getWorkspace().getQueryManager().createQuery(stmt, Query.XPATH);
            query.setLimit(maxItems);
            QueryResult result = query.execute();
            Node handle;
            String docPath;
            for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext(); ) {
                handle = nodeIt.nextNode();
                if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                    docPath = StringUtils.removeStart(handle.getPath(), "/content/documents/");
                    handleNodePaths.add(docPath);
                    if (maxItems > 0 && maxItems <= handleNodePaths.size()) {
                        break;
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to retrieve all the classified documents by key, '{}'.", key);
        }

        return handleNodePaths;

    }

    private class AddButton extends AjaxLink<Void> {

        public static final String ID = "add-category";
        private final IModel<Taxonomy> taxonomyModel;
        private final Comparator<Category> categoryComparator;

        public AddButton(final IModel<Taxonomy> taxonomyModel, final Comparator<Category> categoryComparator) {
            super(ID);

            this.taxonomyModel = taxonomyModel;
            this.categoryComparator = categoryComparator;

            add(HippoIcon.fromSprite("add-category-icon", Icon.PLUS));
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            IDialogService dialogService = getDialogService();
            dialogService.show(new NewCategoryDialog(taxonomyModel) {

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
                    EditableCategory parentCategory = taxonomy.getCategoryByKey(key);
                    AbstractNode node;
                    if (parentCategory != null) {
                        node = new CategoryNode(new CategoryModel(taxonomyModel, key), currentLocaleSelection, categoryComparator);
                    } else {
                        node = new TaxonomyNode(taxonomyModel, currentLocaleSelection, categoryComparator);
                    }
                    try {
                        String newKey = getKey();
                        Category childCategory = addChildCategory(parentCategory, newKey);
                        TreeNode child = new CategoryNode(new CategoryModel(taxonomyModel, newKey), currentLocaleSelection, categoryComparator);
                        tree.getTreeState().selectNode(child, true);
                        key = newKey;
                        updateToolbarForCategory(childCategory);
                    } catch (TaxonomyException e) {
                        error(e.getMessage());
                    }
                    tree.expandNode(node);
                    tree.markNodeChildrenDirty(node);
                    redraw();
                }

                private Category addChildCategory(final EditableCategory category, final String newKey) throws TaxonomyException {
                    if (category != null) {
                        return category.addCategory(newKey, getName(), currentLocaleSelection, taxonomyModel);
                    } else {
                        return taxonomy.addCategory(newKey, getName(), currentLocaleSelection);
                    }
                }
            });
        }

    }

    private class MoveButton extends AjaxLink<Void> {

        public static final String ID = "move-category";
        private final IPluginContext context;
        private final IPluginConfig config;

        public MoveButton(final IPluginContext context, final IPluginConfig config) {
            super(ID);

            this.context = context;
            this.config = config;

            add(HippoIcon.fromSprite("move-category-icon", Icon.MOVE_INTO));
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            final TaxonomyNode taxonomyRoot = (TaxonomyNode) treeModel.getRoot();
            final CategoryNode categoryNode = taxonomyRoot.findCategoryNodeByKey(key);

            if (categoryNode == null || categoryNode.getCategory() == null) {
                return;
            }

            IDialogService dialogService = getDialogService();
            final List<String> keys = new ArrayList<>();
            final Model<String> classificationIdModel = new Model<>();
            final Classification classification = new Classification(keys, classificationIdModel);
            final IModel<Classification> classificationModel = Model.of(classification);

            TaxonomyModel taxonomyModel;

            try {
                taxonomyModel = new TaxonomyModel(context, config, null, taxonomy.getName(),
                        new JcrNodeModel(taxonomy.getJcrNode()));
            } catch (RepositoryException e) {
                log.error("Failed to read taxonomy document variant node model.", e);
                return;
            }

            dialogService.show(new TaxonomyMoveDialog(context, config, classificationModel, currentLocaleSelection, taxonomyModel) {

                @Override
                protected void onOk() {
                    final String destParentCategoryKey = getCurrentCategoryKey();

                    if (StringUtils.equals(key, destParentCategoryKey)) {
                        error(new StringResourceModel("cannot-move-category-to-itself", TaxonomyEditorPlugin.this, null, new NameModel()).getString());
                    } else if (destParentCategoryKey != null) {
                        try {
                            EditableCategory destParentCategory = taxonomy.getCategoryByKey(destParentCategoryKey);
                            EditableCategory srcCategory = taxonomy.getCategoryByKey(key);
                            TaxonomyNode taxonomyRoot = (TaxonomyNode) treeModel.getRoot();
                            CategoryNode destParentCategoryNode = taxonomyRoot.findCategoryNodeByKey(destParentCategoryKey);
                            CategoryNode srcCategoryNode = taxonomyRoot.findCategoryNodeByKey(key);

                            if (srcCategory != null && srcCategoryNode != null && destParentCategoryNode != null) {
                                srcCategory.move(destParentCategory);
                                destParentCategoryNode.getChildren(true);
                                treeModel.reload(destParentCategoryNode);
                                tree.expandAllToNode(destParentCategoryNode);
                                ((AbstractNode) srcCategoryNode.getParent()).getChildren(true);
                                treeModel.reload(srcCategoryNode.getParent());
                                updateToolbarForCategory(srcCategory);
                                redraw();
                            }
                        } catch (TaxonomyException e) {
                            error(e.getMessage());
                        }
                    } else if (isTaxonomyRootSelected()) {
                        try {
                            EditableCategory srcCategory = taxonomy.getCategoryByKey(key);
                            TaxonomyNode taxonomyRoot = (TaxonomyNode) treeModel.getRoot();
                            CategoryNode srcCategoryNode = taxonomyRoot.findCategoryNodeByKey(key);

                            if (srcCategory != null && srcCategoryNode != null) {
                                srcCategory.move(taxonomy);
                                taxonomyRoot.getChildren(true);
                                treeModel.reload(taxonomyRoot);
                                updateToolbarForCategory(srcCategory);
                                redraw();
                            }
                        } catch (TaxonomyException e) {
                            error(e.getMessage());
                        }
                    }

                    super.onOk();
                }
            });
        }

    }

    private class RemoveButton extends AjaxLink<Void> {

        public static final String ID = "remove-category";

        public RemoveButton() {
            super(ID);
            add(HippoIcon.fromSprite("remove-category-icon", Icon.TIMES));
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            final TaxonomyNode taxonomyRoot = (TaxonomyNode) treeModel.getRoot();
            final CategoryNode categoryNode = taxonomyRoot.findCategoryNodeByKey(key);

            if (categoryNode == null || categoryNode.getCategory() == null) {
                return;
            }

            IDialogService dialogService = getDialogService();

            if (!categoryNode.isLeaf()) {
                dialogService.show(
                        new ConfirmDialog(
                                new StringResourceModel("cannot-remove-category-title", this, null),
                                new StringResourceModel("cannot-remove-nonleaf-category-message", this, null, new NameModel()),
                                null, null) {
                            {
                                setCancelVisible(false);
                            }

                            @Override
                            public void invokeWorkflow() throws Exception {
                            }

                            @Override
                            public IValueMap getProperties() {
                                return DialogConstants.SMALL;
                            }
                        });
            } else {
                final Set<String> referringDocumentHandlePaths = getClassifiedDocumentHandlesByCategoryKey(key, 10);

                if (referringDocumentHandlePaths.isEmpty()) {
                    dialogService.show(
                            new ConfirmDialog(
                                    new StringResourceModel("remove-category-confirm-title", this, null),
                                    new StringResourceModel("remove-category-confirm-message", this, null, new NameModel())) {

                                @Override
                                public void invokeWorkflow() throws Exception {
                                    try {
                                        final EditableCategory category = taxonomy.getCategoryByKey(key);
                                        TaxonomyNode taxonomyRoot = (TaxonomyNode) treeModel.getRoot();
                                        CategoryNode categoryNode = taxonomyRoot.findCategoryNodeByKey(key);

                                        if (category != null && categoryNode != null) {
                                            category.remove();
                                            ((AbstractNode) categoryNode.getParent()).getChildren(true);
                                            treeModel.reload(categoryNode.getParent());
                                            redraw();
                                        }
                                    } catch (TaxonomyException e) {
                                        error(e.getMessage());
                                    }
                                }
                            });
                } else {
                    dialogService.show(
                            new ConfirmDialog(
                                    new StringResourceModel("cannot-remove-category-title", this, null),
                                    new StringResourceModel("cannot-remove-category-message", this, null, new NameModel()),
                                    new Model<>(StringUtils.join(referringDocumentHandlePaths, "\n")),
                                    null) {
                                {
                                    setCancelVisible(false);
                                }

                                @Override
                                public void invokeWorkflow() throws Exception {
                                }

                                @Override
                                public IValueMap getProperties() {
                                    return DialogConstants.LARGE;
                                }
                            });
                }
            }
        }
    }

    private class MoveUpButton extends AjaxLink<Void> {

        public static final String ID = "moveup-category";

        public MoveUpButton(final Comparator<Category> categoryComparator) {
            super(ID);

            add(HippoIcon.fromSprite("moveup-category-icon", Icon.ARROW_UP));

            if (categoryComparator != null) {
                setVisible(false);
            }
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            try {
                EditableCategory category = taxonomy.getCategoryByKey(key);
                TaxonomyNode taxonomyRoot = (TaxonomyNode) treeModel.getRoot();
                CategoryNode categoryNode = taxonomyRoot.findCategoryNodeByKey(key);

                if (category != null && categoryNode != null) {
                    if (category.moveUp()) {
                        ((AbstractNode) categoryNode.getParent()).getChildren(true);
                        treeModel.reload(categoryNode.getParent());
                        updateToolbarForCategory(category);
                        redraw();
                    }
                }
            } catch (TaxonomyException e) {
                error(e.getMessage());
            }
        }
    }

    private class MoveDownButton extends AjaxLink<Void> {

        public static final String ID = "movedown-category";

        public MoveDownButton(final Comparator<Category> categoryComparator) {
            super(ID);

            add(HippoIcon.fromSprite("movedown-category-icon", Icon.ARROW_DOWN));

            if (categoryComparator != null) {
                setVisible(false);
            }
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            try {
                EditableCategory category = taxonomy.getCategoryByKey(key);
                TaxonomyNode taxonomyRoot = (TaxonomyNode) treeModel.getRoot();
                CategoryNode categoryNode = taxonomyRoot.findCategoryNodeByKey(key);

                if (category != null && categoryNode != null) {
                    if (category.moveDown()) {
                        ((AbstractNode) categoryNode.getParent()).getChildren(true);
                        treeModel.reload(categoryNode.getParent());
                        updateToolbarForCategory(category);
                        redraw();
                    }
                }
            } catch (TaxonomyException e) {
                error(e.getMessage());
            }
        }

    }

    private class TaxonomyMoveDialog extends TaxonomyPickerDialog {

        TaxonomyMoveDialog(final IPluginContext context, final IPluginConfig config, final IModel<Classification> classificationModel,
                           final Locale currentLocaleSelection, final TaxonomyModel taxonomyModel) {
            super(context, config, classificationModel, currentLocaleSelection, taxonomyModel, true);

            // the super checks for model changes, but that won't happen for 'select' function
            setOkEnabled(true);
        }
    }
}
