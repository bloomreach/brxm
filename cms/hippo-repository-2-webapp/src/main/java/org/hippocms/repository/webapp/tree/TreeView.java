package org.hippocms.repository.webapp.tree;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class TreeView extends Tree {
    private static final long serialVersionUID = 1L;

    public TreeView(String id, TreeModel model) {
        super(id, model);
    }
    
    protected String renderNode(TreeNode node) {
        JcrNodeModel treeNode = (JcrNodeModel) node;
        Node jcrNode = treeNode.getNode();
        String result = "null";
        if (jcrNode != null) {
            try {
                result = jcrNode.getName();
            } catch (RepositoryException e) {
                result = e.getMessage();
            }
        }
        return result;
    }

}
