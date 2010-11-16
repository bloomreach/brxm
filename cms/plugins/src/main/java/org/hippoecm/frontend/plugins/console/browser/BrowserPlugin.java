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

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.JcrTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowserPlugin.class);

    protected JcrTree tree;
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

    protected JcrTree newTree(TreeModel treeModel) {
        return new JcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                if (clickedNode instanceof IJcrTreeNode) {
                    IJcrTreeNode treeNodeModel = (IJcrTreeNode) clickedNode;
                    BrowserPlugin.this.onSelect(treeNodeModel, target);
                }
            }
        };
    }

    protected void onSelect(final IJcrTreeNode treeNodeModel, AjaxRequestTarget target) {
        setDefaultModel(treeNodeModel.getNodeModel());
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        tree.updateTree();
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

}
