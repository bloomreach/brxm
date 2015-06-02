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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.addon.workflow.ConfirmDialog;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.plugin.api.EditableCategory;
import org.onehippo.taxonomy.plugin.api.EditableCategoryInfo;
import org.onehippo.taxonomy.plugin.api.TaxonomyException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TaxonomyEditorPlugin used when editing taxonomy documents.
 *
 * @version $Id$
 */
public class TaxonomyEditorPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TaxonomyEditorPlugin.class);

    private static final String MENU_ACTION_STYLE_CLASS = "menu-action";
    private static final String DISABLED_ACTION_STYLE_CLASS = "taxonomy-disabled-action";
    private static final String DISABLED_MENU_ACTION_STYLE_CLASS = MENU_ACTION_STYLE_CLASS + " " + DISABLED_ACTION_STYLE_CLASS;

    private List<LanguageSelection> availableLanguageSelections;
    private LanguageSelection currentLanguageSelection;
    private JcrTaxonomy taxonomy;
    private String key;
    private IModel<String[]> synonymModel;
    private Form<?> container;
    private MarkupContainer holder;
    private MarkupContainer toolbarHolder;
    private TaxonomyTreeModel treeModel;
    private TaxonomyTree tree;
    private final boolean useUrlKeyEncoding;

    private AjaxLink<Void> addCategory;
    private AjaxLink<Void> moveCategory;
    private AjaxLink<Void> removeCategory;
    private AjaxLink<Void> moveupCategory;
    private AjaxLink<Void> movedownCategory;

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
        treeModel = new TaxonomyTreeModel(taxonomyModel, currentLanguageCode, categoryComparator);
        tree = new
                TaxonomyTree("tree", treeModel, currentLanguageCode) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isEnabled() {
                        return editing;
                    }

                    @Override
                    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode node) {
                        if (node instanceof CategoryNode) {
                            final Category category = ((CategoryNode) node).getCategory();
                            key = category.getKey();
                            setMenuActionEnabled(addCategory, true);
                            setMenuActionEnabled(removeCategory, true);
                            setMenuActionEnabled(moveCategory, true);
                            setMenuActionEnabled(moveupCategory,
                                    category instanceof EditableCategory && ((EditableCategory) category).canMoveUp());
                            setMenuActionEnabled(movedownCategory,
                                    category instanceof EditableCategory && ((EditableCategory) category).canMoveDown());
                            redraw();
                        } else if (node instanceof TaxonomyNode) {
                            key = null;
                            setMenuActionEnabled(addCategory, true);
                            setMenuActionEnabled(removeCategory, false);
                            setMenuActionEnabled(moveCategory, false);
                            setMenuActionEnabled(moveupCategory, false);
                            setMenuActionEnabled(movedownCategory, false);
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
        addCategory = new AjaxLink<Void>("add-category") {
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
                        AbstractNode node;
                        if (category != null) {
                            node = new CategoryNode(new CategoryModel(taxonomyModel, key), currentLanguageSelection.getLanguageCode(), categoryComparator);
                        } else {
                            node = new TaxonomyNode(taxonomyModel, currentLanguageSelection.getLanguageCode(), categoryComparator);
                        }
                        try {
                            String newKey = getKey();
                            if (category != null) {
                                category.addCategory(newKey, getName(), currentLanguageSelection.getLanguageCode(), taxonomyModel);
                            } else {
                                taxonomy.addCategory(newKey, getName(), currentLanguageSelection.getLanguageCode());
                            }
                            TreeNode child = new CategoryNode(new CategoryModel(taxonomyModel, newKey), currentLanguageSelection.getLanguageCode(), categoryComparator);
                            tree.getTreeState().selectNode(child, true);
                            key = newKey;
                        } catch (TaxonomyException e) {
                            error(e.getMessage());
                        }
                        tree.expandNode(node);
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
            addCategory.setVisible(false);
        }
        toolbarHolder.add(addCategory);

        moveCategory = new AjaxLink<Void>("move-category") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return editing;
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                final List<String> keys = new ArrayList<String>();
                final Model<String> classificationIdModel = new Model<String>();
                final Classification classification = new Classification(keys, classificationIdModel);
                final IModel<Classification> classificationModel = new Model<Classification>(classification);

                TaxonomyModel taxonomyModel = null;

                try {
                    taxonomyModel = new TaxonomyModel(context, config, null, taxonomy.getName(),
                            new JcrNodeModel(taxonomy.getJcrNode()));
                } catch (RepositoryException e) {
                    log.error("Failed to read taxonomy document variant node model.", e);
                    return;
                }

                dialogService.show(new TaxonomyPickerDialog(context, config, classificationModel,
                        currentLanguageSelection.getLanguageCode(), taxonomyModel, true) {
                    private static final long serialVersionUID = 1L;
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

        };
        moveCategory.add(new Image("move-category-icon", new PackageResourceReference(TaxonomyEditorPlugin.class,
                "res/move-category-16.png")));
        if (!editing) {
            moveCategory.add(new AttributeAppender("class", new Model<String>("disabled"), " "));
            moveCategory.setVisible(false);
        }
        toolbarHolder.add(moveCategory);


        removeCategory = new AjaxLink<Void>("remove-category") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return editing;
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();

                final TaxonomyNode taxonomyRoot = (TaxonomyNode) treeModel.getRoot();
                final CategoryNode categoryNode = taxonomyRoot.findCategoryNodeByKey(key);

                if (!categoryNode.isLeaf()) {
                    dialogService.show(
                            new ConfirmDialog(
                                    new StringResourceModel("cannot-remove-category-title", this, null), 
                                    new StringResourceModel("cannot-remove-nonleaf-category-message", this, null, new NameModel()),
                                    null, null) {
                        private static final long serialVersionUID = 1L;
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
                            private static final long serialVersionUID = 1L;

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
                            private static final long serialVersionUID = 1L;
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

        };
        removeCategory.add(new Image("remove-category-icon", new PackageResourceReference(TaxonomyEditorPlugin.class,
                "res/remove-category-16.png")));
        if (!editing) {
            removeCategory.add(new AttributeAppender("class", new Model<String>("disabled"), " "));
            removeCategory.setVisible(false);
        }
        toolbarHolder.add(removeCategory);

        moveupCategory = new AjaxLink<Void>("moveup-category") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return editing;
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
                            setMenuActionEnabled(moveupCategory,
                                    category instanceof EditableCategory && ((EditableCategory) category).canMoveUp());
                            setMenuActionEnabled(movedownCategory,
                                    category instanceof EditableCategory && ((EditableCategory) category).canMoveDown());
                            redraw();
                        }
                    }
                } catch (TaxonomyException e) {
                    error(e.getMessage());
                }
            }

        };
        moveupCategory.add(new Image("moveup-category-icon", new PackageResourceReference(TaxonomyEditorPlugin.class,
                "res/moveup-category-16.png")));
        if (categoryComparator != null) {
            moveupCategory.setVisible(false);
        }
        toolbarHolder.add(moveupCategory);

        movedownCategory = new AjaxLink<Void>("movedown-category") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return editing;
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
                            setMenuActionEnabled(moveupCategory,
                                    category instanceof EditableCategory && ((EditableCategory) category).canMoveUp());
                            setMenuActionEnabled(movedownCategory,
                                    category instanceof EditableCategory && ((EditableCategory) category).canMoveDown());
                            redraw();
                        }
                    }
                } catch (TaxonomyException e) {
                    error(e.getMessage());
                }
            }

        };
        movedownCategory.add(new Image("movedown-category-icon", new PackageResourceReference(TaxonomyEditorPlugin.class,
                "res/movedown-category-16.png")));
        if (categoryComparator != null) {
            movedownCategory.setVisible(false);
        }
        toolbarHolder.add(movedownCategory);

        // Select the root tree node, and enable or disable toolbar menu actions.
        if (editing) {
            TreeNode rootNode = (TreeNode) tree.getModelObject().getRoot();
            tree.getTreeState().selectNode(rootNode, true);

            setMenuActionEnabled(addCategory, true);
            setMenuActionEnabled(removeCategory, false);
            setMenuActionEnabled(moveCategory, false);
            setMenuActionEnabled(moveupCategory, false);
            setMenuActionEnabled(movedownCategory, false);
        } else {
            setMenuActionEnabled(addCategory, false);
            setMenuActionEnabled(removeCategory, false);
            setMenuActionEnabled(moveCategory, false);
            setMenuActionEnabled(moveupCategory, false);
            setMenuActionEnabled(movedownCategory, false);
        }

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
            target.add(toolbarHolder);
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
     */
    protected Form<?> getContainerForm() {
        return container;
    }

    /**
     * Return <code>Category</code> comparator to be used when sorting sibling category nodes.
     */
    protected Comparator<Category> getCategoryComparator(final IPluginConfig config, final String locale) {
        Comparator<Category> categoryComparator = null;
        final String sortOptions = config.getString("category.sort.options");

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

    protected void setMenuActionEnabled(final AjaxLink<Void> menuAction, boolean enabled) {
        if (enabled) {
            menuAction.add(new AttributeModifier("class", new Model<String>(MENU_ACTION_STYLE_CLASS)));
        } else {
            menuAction.add(new AttributeModifier("class", new Model<String>(DISABLED_MENU_ACTION_STYLE_CLASS)));
        }
    }

    protected Set<String> getClassifiedDocumentHandlesByCategoryKey(final String key, final int maxItems) {
        Set<String> handleNodePaths = new LinkedHashSet<String>();

        try {
            String stmt = "//element(*, hippotaxonomy:classifiable)[@hippotaxonomy:keys = '" + key + "']";
            Query query = getModelObject().getSession().getWorkspace().getQueryManager().createQuery(stmt, Query.XPATH);
            query.setLimit(maxItems);
            QueryResult result = query.execute();
            Node handle;
            Node variant;
            String docPath;
            for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext(); ) {
                variant = nodeIt.nextNode();
                if (variant != null && variant.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    handle = variant.getParent();
                    if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                        docPath = StringUtils.removeStart(handle.getPath(), "/content/documents/");
                        handleNodePaths.add(docPath);
                        if (maxItems > 0 && maxItems <= handleNodePaths.size()) {
                            break;
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to retrieve all the classified documents by key, '{}'.", key);
        }

        return handleNodePaths;
      
    }
}
