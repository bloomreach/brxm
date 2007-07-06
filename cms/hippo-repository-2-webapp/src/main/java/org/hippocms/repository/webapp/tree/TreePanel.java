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

import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.extensions.markup.html.tree.DefaultAbstractTree.LinkType;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class TreePanel extends Panel implements ITreeStateListener {
    private static final long serialVersionUID = 1L;
    
    private Tree tree;

    public TreePanel(String id, String rootPath) throws RepositoryException {

        super(id);

        // Wrap the jcr root node model in a treenode model
        JcrNodeModel root = new JcrNodeModel(rootPath);

        //Expand the root node
        expandNode(root);

        // Create the treeModel based on the root node
        TreeModel treeModel = new DefaultTreeModel(root);

        // Create the treeComponent based on the treeModel
        tree = new TreeView("tree", treeModel);
        
        // Collapse the treeComponent
        tree.getTreeState().collapseAll();

        // Add behaviour to the treeComponent
        addTreeStateListener(this);

        // Disable ajax links for the time being
        tree.setLinkType(LinkType.REGULAR);

        add(tree);
    }
    
    public void addTreeStateListener(ITreeStateListener listener) {
        tree.getTreeState().addTreeStateListener(listener);
    }

    // ITreeStateListener

    public void nodeExpanded(TreeNode node) {
        if (node == null) {
            return;
        }
        Enumeration children = node.children();
        while (children.hasMoreElements()) {
            JcrNodeModel child = (JcrNodeModel) children.nextElement();
            try {
                expandNode(child);
            } catch (RepositoryException e) {
                System.out.println("Exception while expanding node " + e.getMessage());
            }
        }
    }

    public void nodeCollapsed(TreeNode node) {
        Enumeration children = node.children();
        while (children.hasMoreElements()) {
            JcrNodeModel child = (JcrNodeModel) children.nextElement();
            child.removeAllChildren();
        }
    }

    public void nodeSelected(TreeNode node) {
//        if (node != null) {
//            JcrNodeModel treeNode = (JcrNodeModel) node;
//            editor.setNode(treeNode.getNode());
//        }
    }

    public void nodeUnselected(TreeNode node) {
    }

    public void allNodesExpanded() {
    }

    public void allNodesCollapsed() {
    }

    // privates

    private void expandNode(JcrNodeModel treeNode) throws RepositoryException {
        Node jcrNode = treeNode.getNode();
        for (NodeIterator iter = jcrNode.getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            //if (node.getPath().indexOf("/jcr:system") == -1) {
            treeNode.add(new JcrNodeModel(node));
            //}
        }
    }

}
