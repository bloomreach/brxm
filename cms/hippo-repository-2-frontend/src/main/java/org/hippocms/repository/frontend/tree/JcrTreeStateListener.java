package org.hippocms.repository.frontend.tree;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.wicket.IClusterable;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public class JcrTreeStateListener implements ITreeStateListener, IClusterable {
    private static final long serialVersionUID = 1L;

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

    public void allNodesCollapsed() {
        // TODO Auto-generated method stub

    }

    public void allNodesExpanded() {
        // TODO Auto-generated method stub

    }

    public void nodeSelected(TreeNode node) {
        // TODO Auto-generated method stub

    }

    public void nodeUnselected(TreeNode node) {
        // TODO Auto-generated method stub

    }

}
