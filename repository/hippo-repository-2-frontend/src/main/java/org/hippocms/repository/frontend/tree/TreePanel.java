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
package org.hippocms.repository.frontend.tree;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.frontend.update.IUpdatable;

public class TreePanel extends Panel implements IUpdatable {
    private static final long serialVersionUID = 1L;

    public TreePanel(String id, JcrNodeModel model) {
        super(id);
        DefaultTreeModel treeModel = new DefaultTreeModel(model);
        TreeView tree = new TreeView("tree", treeModel);
        tree.getTreeState().addTreeStateListener(new JcrTreeStateListener());
        tree.getTreeState().expandNode((TreeNode) model.getRoot());
        add(tree);
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        //FIXME
        // TreeNode treeNode = (TreeNode)tree.getTreeState().getSelectedNodes().iterator().next();
        // ....
        // tree.updateTree(target);
    }

}
