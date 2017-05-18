/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
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
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreePlugin extends RenderPlugin {
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
        boolean canAccessPath = true;
        try {
            final Session session = getSession().getJcrSession();
            if (!session.hasPermission(startingPath, Session.ACTION_READ)) {
                log.warn("User '{} is unauthorized to read at the configured path '{}'", session.getUserID(), startingPath);
                canAccessPath = false;
            } else if (!session.itemExists(startingPath)) {
                log.warn("The configured path '{}' does not exist. Please check the configuration", startingPath);
                canAccessPath =  false;
            }
        } catch (RepositoryException exception) {
            canAccessPath = false;
            log.debug("Path '{}' is invalid", startingPath);
        }

        if (!canAccessPath) {
            tree = null;
            add(new Label("tree", StringUtils.EMPTY));
            return;
        }

        add(tree = initializeTree(context, config, startingPath));
        onModelChanged();
        add(new ScrollBehavior());
    }

    private CmsJcrTree initializeTree(final IPluginContext context, final IPluginConfig config, final String startingPath) {
        rootModel = new JcrNodeModel(startingPath);

        DocumentListFilter folderTreeConfig = new DocumentListFilter(config);
        final boolean workflowEnabled = getPluginConfig().getAsBoolean("workflow.enabled", true);

        this.rootNode = new FolderTreeNode(rootModel, folderTreeConfig, createCustomChildComparator());
        treeModel = new JcrTreeModel(rootNode);
        context.registerService(treeModel, IObserver.class.getName());
        final CmsJcrTree cmsJcrTree = new CmsJcrTree("tree", treeModel, newTreeNodeTranslator(config), newTreeNodeIconProvider(context, config)) {
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
                Task nodeClickedTask = null;
                try {
                    if (HDC.isStarted()) {
                        nodeClickedTask = HDC.getCurrentTask().startSubtask("FolderTreePlugin.onNodeLinkClicked");
                    }
                    if (clickedNode instanceof IJcrTreeNode) {
                        final IJcrTreeNode treeNodeModel = (IJcrTreeNode) clickedNode;
                        // add clicked node path to diagnostics output
                        if (nodeClickedTask != null && clickedNode instanceof FolderTreeNode) {
                            final FolderTreeNode ftn = (FolderTreeNode) clickedNode;
                            final IModel<Node> chainedModel = ftn.getChainedModel();
                            if (chainedModel != null && chainedModel.getObject() != null) {
                                nodeClickedTask.setAttribute("node", JcrUtils.getNodePathQuietly(chainedModel.getObject()));
                            }
                        }
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
                } finally {
                    if (nodeClickedTask != null) {
                        nodeClickedTask.stop();
                    }
                }
            }

            @Override
            protected void onJunctionLinkClicked(AjaxRequestTarget target, TreeNode node) {
                updateTree(target);
            }

            @Override
            public void onTargetRespond(final AjaxRequestTarget target, boolean dirty) {
                if (dirty) {
                    target.appendJavaScript(treeHelperBehavior.getRenderString());
                }
            }

            @Override
            protected void addComponent(final AjaxRequestTarget target, final Component component) {
                if (component.findParent(Page.class) != null) {
                    super.addComponent(target, component);
                }
            }
        };


        cmsJcrTree.add(treeHelperBehavior = new WicketTreeHelperBehavior(new WicketTreeHelperSettings(config)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getWicketId() {
                return tree.getMarkupId();
            }

        });

        cmsJcrTree.setRootLess(config.getBoolean("rootless"));
        return cmsJcrTree;
    }

    protected ITreeNodeTranslator newTreeNodeTranslator(IPluginConfig config) {
        return new TreeNodeTranslator();
    }

    public static ITreeNodeIconProvider newTreeNodeIconProvider(IPluginContext context, IPluginConfig config) {
        final List<ITreeNodeIconProvider> providers = new LinkedList<>();
        providers.add(new DefaultTreeNodeIconProvider());
        providers.addAll(context.getServices(ITreeNodeIconProvider.class.getName(), ITreeNodeIconProvider.class));
        if (config.containsKey("tree.icon.id")) {
            providers.addAll(context.getServices(config.getString("tree.icon.id"), ITreeNodeIconProvider.class));
        }
        Collections.reverse(providers);

        return new ITreeNodeIconProvider() {
            private static final long serialVersionUID = 1L;

            public Component getNodeIcon(final String id, final TreeNode treeNode, final ITreeState state) {
                for (ITreeNodeIconProvider provider : providers) {
                    final Component icon = provider.getNodeIcon(id, treeNode, state);
                    if (icon != null) {
                        return icon;
                    }
                }
                return null;
            }
        };
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (tree != null) {
            if (target != null && isActive() && isVisible()) {
                tree.updateTree();
            }
        }
    }

    @Override
    public void onBeforeRender() {
        if (tree != null) {
            tree.detach();
        }
        super.onBeforeRender();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        if (tree == null) {
            return;
        }
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
            treeState.selectNode(treePath.getLastPathComponent(), true);
        }
    }

    /**
     * Initialize and return a custom comparator that can be used globally in this specific folder tree plugin
     * if set by 'custom.child.comparator' string property in the plugin configuration.
     * @return a custom comparator that can be used globally in this specific folder tree plugin
     */
    private Comparator<IJcrTreeNode> createCustomChildComparator() {
        Comparator<IJcrTreeNode> customComparator = null;

        final String customComparatorName = getPluginConfig().getString("custom.child.comparator", null);

        if (StringUtils.isNotBlank(customComparatorName)) {
            try {
                customComparator = (Comparator<IJcrTreeNode>) Class.forName(customComparatorName).newInstance();
            } catch (Throwable th) {
                log.error("Failed to create custom comparator: '{}'.", customComparatorName, th);
            }
        }

        return customComparator;
    }
}
