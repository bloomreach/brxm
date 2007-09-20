package org.hippoecm.frontend.model;

import java.util.Enumeration;

import javax.jcr.Node;
import javax.swing.tree.DefaultTreeModel;

public class JcrTreeModel extends DefaultTreeModel {
    private static final long serialVersionUID = 1L;

    public JcrTreeModel(JcrNodeModel root) {
        super(root);
        // TODO Auto-generated constructor stub
    }
    
    
    public JcrNodeModel findJcrNode(Node node) {
        return findJcrNode((JcrNodeModel) getRoot(), node);
    }
    
    protected JcrNodeModel findJcrNode(JcrNodeModel root, Node node) {
        Enumeration children = root.children();
        while (children.hasMoreElements()) {
            JcrNodeModel child = (JcrNodeModel) children.nextElement();
            Node childNode = child.getNode();
            if (childNode.equals(node)) {
                return child;
            }
            if (!child.isLeaf()) {
                JcrNodeModel temp = findJcrNode(child, node);
                if (temp != null) {
                    return temp;
                }
            }
        }
        return null;
    }


}
