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
package org.hippoecm.frontend.plugins.cms.browse.tree;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.hippoecm.addon.workflow.ContextWorkflowPlugin;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.IContextMenuManager;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.tree.CmsJcrTree.ITreeNodeTranslator;
import org.hippoecm.frontend.plugins.cms.browse.tree.CmsJcrTree.TreeNodeTranslator;
import org.hippoecm.frontend.plugins.cms.browse.tree.yui.WicketTreeHelperBehavior;
import org.hippoecm.frontend.plugins.cms.browse.tree.yui.WicketTreeHelperSettings;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.FolderTreeNode;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.rightclick.RightClickBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.util.MaxLengthNodeNameFormatter;
import org.hippoecm.frontend.widgets.JcrTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreePlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(FolderTreePlugin.class);

    protected final JcrTree tree;
    protected JcrTreeModel treeModel;
    protected JcrTreeNode rootNode;
    private JcrNodeModel rootModel;

    private WicketTreeHelperBehavior treeHelperBehavior;

    public FolderTreePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        String startingPath = config.getString("path", "/");
        rootModel = new JcrNodeModel(startingPath);

        ModelReference modelService = new ModelReference(context.getReference(this).getServiceId(), rootModel);
        modelService.init(context);

        DocumentListFilter folderTreeConfig = new DocumentListFilter(config);
        this.rootNode = new FolderTreeNode(rootModel, folderTreeConfig);

        treeModel = new JcrTreeModel(rootNode);
        context.registerService(treeModel, IObserver.class.getName());
        tree = new CmsJcrTree("tree", treeModel, newTreeNodeTranslator(config)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected MarkupContainer newContextContent(MarkupContainer parent, String id, final TreeNode node) {
                IPluginConfig workflowConfig = config.getPluginConfig("module.workflow");
                if (workflowConfig != null) {
                    ContextWorkflowPlugin content = new ContextWorkflowPlugin(context, workflowConfig);
                    content.bind(FolderTreePlugin.this, id);
                    JcrNodeModel nodeModel = ((IJcrTreeNode) node).getNodeModel();
                    content.setDefaultModel(nodeModel);
                    return content;
                    /* FIMXE: the following section would be a better implementation, but plugins
                    loaded this way cannot instantiate plugins themselves.
                    MarkupContainer content = (MarkupContainer) FolderTreePlugin.this.newPlugin(id, "module.workflow");
                    JcrNodeModel nodeModel = ((IJcrTreeNode) node).getNodeModel();
                    content.setModel(nodeModel);
                    return content;
                     */
                }
                return new EmptyPanel(id);
            }

            @Override
            protected MarkupContainer newContextLink(final MarkupContainer parent, String id, final TreeNode node,
                    final MarkupContainer content) {
                parent.add(new AbstractBehavior() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void renderHead(IHeaderResponse response) {
                        response.renderOnLoadJavascript(treeHelperBehavior.getRenderString());
                        response.renderOnLoadJavascript(treeHelperBehavior.getUpdateString());
                    }
                });

                if (getPluginConfig().getBoolean("contextmenu.rightclick.enabled")) {
                    parent.add(new RightClickBehavior(content, parent) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void respond(AjaxRequestTarget target) {
                            updateTree(target);
                            getContextmenu().setVisible(true);
                            target.addComponent(getComponentToUpdate());
                            IContextMenuManager menuManager = (IContextMenuManager) findParent(IContextMenuManager.class);
                            if (menuManager != null) {
                                menuManager.showContextMenu(this);
                                String x = RequestCycle.get().getRequest().getParameter(MOUSE_X_PARAM);
                                String y = RequestCycle.get().getRequest().getParameter(MOUSE_Y_PARAM);
                                if (x != null && y != null) {
                                    target.appendJavascript("Hippo.ContextMenu.renderAtPosition('"
                                            + content.getMarkupId() + "', " + x + ", " + y + ");");
                                } else {
                                    target.appendJavascript("Hippo.ContextMenu.renderInTree('" + content.getMarkupId()
                                            + "');");
                                }
                            }
                        }
                    });
                    return null;
                } else {
                    return super.newContextLink(parent, id, node, content);
                }
            };

            @Override
            protected void onContextLinkClicked(MarkupContainer content, AjaxRequestTarget target) {
                target.appendJavascript("Hippo.ContextMenu.renderInTree('" + content.getMarkupId() + "');");
            }

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                IJcrTreeNode treeNodeModel = (IJcrTreeNode) clickedNode;
                FolderTreePlugin.this.setDefaultModel(treeNodeModel.getNodeModel());
                ITreeState state = getTreeState();
                if (state.isNodeExpanded(clickedNode)) {
                    // super has already switched selection.
                    if (!state.isNodeSelected(clickedNode)) {
                        state.collapseNode(clickedNode);
                    }
                } else {
                    state.expandNode(clickedNode);
                }
                updateTree(target);
            }

            @Override
            protected void onJunctionLinkClicked(AjaxRequestTarget target, TreeNode node) {
                updateTree(target);
            }
        };
        add(tree);

        tree.add(treeHelperBehavior = new WicketTreeHelperBehavior(YuiPluginHelper.getManager(context),
                new WicketTreeHelperSettings(config)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getWicketId() {
                return tree.getMarkupId();
            }

        });

        tree.setRootLess(config.getBoolean("rootless"));

        addExtensionPoint("extension.addfolder");

        onModelChanged();

        add(new ScrollBehavior());
    }

    protected ITreeNodeTranslator newTreeNodeTranslator(IPluginConfig config) {
        return new TreeNodeTranslator();
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        tree.updateTree();
    }

    @Override
    public void onBeforeRender() {
        tree.detach();
        super.onBeforeRender();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        JcrNodeModel model = (JcrNodeModel) getDefaultModel();

        ITreeState treeState = tree.getTreeState();
        TreePath treePath = treeModel.lookup(model);
        if (treePath != null) {
            for (Object component : treePath.getPath()) {
                TreeNode treeNode = (TreeNode) component;
                if (!treeState.isNodeExpanded(treeNode)) {
                    treeState.expandNode(treeNode);
                }
            }

            treeState.selectNode((TreeNode) treePath.getLastPathComponent(), true);
        }
    }

    public class FormattedTreeNodeTranslator extends MaxLengthNodeNameFormatter implements ITreeNodeTranslator {
        private static final long serialVersionUID = 1L;

        public FormattedTreeNodeTranslator(IPluginConfig config) {
            super(config.getInt("nodename.max.length", -1), config.getString("nodename.splitter", ".."), config.getInt(
                    "nodename.indent.length", 3));
        }

        public String getTitleName(TreeNode treeNode) {
            return getName(((IJcrTreeNode) treeNode).getNodeModel());
        }

        public String getName(TreeNode treeNode, int indent) {
            return parse(getTitleName(treeNode), indent);
        }

        public boolean hasTitle(TreeNode treeNode, int level) {
            return isTooLong(getTitleName(treeNode), level);
        }
    }

}
