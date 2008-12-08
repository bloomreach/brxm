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
package org.hippoecm.frontend.dialog.lookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.widgets.JcrTree;

class LookupTargetTreeView extends JcrTree {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private TreeNode selectedNode;
    private LookupDialog dialog;

    LookupTargetTreeView(String id, JcrTreeModel treeModel, LookupDialog dialog) {
        super(id, treeModel);
        this.dialog = dialog;
    }

    @Override
    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
        this.selectedNode = treeNode;
        AbstractTreeNode treeNodeModel = (AbstractTreeNode) treeNode;
        dialog.setModel(treeNodeModel.getNodeModel());
    }

    TreeNode getSelectedNode() {
        return selectedNode;
    }

    void setSelectedNode(JcrNodeModel selectedNode, JcrTreeModel treeModel) {
        List<JcrNodeModel> parents = new ArrayList<JcrNodeModel>();
        JcrNodeModel parent = selectedNode.getParentModel();
        if (parent != null) {
            while (parent != null) {
                parents.add(parent);
                parent = parent.getParentModel();
            }

            Collections.reverse(parents);
            ITreeState treeState = getTreeState();
            for (JcrNodeModel ancestor : parents) {
                treeState.expandNode(treeModel.lookup(ancestor));
            }

            AbstractTreeNode treeNode= treeModel.lookup(selectedNode.getParentModel());
            treeState.selectNode(treeNode, true);
            this.selectedNode = treeNode;
        }
    }
}
