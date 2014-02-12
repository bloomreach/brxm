/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.addon.workflow.ContextWorkflowManagerPlugin;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.IContextMenuManager;
import org.hippoecm.frontend.model.JcrNodeModel;
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
import org.hippoecm.frontend.plugins.standards.tree.FolderTreeNode;
import org.hippoecm.frontend.plugins.standards.tree.icon.DefaultTreeNodeIconProvider;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;
import org.hippoecm.frontend.plugins.yui.rightclick.RightClickBehavior;
import org.hippoecm.frontend.plugins.yui.scrollbehavior.ScrollBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.util.MaxLengthNodeNameFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreePlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FolderTreePlugin.class);

    protected final CmsJcrTree tree;
    protected JcrTreeModel treeModel;
    protected JcrTreeNode rootNode;
    private JcrNodeModel rootModel;

    private static final String DEFAULT_START_PATH = "/content";

    private WicketTreeHelperBehavior treeHelperBehavior;

    public FolderTreePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        String startingPath = config.getString("path", DEFAULT_START_PATH);
        try {
            Session session = getSession().getJcrSession();
            if (!session.itemExists(startingPath)) {
                log.warn("The configured path '"+startingPath+"' does not exist, using '"+DEFAULT_START_PATH+"' instead.");
                startingPath = DEFAULT_START_PATH;
            }
        } catch (RepositoryException exception) {
            log.warn("The configured path '"+startingPath+"' does not exist, using '"+DEFAULT_START_PATH+"' instead.");
            startingPath = DEFAULT_START_PATH;
        }
        rootModel = new JcrNodeModel(startingPath);

        DocumentListFilter folderTreeConfig = new DocumentListFilter(config);
        final boolean workflowEnabled = getPluginConfig().getAsBoolean("workflow.enabled", true);

        this.rootNode = new FolderTreeNode(rootModel, folderTreeConfig);
        treeModel = new JcrTreeModel(rootNode);
        context.registerService(treeModel, IObserver.class.getName());
        tree = new CmsJcrTree("tree", treeModel, newTreeNodeTranslator(config), newTreeNodeIconProvider()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected MarkupContainer newContextContent(MarkupContainer parent, String id, final TreeNode node) {
                IPluginConfig workflowConfig = config.getPluginConfig("module.workflow");
                if (workflowConfig != null && (node instanceof IJcrTreeNode)) {
                    ContextWorkflowManagerPlugin content = new ContextWorkflowManagerPlugin(context, workflowConfig);
                    content.bind(FolderTreePlugin.this, id);
                    IModel<Node> nodeModel = ((IJcrTreeNode) node).getNodeModel();
                    content.setModel(nodeModel);
                    return content;
                }
                return new EmptyPanel(id);
            }

            @Override
            protected MarkupContainer newContextLink(final MarkupContainer parent, String id, final TreeNode node,
                    final MarkupContainer content) {

                if (getPluginConfig().getBoolean("contextmenu.rightclick.enabled")) {
                    parent.add(new RightClickBehavior(content, parent) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void respond(AjaxRequestTarget target) {
                            updateTree(target);
                            getContextmenu().setVisible(true);
                            target.add(getComponentToUpdate());
                            IContextMenuManager menuManager = findParent(IContextMenuManager.class);
                            if (menuManager != null) {
                                menuManager.showContextMenu(this);
                                final IRequestParameters requestParameters = RequestCycle.get().getRequest().getRequestParameters();
                                StringValue x = requestParameters.getParameterValue(MOUSE_X_PARAM);
                                StringValue y = requestParameters.getParameterValue(MOUSE_Y_PARAM);
                                if (x != null && y != null) {
                                    target.appendJavaScript("Hippo.ContextMenu.renderAtPosition('"
                                            + content.getMarkupId() + "', " + x + ", " + y + ");");
                                } else {
                                    target.appendJavaScript("Hippo.ContextMenu.renderInTree('" + content.getMarkupId()
                                            + "');");
                                }
                            }
                        }
                    });

                }
                MarkupContainer container = super.newContextLink(parent, id, node, content);
                if (!workflowEnabled) {
                    container.setEnabled(false);
                }
                return container;
            }

            @Override
            protected void onContextLinkClicked(MarkupContainer content, AjaxRequestTarget target) {
                target.appendJavaScript("Hippo.ContextMenu.renderInTree('" + content.getMarkupId() + "');");
            }

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                if (clickedNode instanceof IJcrTreeNode) {
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
                }
                updateTree(target);
            }

            @Override
            protected void onJunctionLinkClicked(AjaxRequestTarget target, TreeNode node) {
                updateTree(target);
            }

            @Override
            public void onTargetRespond(final AjaxRequestTarget target) {
                super.onTargetRespond(target);
                target.appendJavaScript(treeHelperBehavior.getRenderString());
                if (workflowEnabled) {
                    target.appendJavaScript(treeHelperBehavior.getUpdateString());
                }
            }
        };
        add(tree);

        tree.add(treeHelperBehavior = new WicketTreeHelperBehavior(new WicketTreeHelperSettings(config)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getWicketId() {
                return tree.getMarkupId();
            }

        });

        tree.setRootLess(config.getBoolean("rootless"));

        onModelChanged();

        add(new ScrollBehavior());
    }

    protected ITreeNodeTranslator newTreeNodeTranslator(IPluginConfig config) {
        return new TreeNodeTranslator();
    }

    protected ITreeNodeIconProvider newTreeNodeIconProvider() {
        IPluginContext context = getPluginContext();
        IPluginConfig config = getPluginConfig();

        final List<ITreeNodeIconProvider> providers = new LinkedList<ITreeNodeIconProvider>();
        providers.add(new DefaultTreeNodeIconProvider());
        providers.addAll(context.getServices(ITreeNodeIconProvider.class.getName(), ITreeNodeIconProvider.class));
        if (config.containsKey("tree.icon.id")) {
            providers.addAll(context.getServices(config.getString("tree.icon.id"), ITreeNodeIconProvider.class));
        }
        Collections.reverse(providers);

        return new ITreeNodeIconProvider() {
            private static final long serialVersionUID = 1L;

            public ResourceReference getNodeIcon(TreeNode treeNode, ITreeState state) {
                for (ITreeNodeIconProvider provider : providers) {
                    ResourceReference icon = provider.getNodeIcon(treeNode, state);
                    if (icon != null) {
                        return icon;
                    }
                }
                throw new RuntimeException("No icon could be found for tree node");
            }

        };
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (target != null && isActive()) {
            tree.updateTree();
        }
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
