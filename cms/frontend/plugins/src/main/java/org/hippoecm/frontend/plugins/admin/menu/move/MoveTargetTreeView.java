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
package org.hippoecm.frontend.plugins.admin.menu.move;

import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.tree.JcrLazyTreeNode;
import org.hippoecm.frontend.tree.JcrTree;
import org.hippoecm.frontend.tree.LazyTreeModel;
import org.hippoecm.frontend.tree.LazyTreeNode;

public class MoveTargetTreeView extends JcrTree {
    private static final long serialVersionUID = 1L;
    private TreeNode selectedNode;
    private MoveDialog dialog;

    public MoveTargetTreeView(String id, LazyTreeModel treeModel, MoveDialog dialog) {
        super(id, treeModel);
        this.dialog = dialog;
    }

    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
        selectedNode = treeNode;
        JcrLazyTreeNode jcrTreeNode = (JcrLazyTreeNode) treeNode;
        dialog.update(target, jcrTreeNode.getJcrNodeModel());
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }
}
