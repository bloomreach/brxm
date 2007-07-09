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

import java.util.Enumeration;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.extensions.markup.html.tree.DefaultAbstractTree.LinkType;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class TreePanel extends Panel implements ITreeStateListener {
    private static final long serialVersionUID = 1L;

    private TreeView tree;

    public TreePanel(String id, JcrNodeModel model) {
        super(id);

        TreeModel treeModel = new DefaultTreeModel(model);
        expandNode(model);

        tree = new TreeView("tree", treeModel);
        tree.getTreeState().collapseAll();
        tree.addTreeStateListener(this);
        tree.setLinkType(LinkType.AJAX);

        add(tree);
    }

    public TreeView getTree() {
        return tree;
    }
    
    // ITreeStateListener

    public void nodeExpanded(TreeNode node) {
        if (node == null) {
            return;
        }
        Enumeration children = node.children();
        while (children.hasMoreElements()) {
            JcrNodeModel child = (JcrNodeModel) children.nextElement();
            expandNode(child);
        }
    }

    public void nodeCollapsed(TreeNode node) {
    }

    public void nodeSelected(TreeNode node) {
    }

    public void nodeUnselected(TreeNode node) {
    }

    public void allNodesExpanded() {
    }

    public void allNodesCollapsed() {
    }

    // privates

    private void expandNode(JcrNodeModel parentNodeModel) {
        Node parentNode = parentNodeModel.getNode();
        try {
            for (NodeIterator iter = parentNode.getNodes(); iter.hasNext();) {
                Node childNode = iter.nextNode();
                JcrNodeModel childNodeModel = new JcrNodeModel(childNode);
                parentNodeModel.add(childNodeModel);
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
