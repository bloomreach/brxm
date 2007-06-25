package org.hippocms.repository.webapp;

import java.util.Enumeration;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.extensions.markup.html.tree.DefaultAbstractTree.LinkType;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.ITreeStateListener;

public class TreePanel extends Panel implements ITreeStateListener {

    private static final long serialVersionUID = 1L;
    private transient NodeEditor editor;

    public TreePanel(String id, NodeEditor editor, Node rootNode) throws RepositoryException  {

        super(id);
        this.editor = editor;

        // Create the root node and add expand it.
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootNode);
        expandNode(root);

        // Create the treeModel based on the root node
        TreeModel treeModel = new DefaultTreeModel(root);

        // Create the treeComponent based on the treeModel
        Tree tree = new Tree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            protected String renderNode(TreeNode node) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
                Node jcrNode = (Node) treeNode.getUserObject();
                String result;
                try {
                    result = jcrNode.getName();
                } catch (RepositoryException e) {
                    result = e.getMessage();
                }
                return result;
            }
        };

        // Collapse the treeComponent
        tree.getTreeState().collapseAll();

        // Add behaviour to the treeComponent
        tree.getTreeState().addTreeStateListener(this);

        // Disable ajax links for the time being
        tree.setLinkType(LinkType.REGULAR);

        add(tree);
    }

    // Behaviour of the treeComponent

    public void nodeExpanded(TreeNode node) {
        if (node == null) {
            return;
        }
        Enumeration children = node.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            try {
                expandNode(child);
            } catch (RepositoryException e) {
                System.out.println("Exception while expanding node " + e.getMessage());
            }
        }
    }

    public void nodeCollapsed(TreeNode node) {
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
        Enumeration children = node.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            child.removeAllChildren();
        }
    }

    public void nodeSelected(TreeNode node) {
        if (node != null) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
            Node jcrNode = (Node) treeNode.getUserObject();
            editor.renderNode(jcrNode);
        }
    }

    public void nodeUnselected(TreeNode node) {
    }

    public void allNodesExpanded() {
    }

    public void allNodesCollapsed() {
    }

    // privates

    private void expandNode(DefaultMutableTreeNode treeNode) throws RepositoryException {
        Node jcrNode = (Node) treeNode.getUserObject();
        for (NodeIterator iter = jcrNode.getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            if (node.getPath().indexOf("/jcr:system") == -1) {
                treeNode.add(new DefaultMutableTreeNode(node));
            }
        }
    }
    
}
