/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.model.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTreeNode extends NodeModelWrapper implements IJcrTreeNode {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(JcrTreeNode.class);

    private final static int MAXCOUNT = 2000;

    private List<TreeNode> children;

    private boolean reloadChildren = true;
    private boolean reloadChildCount = true;
    private int childCount = -1;
    private IJcrTreeNode parent;

    public JcrTreeNode(JcrNodeModel nodeModel, IJcrTreeNode parent) {
        super(nodeModel);

        this.parent = parent;
    }

    /**
     * Resolve the child that corresponds to a particular name.
     * @param name
     * @return child node
     * @throws RepositoryException
     */
    public IJcrTreeNode getChild(String name) throws RepositoryException {
        if (getNodeModel().getNode().hasNode(name)) {
            JcrNodeModel childModel = new JcrNodeModel(getNodeModel().getNode().getNode(name));
            return new JcrTreeNode(childModel, this);
        }
        return null;
    }

    // implement TreeNode

    public TreeNode getParent() {
        return parent;
    }

    public Enumeration<TreeNode> children() {
        ensureChildrenLoaded();
        return Collections.enumeration(children);
    }

    public TreeNode getChildAt(int i) {
        if (i == -1) {
            // this is an artifact of the DefaultAbstractTree.isNodeLast(TreeNode)
            // in line: parent.getChildAt(parent.getChildCount() - 1).equals(node)
            // if the parent doesn't have children
            return new LabelTreeNode(this, "empty stub tree node");
        }
        ensureChildrenLoaded();
        if (i >= 0 && i < childCount) {
            return children.get(i);
        }
        log.warn("Invalid index: " + i + " of " + childCount + " children");
        return new LabelTreeNode(this, "invalid tree node");
    }

    public int getChildCount() {
        if (!reloadChildCount && childCount > -1) {
            return childCount;
        }
        ensureChildrenLoaded();
        return childCount;
    }

    public int getIndex(TreeNode node) {
        ensureChildrenLoaded();
        return children.indexOf(node);
    }

    public boolean isLeaf() {
        try {
            if (nodeModel != null) {
                Node node = nodeModel.getNode();
                if (node != null) {
                    return !node.getNodes().hasNext();
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return true;
    }

    public boolean getAllowsChildren() {
        return true;
    }

    // implement IDetachable

    @Override
    public void detach() {
        reloadChildren = true;
        if (children != null) {
            for (TreeNode child : children) {
                if (child instanceof IDetachable) {
                    ((IDetachable) child).detach();
                }
            }
            children = null;
        }
        super.detach();
    }

    protected List<TreeNode> loadChildren() throws RepositoryException {
        Node node = nodeModel.getNode();
        List<TreeNode> newChildren = new ArrayList<TreeNode>();
        NodeIterator jcrChildren = node.getNodes();
        int count = 0;
        while (jcrChildren.hasNext() && count < MAXCOUNT) {
            Node jcrChild = jcrChildren.nextNode();
            if (jcrChild != null) {
                ++count;
                JcrNodeModel childModel = new JcrNodeModel(jcrChild);
                JcrTreeNode treeNodeModel = new JcrTreeNode(childModel, this);
                newChildren.add(treeNodeModel);
            }
        }
        if (jcrChildren.hasNext()) {
            String label = " ... " + (jcrChildren.getSize() - jcrChildren.getPosition()) + " more ...";
            LabelTreeNode treeNodeModel = new LabelTreeNode(this, label);
            newChildren.add(treeNodeModel);
        }
        return newChildren;
    }

    private void ensureChildrenLoaded() {
        if (nodeModel.getNode() == null) {
            reloadChildren = false;
            reloadChildCount = false;
            children = new ArrayList<TreeNode>();
            childCount = 0;
        } else if (children == null || reloadChildren) {
            try {
                children = loadChildren();
                childCount = children.size();
            } catch (RepositoryException e) {
                log.warn("Unable to load children, settting empty list: " + e.getMessage());
                children = new ArrayList<TreeNode>();
                childCount = 0;
            }
            reloadChildren = false;
            reloadChildCount = false;
        }
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
        return nodeModel.equals(treeNode.getNodeModel());
    }

    @Override
    public int hashCode() {
        return nodeModel.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("nodeModel", nodeModel.toString())
                .toString();
    }

}
