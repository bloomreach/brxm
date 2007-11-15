package org.hippoecm.frontend.model;

import java.util.Enumeration;

import javax.jcr.Node;
import javax.swing.tree.TreeNode;

public class SimpleJcrNodeModel extends JcrNodeModel {
    private static final long serialVersionUID = 1L;
    
    public SimpleJcrNodeModel(Node node) {
        super(node);
    }

    public Enumeration children() {
        return null;
    }

    public boolean getAllowsChildren() {
        return false;
    }

    public TreeNode getChildAt(int childIndex) {
        return null;
    }

    public int getChildCount() {
        return 0;
    }

    public int getIndex(TreeNode node) {
        return 0;
    }

    public TreeNode getParent() {
        return null;
    }

    public boolean isLeaf() {
        return false;
    }
    
}
