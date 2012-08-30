/*
 *  Copyright 2008 Hippo.
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

import javax.jcr.Node;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.tree.DefaultTreeState;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.apache.wicket.model.Model;
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
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.console.NodeModelReference;
import org.hippoecm.frontend.plugins.console.menu.content.ContentExportDialog;
import org.hippoecm.frontend.plugins.console.menu.content.ContentImportDialog;
import org.hippoecm.frontend.plugins.console.menu.copy.CopyDialog;
import org.hippoecm.frontend.plugins.console.menu.delete.DeleteDialog;
import org.hippoecm.frontend.plugins.console.menu.move.MoveDialog;
import org.hippoecm.frontend.plugins.console.menu.node.NodeDialog;
import org.hippoecm.frontend.plugins.console.menu.paths.FixHippoPathsDialog;
import org.hippoecm.frontend.plugins.console.menu.rename.RenameDialog;
import org.hippoecm.frontend.plugins.console.menu.t9ids.T9idsDialog;
import org.hippoecm.frontend.plugins.yui.rightclick.RightClickBehavior;
import org.hippoecm.frontend.plugins.yui.scrollbehavior.ScrollBehavior;
import org.hippoecm.frontend.plugins.yui.widget.tree.TreeWidgetBehavior;
import org.hippoecm.frontend.plugins.yui.widget.tree.TreeWidgetSettings;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.JcrTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowserPlugin.class);

    protected JcrTree tree;
    private TreeWidgetBehavior treeBehavior;

    protected JcrTreeModel treeModel;
    protected IJcrTreeNode rootNode;

    public BrowserPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        this.rootNode = new JcrTreeNode(new JcrNodeModel("/"), null);
        treeModel = new JcrTreeModel(rootNode);
        context.registerService(treeModel, IObserver.class.getName());
        tree = newTree(treeModel);
        add(tree);

        add(new ScrollBehavior());
        onModelChanged();
    }

    protected JcrTree newTree(JcrTreeModel treeModel) {
        JcrTree tree = new BrowserTree(treeModel);

        tree.add(treeBehavior = new TreeWidgetBehavior(new TreeWidgetSettings()));
        return tree;
    }

    protected void onSelect(final IJcrTreeNode treeNodeModel, AjaxRequestTarget target) {
        setDefaultModel(treeNodeModel.getNodeModel());
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        tree.updateTree();
        treeBehavior.render(target);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        JcrNodeModel model = (JcrNodeModel) getDefaultModel();
        TreePath treePath = treeModel.lookup(model);
        ITreeState treeState = tree.getTreeState();
        for (Object node : (Object[]) treePath.getPath()) {
            TreeNode treeNode = (TreeNode) node;
            if (!treeState.isNodeExpanded(treeNode)) {
                treeState.expandNode(treeNode);
            }
        }
        treeState.selectNode((TreeNode) treePath.getLastPathComponent(), true);
    }

    private class BrowserTree extends JcrTree {
        private static final long serialVersionUID = 1L;

        public BrowserTree(final JcrTreeModel treeModel) {
            super("tree", treeModel);
        }

        @Override
        protected ITreeState newTreeState() {
            DefaultTreeState state = new DefaultTreeState();
            JcrTreeModel model = (JcrTreeModel) getModelObject();
            model.setTreeState(state);
            return state;
        }

        @Override
        protected void populateTreeItem(WebMarkupContainer item, int level){
            super.populateTreeItem(item, level);

            Object object = item.getDefaultModelObject();
            if (object instanceof IJcrTreeNode) {
                IJcrTreeNode treeNode = (IJcrTreeNode) object;
                final WebMarkupContainer menu = createContextMenu("contextMenu", (JcrNodeModel) treeNode.getNodeModel());
                item.add(menu);
                item.add(new RightClickBehavior(menu, item) {
                    @Override
                    protected void respond(AjaxRequestTarget target) {
                        getContextmenu().setVisible(true);
                        target.addComponent(getComponentToUpdate());
                        IContextMenuManager menuManager = findParent(IContextMenuManager.class);
                        if (menuManager != null) {
                            menuManager.showContextMenu(this);
                            String x = RequestCycle.get().getRequest().getParameter(MOUSE_X_PARAM);
                            String y = RequestCycle.get().getRequest().getParameter(MOUSE_Y_PARAM);
                            target.appendJavascript(
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
            menuContainer.add(new DialogLink("add-node", new Model<String>("Add node"), dialogFactory, getDialogService()));
            // add node icon
            Image iconAddNode = new Image("icon-add-node") {
                private static final long serialVersionUID = 1L;
                @Override
                protected ResourceReference getImageResourceReference() {
                    return new ResourceReference(BrowserPlugin.class, "add-node.png");
                }
            };
            menuContainer.add(iconAddNode);

            // delete node
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;
                public AbstractDialog<Node> createDialog() {
                    return new DeleteDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(new DialogLink("delete-node", new Model<String>("Delete node"), dialogFactory, getDialogService()));
            // delete node icon
            Image iconDeleteNode = new Image("icon-delete-node") {
                private static final long serialVersionUID = 1L;
                @Override
                protected ResourceReference getImageResourceReference() {
                    return new ResourceReference(BrowserPlugin.class, "delete-node.png");
                }
            };
            menuContainer.add(iconDeleteNode);

            // copy node
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;
                public AbstractDialog<Node> createDialog() {
                    return new CopyDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(new DialogLink("copy-node", new Model<String>("Copy node"), dialogFactory, getDialogService()));
            // copy node icon
            Image iconCopyNode = new Image("icon-copy-node") {
                private static final long serialVersionUID = 1L;
                @Override
                protected ResourceReference getImageResourceReference() {
                    return new ResourceReference(BrowserPlugin.class, "copy-node.png");
                }
            };
            menuContainer.add(iconCopyNode);

            // move node
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;
                public AbstractDialog<Node> createDialog() {
                    return new MoveDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(new DialogLink("move-node", new Model<String>("Move node"), dialogFactory, getDialogService()));
            // copy node icon
            Image iconMoveNode = new Image("icon-move-node") {
                private static final long serialVersionUID = 1L;
                @Override
                protected ResourceReference getImageResourceReference() {
                    return new ResourceReference(BrowserPlugin.class, "move-node.png");
                }
            };
            menuContainer.add(iconMoveNode);

            // rename node
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;
                public AbstractDialog<Node> createDialog() {
                    return new RenameDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(new DialogLink("rename-node", new Model<String>("Rename node"), dialogFactory, getDialogService()));
            // copy node icon
            Image iconRenameNode = new Image("icon-rename-node") {
                private static final long serialVersionUID = 1L;
                @Override
                protected ResourceReference getImageResourceReference() {
                    return new ResourceReference(BrowserPlugin.class, "rename-node.png");
                }
            };
            menuContainer.add(iconRenameNode);

            // xml export
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;
                public IDialogService.Dialog createDialog() {
                    return new ContentExportDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(new DialogLink("xml-export", new Model<String>("XML Export"), dialogFactory, getDialogService()));
            // xml export icon
            Image iconXmlExport = new Image("icon-xml-export") {
                private static final long serialVersionUID = 1L;
                @Override
                protected ResourceReference getImageResourceReference() {
                    return new ResourceReference(BrowserPlugin.class, "xml-export.png");
                }
            };
            menuContainer.add(iconXmlExport);
            // xml import
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;
                public IDialogService.Dialog createDialog() {
                    return new ContentImportDialog(new NodeModelReference(BrowserPlugin.this, model));
                }
            };
            menuContainer.add(new DialogLink("xml-import", new Model<String>("XML Import"), dialogFactory, getDialogService()));
            // xml import icon
            Image iconXmlImport = new Image("icon-xml-import") {
                private static final long serialVersionUID = 1L;
                @Override
                protected ResourceReference getImageResourceReference() {
                    return new ResourceReference(BrowserPlugin.class, "xml-import.png");
                }
            };
            menuContainer.add(iconXmlImport);
            // generate t9ids
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;
                @Override public Dialog createDialog() {
                    return new T9idsDialog(model);
                }
                
            };
            menuContainer.add(new DialogLink("t9ids", new Model<String>("New translation ids"), dialogFactory, getDialogService()));
            // generate t9ids icon
            Image iconT9ids = new Image("icon-t9ids") {
                private static final long serialVersionUID = 1L;
                @Override
                protected ResourceReference getImageResourceReference() {
                    return new ResourceReference(BrowserPlugin.class, "t9ids.png");
                }
            };
            menuContainer.add(iconT9ids);

            dialogFactory = new IDialogFactory() {
                @Override
                public Dialog createDialog() {
                    return new FixHippoPathsDialog(model);
                }
            };
            menuContainer.add(new DialogLink("hippo-paths", new Model<String>("Fix hippo:paths"), dialogFactory, getDialogService()));
            Image iconHippoPaths = new Image("icon-hippo-paths") {
                private static final long serialVersionUID = 1L;
                @Override
                protected ResourceReference getImageResourceReference() {
                    return new ResourceReference(BrowserPlugin.class, "t9ids.png");
                }
            };
            menuContainer.add(iconHippoPaths);

            return menuContainer;
        }

    }

}
