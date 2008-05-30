/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.console.browser;

import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.sa.model.IJcrNodeModelListener;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.IJcrService;
import org.hippoecm.frontend.sa.service.render.RenderPlugin;
import org.hippoecm.frontend.tree.JcrTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserPlugin extends RenderPlugin implements IJcrNodeModelListener {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowserPlugin.class);

    protected Tree tree;
    protected AbstractTreeNode rootNode;

    public BrowserPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, IJcrService.class.getName());

        this.rootNode = new JcrTreeNode(new JcrNodeModel("/"));

        JcrTreeModel treeModel = new JcrTreeModel(rootNode);
        tree = newTree(treeModel);
        add(tree);

        onModelChanged();
    }

    protected Tree newTree(JcrTreeModel treeModel) {
        return new JcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                AbstractTreeNode treeNodeModel = (AbstractTreeNode) clickedNode;
                BrowserPlugin.this.onSelect(treeNodeModel, target);
            }
        };
    }

    protected void onSelect(final AbstractTreeNode treeNodeModel, AjaxRequestTarget target) {
        setModel(treeNodeModel.getNodeModel());
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        JcrNodeModel model = (JcrNodeModel) getModel();
        AbstractTreeNode node = null;
        while (model != null) {
            node = rootNode.getTreeModel().lookup(model);

            if (node != null) {
                TreeNode parentNode = (AbstractTreeNode) node.getParent();
                while (parentNode != null && !tree.getTreeState().isNodeExpanded(parentNode)) {
                    tree.getTreeState().expandNode(parentNode);
                    parentNode = parentNode.getParent();
                }
                tree.getTreeState().selectNode(node, true);
                redraw();
                break;
            }
            model = model.getParentModel();
        }
    }

    // implements IJcrNodeModelListener

    public void onFlush(JcrNodeModel nodeModel) {
        AbstractTreeNode node = rootNode.getTreeModel().lookup(nodeModel);
        if (node != null) {
            node.markReload();
            node.getTreeModel().nodeStructureChanged(node);
            redraw();
        }
    }

}
