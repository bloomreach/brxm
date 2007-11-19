package org.hippoecm.frontend.model.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNodeType;

public class JcrTreeNode extends NodeModelWrapper implements TreeNode {
    private static final long serialVersionUID = 1L;

    private JcrTreeModel treeModel;

    private List children = new ArrayList();

    private boolean reload = true;
    private boolean reloadChildCount = true;
    private int childCount = 0;

    // Constructors

    public JcrTreeNode(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public JcrTreeNode(JcrNodeModel nodeModel, JcrTreeModel treeModel) {
        super(nodeModel);       
        setTreeModel(treeModel);
        treeModel.register(this);
    }

    // This invalidades the tree node and all of its children
    // (recursively), causing a reload the next time one of the 
    // TreeNode methods is called.
       
    public void markReload() {
        Iterator it = children.iterator();
        while (it.hasNext()) {
            JcrTreeNode child = (JcrTreeNode) it.next();
            child.markReload();
        }
        this.reload = true;
        this.reloadChildCount = true;
    }

    // The TreeModel that contains this TreeNode

    public void setTreeModel(JcrTreeModel treeModel) {
        this.treeModel = treeModel;
    }

    public JcrTreeModel getTreeModel() {
        return treeModel;
    }

    // Implement TreeNode

    public Enumeration children() {
        ensureChildrenLoaded();
        return Collections.enumeration(children);
    }

    public TreeNode getChildAt(int i) {
        ensureChildrenLoaded();
        return (TreeNode) children.get(i);
    }

    public int getChildCount() {
        childCountLoaded();
        return childCount;
    }

    public int getIndex(TreeNode node) {
        ensureChildrenLoaded();
        return children.indexOf(node);
    }

    public boolean isLeaf() {
        childCountLoaded();
        return childCount == 0;
    }

    public TreeNode getParent() {
        JcrNodeModel parentModel = nodeModel.getParentModel();
        if (parentModel != null) {
            return getTreeModel().lookup(parentModel);
        }
        return null;
    }

    public boolean getAllowsChildren() {
        return true;
    }


    // Override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("nodeModel", nodeModel.toString())
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrTreeNode == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrTreeNode treeNode = (JcrTreeNode) object;
        return new EqualsBuilder().append(nodeModel, treeNode.nodeModel).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(87, 335).append(nodeModel).toHashCode();
    }

    // privates

    private void childCountLoaded() {
        if (nodeModel.getNode() == null) {
            reloadChildCount = false;
            childCount = 0;
        } else if (reloadChildCount) {
            Node node = nodeModel.getNode();
            try {
                /*
                 * When the node is of type NT_FACETRESULT or NT_FACETSEARCH, always show a folder! Therefore,
                 * a place holder childCount = 1 is used. Note that not the real count
                 * getNode().getNodes().getSize() is used, because this might be extremely expensive.
                 */
                if (node.isNodeType(HippoNodeType.NT_FACETRESULT) || node.isNodeType(HippoNodeType.NT_FACETSEARCH)) {
                    childCount = 1;
                } else {
                    childCount = (int) node.getNodes().getSize();
                }

            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            reloadChildCount = false;
        }
    }

    private void ensureChildrenLoaded() {
        if (nodeModel.getNode() == null) {
            reload = false;
            children.clear();
        } else if (reload) {
            Node node = nodeModel.getNode();
            List newChildren = new ArrayList();
            try {
                NodeIterator jcrChildren = node.getNodes();
                while (jcrChildren.hasNext()) {
                    Node jcrChild = jcrChildren.nextNode();
                    if (jcrChild != null) {
                        JcrNodeModel childModel = new JcrNodeModel(nodeModel, jcrChild);
                        JcrTreeNode treeNodeModel = new JcrTreeNode(childModel, treeModel);
                        newChildren.add(treeNodeModel);
                    }
                }
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            reload = false;
            children = newChildren;

            /*
             * When the node is of type NT_FACETRESULT or NT_FACETSEARCH, the child count
             * could be incorrect because it may have been set without retrieving the child nodes.
             */
            childCount = newChildren.size();
            reloadChildCount = false;
        }
    }

}
