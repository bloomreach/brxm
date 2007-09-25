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
import org.hippoecm.frontend.model.JcrNodeModelState;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.tree.JcrLazyTreeModel;
import org.hippoecm.frontend.tree.JcrLazyTreeNode;
import org.hippoecm.frontend.tree.JcrTree;
import org.hippoecm.frontend.tree.LazyTreeModel;
import org.hippoecm.repository.api.HippoNode;

import javax.swing.tree.TreeNode;

public class BrowserPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    JcrLazyTreeModel treeModel;
    JcrTree tree;
    
    public BrowserPlugin(String id, JcrNodeModel model) {
        super(id, model);
        JcrNodeModel modelForTree = new JcrNodeModel(model.getNode());
        JcrLazyTreeNode treeNode = new JcrLazyTreeNode(null, modelForTree);
        treeModel = new JcrLazyTreeModel(treeNode);
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
        JcrNodeModelState nodeState = model.getState();
        JcrLazyTreeNode treeNode = treeModel.getJcrLazyTreeNode(model);
        
        if (nodeState.isChanged()) {
            if (nodeState.isDeleted()) {
                JcrLazyTreeNode parent = (JcrLazyTreeNode) treeNode.getParent();
                if (parent != null) {
                    parent.childRemoved(treeNode.getJcrNodeModel());
                    treeModel.nodeStructureChanged(parent);
                    tree.getTreeState().selectNode(parent, true);
                    
                    // FIXME doing another update is not the best way, any better ideas?
                    Home home = (Home)getWebPage();
                    home.update(target, parent.getJcrNodeModel());
                }
            }
            if (nodeState.isChildAdded()) {
                JcrNodeModel child = nodeState.getRelatedNode();
                if (child != null) {
                    treeNode.childAdded(child);
                    treeModel.nodeStructureChanged(treeNode);
                    tree.getTreeState().expandNode(treeNode);
                }
            }
            if (nodeState.isMoved()) {
                JcrNodeModel newParent = nodeState.getRelatedNode();
                if (newParent == null) {
                    // same parent, node was only renamed
                    treeModel.nodeChanged(treeNode);
                }
                else {
                    // node was moved to different parent
                    JcrLazyTreeNode newParentTreeNode = treeModel.getJcrLazyTreeNode(newParent);
                    if (newParentTreeNode != null) {
                        newParentTreeNode.childAdded(treeNode.getJcrNodeModel());
                        treeModel.nodeStructureChanged(newParentTreeNode);
                    }
                    JcrLazyTreeNode oldParentTreeNode = (JcrLazyTreeNode) treeNode.getParent();
                    if (newParent != null) {
                        oldParentTreeNode.childRemoved(treeNode.getJcrNodeModel());
                        treeModel.nodeStructureChanged(oldParentTreeNode);
                    }
                }
            }
            tree.updateTree(target);
            nodeState.mark(JcrNodeModelState.UNCHANGED);
            nodeState.setRelatedNode(null);
        }
    }

}
