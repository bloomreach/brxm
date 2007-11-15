package org.hippoecm.frontend.model;

import java.util.Enumeration;

import javax.jcr.Node;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class SimpleJcrNodeModel extends JcrNodeModel {
    private static final long serialVersionUID = 1L;
    
    public SimpleJcrNodeModel(Node node) {
        super(node);
    }

    public void markReload() {
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
