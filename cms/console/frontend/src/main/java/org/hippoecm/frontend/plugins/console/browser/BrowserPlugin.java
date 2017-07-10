/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.browser;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.DefaultTreeState;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.IContextMenuManager;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeNodeComparator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.console.NodeModelReference;
import org.hippoecm.frontend.plugins.console.icons.FontAwesomeIcon;
import org.hippoecm.frontend.plugins.console.menu.content.ContentExportDialog;
import org.hippoecm.frontend.plugins.console.menu.content.ContentImportDialog;
import org.hippoecm.frontend.plugins.console.menu.content.YamlExportDialog;
import org.hippoecm.frontend.plugins.console.menu.content.YamlImportDialog;
import org.hippoecm.frontend.plugins.console.menu.copy.CopyDialog;
import org.hippoecm.frontend.plugins.console.menu.delete.DeleteDialog;
import org.hippoecm.frontend.plugins.console.menu.move.MoveDialog;
import org.hippoecm.frontend.plugins.console.menu.node.NodeDialog;
import org.hippoecm.frontend.plugins.console.menu.property.PropertyDialog;
import org.hippoecm.frontend.plugins.console.menu.recompute.RecomputeDialog;
import org.hippoecm.frontend.plugins.console.menu.rename.RenameDialog;
import org.hippoecm.frontend.plugins.console.menu.t9ids.T9idsDialog;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.yui.rightclick.RightClickBehavior;
import org.hippoecm.frontend.plugins.yui.scrollbehavior.ScrollBehavior;
import org.hippoecm.frontend.plugins.yui.widget.tree.TreeWidgetBehavior;
import org.hippoecm.frontend.plugins.yui.widget.tree.TreeWidgetSettings;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.JcrTree;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Collection;

