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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.tree.ITreeState;

import org.hippoecm.addon.workflow.ContextWorkflowPlugin;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.tree.CachedTreeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.FolderTreeNode;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.JcrTree;

public class FolderTreePlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(FolderTreePlugin.class);

    protected final JcrTree tree;
    protected CachedTreeModel treeModel;
    protected JcrTreeNode rootNode;
    private JcrNodeModel rootModel;

    public FolderTreePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        String startingPath = config.getString("path", "/");
        rootModel = new JcrNodeModel(startingPath);

        ModelReference modelService = new ModelReference(context.getReference(this).getServiceId(), rootModel);
        modelService.init(context);

        DocumentListFilter folderTreeConfig = new DocumentListFilter(config);
        this.rootNode = new FolderTreeNode(rootModel, folderTreeConfig);

        treeModel = new CachedTreeModel(rootNode);
        context.registerService(treeModel, IObserver.class.getName());
        tree = new CmsJcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected MarkupContainer newContextContent(MarkupContainer parent, String id, final TreeNode node) {
                ContextWorkflowPlugin content = new ContextWorkflowPlugin(context, config.getPluginConfig("module.workflow"));
                content.bind(FolderTreePlugin.this, id);
                JcrNodeModel nodeModel = ((IJcrTreeNode) node).getNodeModel();
                content.setModel(nodeModel);
                return content;
                /* FIMXE: the following section would be a better implementation, but plugins
                   loaded this way cannot instantiate plugins themselves.
                MarkupContainer content = (MarkupContainer) FolderTreePlugin.this.newPlugin(id, "module.workflow");
                JcrNodeModel nodeModel = ((IJcrTreeNode) node).getNodeModel();
                content.setModel(nodeModel);
                return content;
                */
            }

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                IJcrTreeNode treeNodeModel = (IJcrTreeNode) clickedNode;
                FolderTreePlugin.this.setModel(treeNodeModel.getNodeModel());
                ITreeState state = getTreeState();
                if (state.isNodeExpanded(clickedNode)) {
                    // super has already switched selection.
                    if (!state.isNodeSelected(clickedNode)) {
                        state.collapseNode(clickedNode);
                    }
                } else {
                    state.expandNode(clickedNode);
                }
                redraw();
            }
        };
        add(tree);

        tree.setRootLess(config.getBoolean("rootless"));

        Component addLink = null;
        if(config.getBoolean("rootless")) {
            addLink = this.newPlugin("id", "module.addfolder");
            if(addLink != null) {
                addLink.setModel(rootModel);
            }
        }
        if(addLink == null) {
            addLink = new Label("id");
            addLink.setVisible(false);
        }
        add(addLink);

        onModelChanged();
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

        JcrNodeModel model = (JcrNodeModel) getModel();

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
            redraw();
        }
    }
}
