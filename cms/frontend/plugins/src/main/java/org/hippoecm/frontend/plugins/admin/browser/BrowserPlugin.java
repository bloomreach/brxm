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
package org.hippoecm.frontend.plugins.admin.browser;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.tree.JcrLazyTreeNode;
import org.hippoecm.frontend.tree.JcrTree;
import org.hippoecm.frontend.tree.LazyTreeModel;
import org.hippoecm.repository.api.HippoNode;

import javax.swing.tree.TreeNode;

public class BrowserPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    LazyTreeModel treeModel;
    JcrTree tree;
    
    public BrowserPlugin(String id, JcrNodeModel model) {
        super(id, model);
        JcrNodeModel modelForTree = new JcrNodeModel(model.getNode());
        JcrLazyTreeNode treeNode = new JcrLazyTreeNode(null, modelForTree);
        treeModel = new LazyTreeModel(treeNode);
        tree = new JcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
                JcrLazyTreeNode jcrTreeNode = (JcrLazyTreeNode) treeNode;
                Home home = (Home)getWebPage();
                home.update(target, jcrTreeNode.getJcrNodeModel());
            }
        };
        tree.getTreeState().expandNode(treeNode);
        add(tree);
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        System.out.println("BrowserPlugin.update: model: " + model);
        treeModel.nodeChanged((TreeNode)treeModel.getRoot());
        tree.updateTree(target);
    }

}
