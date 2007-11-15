package org.hippoecm.frontend.plugins.admin.model;

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
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class BrowserModel extends JcrNodeModel {
    private static final long serialVersionUID = 1L;

    protected BrowserModel parent;
    private List children = new ArrayList();
    
    private boolean reload = true;
    private boolean reloadChildCount = true;    
    private int childCount = 0;
    

    // Constructor

    public BrowserModel(BrowserModel parent, Node node) {
        super(node);
        this.parent = parent;
    }
    
    // Convenience method, not part of an api
        
    public void markReload() {
        Iterator it = children.iterator();
        while (it.hasNext()) {
            BrowserModel child = (BrowserModel) it.next();
            child.markReload();
        }
        this.reload = true;
        this.reloadChildCount = true;
    }
    
    // TreeNode implementation
    
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
        return parent;
    }

    public boolean getAllowsChildren() {
        return true;
    }
    
    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("itemModel", itemModel.toString())
            .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof BrowserModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        BrowserModel treeNode = (BrowserModel) object;
        return new EqualsBuilder()
            .append(itemModel, treeNode.itemModel)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(87, 335)
            .append(itemModel)
            .toHashCode();
    }
    
    // privates

    private void childCountLoaded() {
        HippoNode node = (HippoNode) getObject();
        if (node == null) {
            reloadChildCount = false;
            childCount = 0;
        } else if (reloadChildCount) {
            try {
                /*
                 * When the node is of type NT_FACETRESULT or NT_FACETSEARCH, always show a folder! Therefore, 
                 * a place holder childCount = 1 is used. Note that not the real count 
                 * getNode().getNodes().getSize() is used, because this might be extremely expensive.
                 * When opening a folder, the real childCount is set on the parent to ensure a correct drawn tree.
                 * Setting the correct number on the parent is not expensive anymore, because you already opened
                 * the folder. Setting the correct number directly will result in extreme slow results.
                 */
                if(node.isNodeType(HippoNodeType.NT_FACETRESULT) || node.isNodeType(HippoNodeType.NT_FACETSEARCH)){
                    childCount = 1;
                    HippoNode parentNode = (HippoNode) parent.getObject();
                    parent.childCount = (int) parentNode.getNodes().getSize();
                }
                else {
                    childCount = (int) node.getNodes().getSize();
                }
               
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            reloadChildCount = false;
        }
    }
    
    private void ensureChildrenLoaded() {
        HippoNode node = (HippoNode) getObject();
        if (node == null) {
            reload = false;
            children.clear();
        } else if (reload) {
            List newChildren = new ArrayList();
            try {
                NodeIterator jcrChildren = node.getNodes();
                while (jcrChildren.hasNext()) {
                    Node jcrChild = jcrChildren.nextNode();
                    if (jcrChild != null) {
                        JcrNodeModel childModel = new BrowserModel(this, jcrChild);
                        newChildren.add(childModel);
                    }
                }
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            reload = false;
            children = newChildren;
        }
    }


}
