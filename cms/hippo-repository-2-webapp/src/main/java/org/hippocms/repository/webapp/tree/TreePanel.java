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
package org.hippocms.repository.webapp.tree;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class TreePanel extends Panel implements ITreeStateListener {
    private static final long serialVersionUID = 1L;

    public TreePanel(String id, JcrNodeModel model) {
        super(id);
        
        DefaultTreeModel treeModel = new DefaultTreeModel(model);        
        TreeView tree = new TreeView("tree", treeModel);
        tree.getTreeState().addTreeStateListener(this);
        tree.getTreeState().expandNode((TreeNode)model.getRoot());
        add(tree);
    }


    // ITreeStateListener

    public void nodeExpanded(TreeNode treeNodeModel) {
        JcrNodeModel nodeModel = (JcrNodeModel) treeNodeModel;
        Node node = nodeModel.getNode();
        if (node != null) {
            try {
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    Node childNode = iter.nextNode();
                    JcrNodeModel childNodeModel = new JcrNodeModel(childNode);
                    nodeModel.add(childNodeModel);
                }
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void nodeCollapsed(TreeNode treeNodeModel) {
        if (treeNodeModel == null) {
            return;
        }
        JcrNodeModel nodeModel = (JcrNodeModel) treeNodeModel;
        nodeModel.removeAllChildren();
    }

    public void nodeSelected(TreeNode treeNodeModel) {
    }

    public void nodeUnselected(TreeNode treeNodeModel) {
    }

    public void allNodesExpanded() {
    }

    public void allNodesCollapsed() {
    }

}
