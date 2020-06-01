/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.plugins.console.browser.JcrConsoleTree;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

class LookupTargetTreeView extends JcrConsoleTree {

    private static final long serialVersionUID = 1L;

    private IJcrTreeNode selectedNode;
    private LookupDialog dialog;

    LookupTargetTreeView(String id, TreeModel treeModel, LookupDialog dialog) {
        super(id, treeModel);
        this.dialog = dialog;
    }

    @Override
    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
        if (treeNode instanceof IJcrTreeNode) {
            IJcrTreeNode jcrTreeNode = (IJcrTreeNode) treeNode;
            this.selectedNode = jcrTreeNode;
            dialog.setModel(jcrTreeNode.getNodeModel());
        }
    }

    IJcrTreeNode getSelectedNode() {
        return selectedNode;
    }

    void setSelectedNode(JcrNodeModel selectedNode, IJcrTreeModel treeModel) {
        ITreeState treeState = getTreeState();
        TreePath treePath = treeModel.lookup(selectedNode);
        if (treePath != null) {
            for (Object component : treePath.getPath()) {
                treeState.expandNode((TreeNode) component);
            }
    
            TreeNode treeNode = (TreeNode) treePath.getLastPathComponent();
            treeState.selectNode((TreeNode) treePath.getLastPathComponent(), true);
            if (treeNode instanceof IJcrTreeNode) {
                this.selectedNode = (IJcrTreeNode) treeNode;
            }
        }
    }
}