public class BrowserPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(BrowserPlugin.class);

    private static final JavaScriptResourceReference NAVIGATION_JS = new JavaScriptResourceReference(BrowserPlugin.class, "navigation.js");

    protected final JcrTree tree;
    private TreeWidgetBehavior treeBehavior;
    private volatile boolean navigating;

    protected final JcrTreeModel treeModel;
    protected final IJcrTreeNode rootNode;

    public BrowserPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        this.rootNode = new JcrTreeNode(new JcrNodeModel("/"), null, new JcrTreeNodeComparator());

        treeModel = new JcrTreeModel(rootNode);
        context.registerService(treeModel, IObserver.class.getName());
        tree = newTree(treeModel);
        add(tree);

        add(new ScrollBehavior());
        onModelChanged();
    }

    protected JcrTree newTree(final JcrTreeModel treeModel) {
        final JcrTree newTree = new BrowserTree(treeModel);
        newTree.add(treeBehavior = new TreeWidgetBehavior(new TreeWidgetSettings()));

        newTree.add(new AbstractDefaultAjaxBehavior() {

            final TreeNavigator navigator = new TreeNavigator(newTree.getTreeState());

            @Override
            protected void respond(final AjaxRequestTarget target) {
                navigating = true;
                try {
                    RequestCycle rc = RequestCycle.get();
                    String key = rc.getRequest().getQueryParameters().getParameterValue("key").toString();
                    if ("Up".equals(key)) {
                        navigator.up();
                        updateModel(target);
                    } else if ("Down".equals(key)) {
                        navigator.down();
                        updateModel(target);
                    } else if ("Left".equals(key)) {
                        navigator.left();
                    } else if ("Right".equals(key)) {
                        navigator.right();
                    }
                } finally {
                    navigating = false;
                }
            }

            private void updateModel(final AjaxRequestTarget target) {
                final Collection<Object> selectedNodes = newTree.getTreeState().getSelectedNodes();
                if (selectedNodes.size() == 1) {
                    final Object treeNode = selectedNodes.iterator().next();
                    if (treeNode instanceof IJcrTreeNode) {
                        onSelect((IJcrTreeNode) treeNode, target);
                    }
                }
            }

            @Override
            public void renderHead(Component component, final IHeaderResponse response) {
                super.renderHead(component, response);
                response.render(OnDomReadyHeaderItem.forScript("Hippo.Tree.addShortcuts('" + getCallbackUrl() + "');"));
            }
        });
        return newTree;
    }

    protected void onSelect(final IJcrTreeNode treeNodeModel, AjaxRequestTarget target) {
        setDefaultModel(treeNodeModel.getNodeModel());
        IContextMenuManager manager = findParent(IContextMenuManager.class);
        if (manager != null) {
            manager.collapseAllContextMenus();
        }
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (target != null) {
            tree.updateTree();
        }
        treeBehavior.render(target);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        JcrNodeModel model = (JcrNodeModel) getDefaultModel();
        TreePath treePath = treeModel.lookup(model);
        if (treePath != null) {
            ITreeState treeState = tree.getTreeState();
            if (!navigating) {
                for (Object node : treePath.getPath()) {
                    TreeNode treeNode = (TreeNode) node;
                    if (!treeState.isNodeExpanded(treeNode)) {
                        treeState.expandNode(treeNode);
                    }
                }
            }
            treeState.selectNode(treePath.getLastPathComponent(), true);
        }
    }

    private class BrowserTree extends JcrConsoleTree {
        private static final long serialVersionUID = 1L;

        public BrowserTree(final JcrTreeModel treeModel) {
            super("tree", treeModel);
        }

        @Override
        public void renderHead(final IHeaderResponse response) {
            super.renderHead(response);
            response.render(JavaScriptHeaderItem.forReference(NAVIGATION_JS));
        }

        @Override
        public boolean isVirtual(final IJcrTreeNode jcrNode) {
            try {
                HippoNode hippoNode = (HippoNode) jcrNode;
                return hippoNode.isVirtual();
            } catch (RepositoryException e) {
                log.info("Cannot determine whether node '{}' is virtual, assuming it's not",
                        JcrUtils.getNodePathQuietly((Node) jcrNode), e);
                return false;
            }

        }

        @Override
        protected ITreeState newTreeState() {
            DefaultTreeState state = new DefaultTreeState();
            JcrTreeModel model = (JcrTreeModel) getModelObject();
            model.setTreeState(state);
            return state;
        }

        @Override
        protected void populateTreeItem(WebMarkupContainer item, int level) {
            super.populateTreeItem(item, level);

            Object object = item.getDefaultModelObject();
            if (object instanceof IJcrTreeNode) {
                IJcrTreeNode treeNode = (IJcrTreeNode) object;
                final WebMarkupContainer menu = createContextMenu("contextMenu",
                        (JcrNodeModel) treeNode.getNodeModel());
                item.add(menu);
                item.add(new RightClickBehavior(menu, item) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void respond(AjaxRequestTarget target) {
                        getContextmenu().setVisible(true);
                        target.add(getComponentToUpdate());
                        IContextMenuManager menuManager = findParent(IContextMenuManager.class);
                        if (menuManager != null) {
                            menuManager.showContextMenu(this);
                            final IRequestParameters requestParameters = getRequestParameters();
                            StringValue x = requestParameters.getParameterValue(MOUSE_X_PARAM);
                            StringValue y = requestParameters.getParameterValue(MOUSE_Y_PARAM);
                            target.appendJavaScript(
                                    "Hippo.ContextMenu.renderAtPosition('" + menu.getMarkupId() + "', " + x + ", " + y + ");");
                        }
                    }
                });
            } else {
                item.add(new EmptyPanel("contextMenu"));
            }
        }

        @Override
        protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
            if (clickedNode instanceof IJcrTreeNode) {
                IJcrTreeNode treeNodeModel = (IJcrTreeNode) clickedNode;
                BrowserPlugin.this.onSelect(treeNodeModel, target);
            }
        }

        private WebMarkupContainer createContextMenu(String contextMenu, final JcrNodeModel model) {
            WebMarkupContainer menuContainer = new Fragment(contextMenu, "menu", BrowserTree.this);
            menuContainer.setOutputMarkupId(true);
            menuContainer.setVisible(false);

            // add node
            IDialogFactory dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public AbstractDialog<Node> createDialog() {
                    return new NodeDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(
                    new DialogLink("add-node", new Model<>(getString("add.node")), dialogFactory, getDialogService()));
            // add node icon
            Label iconAddNode = new Label("icon-add-node", StringUtils.EMPTY);
            iconAddNode.add(CssClass.append(FontAwesomeIcon.PLUS.cssClass()));
            iconAddNode.add(CssClass.append("add-icon"));
            menuContainer.add(iconAddNode);

            // delete node
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public AbstractDialog<Node> createDialog() {
                    return new DeleteDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(
                    new DialogLink("delete-node", new Model<>(getString("delete.node")), dialogFactory, getDialogService()));
            // delete node icon
            Label iconDeleteNode = new Label("icon-delete-node", StringUtils.EMPTY);
            iconDeleteNode.add(CssClass.append(FontAwesomeIcon.TIMES.cssClass()));
            iconDeleteNode.add(CssClass.append("delete-icon"));
            menuContainer.add(iconDeleteNode);

            // add property
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public AbstractDialog<Node> createDialog() {
                    return new PropertyDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(
                    new DialogLink("add-property", new Model<>(getString("add.property")), dialogFactory, getDialogService()));
            // add property icon
            Label addProperty = new Label("icon-add-property", StringUtils.EMPTY);
            addProperty.add(CssClass.append(FontAwesomeIcon.PLUS.cssClass()));
            addProperty.add(CssClass.append("add-property-icon"));
            menuContainer.add(addProperty);

            // copy node
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public AbstractDialog<Node> createDialog() {
                    return new CopyDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(
                    new DialogLink("copy-node", new Model<>(getString("copy.node")), dialogFactory, getDialogService()));
            // copy node icon
            Label iconCopyNode = new Label("icon-copy-node", StringUtils.EMPTY);
            iconCopyNode.add(CssClass.append(FontAwesomeIcon.FILES_O.cssClass()));
            iconCopyNode.add(CssClass.append("copy-icon"));
            menuContainer.add(iconCopyNode);

            // move node
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public AbstractDialog<Node> createDialog() {
                    return new MoveDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(
                    new DialogLink("move-node", new Model<>(getString("move.node")), dialogFactory, getDialogService()));
            // move node icon
            Label iconMoveNode = new Label("icon-move-node", StringUtils.EMPTY);
            iconMoveNode.add(CssClass.append(FontAwesomeIcon.BARS.cssClass()));
            iconMoveNode.add(CssClass.append("move-icon"));
            menuContainer.add(iconMoveNode);

            // rename node
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public AbstractDialog<Node> createDialog() {
                    return new RenameDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(
                    new DialogLink("rename-node", new Model<>(getString("rename.node")), dialogFactory, getDialogService()));
            // rename node icon
            Label iconRenameNode = new Label("icon-rename-node", StringUtils.EMPTY);
            iconRenameNode.add(CssClass.append(FontAwesomeIcon.PENCIL_SQUARE_O.cssClass()));
            iconRenameNode.add(CssClass.append("rename-icon"));
            menuContainer.add(iconRenameNode);

            // xml export
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public IDialogService.Dialog createDialog() {
                    return new ContentExportDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(
                    new DialogLink("xml-export", new Model<>(getString("xml.export")), dialogFactory, getDialogService()));
            // xml export icon
            Label iconXmlExport = new Label("icon-xml-export", StringUtils.EMPTY);
            iconXmlExport.add(CssClass.append(new Model<>(FontAwesomeIcon.DOWNLOAD.cssClass())));
            iconXmlExport.add(CssClass.append(new Model<>("xml-export-icon")));
            menuContainer.add(iconXmlExport);
            // xml import
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public IDialogService.Dialog createDialog() {
                    return new ContentImportDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(
                    new DialogLink("xml-import", new Model<>(getString("xml.import")), dialogFactory, getDialogService()));
            // xml import icon
            Label iconXmlImport = new Label("icon-xml-import", StringUtils.EMPTY);
            iconXmlImport.add(CssClass.append(FontAwesomeIcon.UPLOAD.cssClass()));
            iconXmlImport.add(CssClass.append("xml-import-icon"));
            menuContainer.add(iconXmlImport);

//
            // yaml export
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public IDialogService.Dialog createDialog() {
                    return new YamlExportDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(
                    new DialogLink("yaml-export", new Model<>(getString("yaml.export")), dialogFactory, getDialogService()));
            // yaml export icon
            Label iconYamlExport = new Label("icon-yaml-export", StringUtils.EMPTY);
            iconYamlExport.add(CssClass.append(new Model<>(FontAwesomeIcon.DOWNLOAD.cssClass())));
            iconYamlExport.add(CssClass.append(new Model<>("xml-export-icon")));
            menuContainer.add(iconYamlExport);

            // yaml import
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public IDialogService.Dialog createDialog() {
                    return new YamlImportDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(
                    new DialogLink("yaml-import", new Model<>(getString("yaml.import")), dialogFactory, getDialogService()));
            // yaml import icon
            Label iconYamlImport = new Label("icon-yaml-import", StringUtils.EMPTY);
            iconYamlImport.add(CssClass.append(FontAwesomeIcon.UPLOAD.cssClass()));
            iconYamlImport.add(CssClass.append("xml-import-icon"));
            menuContainer.add(iconYamlImport);


            // generate t9ids
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                @Override
                public Dialog createDialog() {
                    return new T9idsDialog(model);
                }

            };
            menuContainer.add(new DialogLink("t9ids", new Model<>(getString("new.translation.ids")), dialogFactory,
                    getDialogService()));
            // generate t9ids icon
            Label iconT9ids = new Label("icon-t9ids", StringUtils.EMPTY);
            iconT9ids.add(CssClass.append(FontAwesomeIcon.FLAG_O.cssClass()));
            iconT9ids.add(CssClass.append("t9ids-icon"));
            menuContainer.add(iconT9ids);

            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                @Override
                public Dialog createDialog() {
                    return new RecomputeDialog(model);
                }
            };
            menuContainer.add(new DialogLink("recompute", new Model<>(getString("recompute.derived")), dialogFactory,
                    getDialogService()));
            Label iconHippoPaths = new Label("icon-recompute", StringUtils.EMPTY);
            iconHippoPaths.add(CssClass.append(FontAwesomeIcon.CALCULATOR.cssClass()));
            iconHippoPaths.add(CssClass.append("recompute-icon"));
            menuContainer.add(iconHippoPaths);

            return menuContainer;
        }

    }

    private IRequestParameters getRequestParameters() {
        return RequestCycle.get().getRequest().getRequestParameters();
    }

}
