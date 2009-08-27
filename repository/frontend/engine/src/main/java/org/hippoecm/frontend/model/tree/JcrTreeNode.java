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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
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
    private boolean reloadChildcount = true;
    private int childcount = 0;
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
        ensureChildrenLoaded();
        if (i >= 0 && i < children.size()) {
            return children.get(i);
        }
        log.error("Invalid index: " + i + " of " + children.size() + " children [node "
                + nodeModel.getItemModel().getPath() + "]");
        return new LabelTreeNode(this, "invalid tree node");
    }

    public int getChildCount() {
        ensureChildcountLoaded();
        return childcount;
    }

    public int getIndex(TreeNode node) {
        ensureChildrenLoaded();
        return children.indexOf(node);
    }

    public boolean isLeaf() {
        if (!reloadChildcount) {
            return childcount == 0;
        }
        try {
            if (nodeModel != null && nodeModel.getNode() != null) {
                return !nodeModel.getNode().getNodes().hasNext();
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
        reloadChildcount = true;
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

    protected int loadChildcount() throws RepositoryException {
        int result;
        Node node = nodeModel.getNode();
        if (node.isNodeType(HippoNodeType.NT_FACETRESULT) || node.isNodeType(HippoNodeType.NT_FACETSEARCH)
                || ((node instanceof HippoNode) && ((HippoNode) node).getCanonicalNode() == null)) {
            result = 1;
        } else {
            result = (int) node.getNodes().getSize();
        }
        return result;
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

    private void ensureChildcountLoaded() {
        if (nodeModel == null || nodeModel.getNode() == null) {
            reloadChildren = false;
            reloadChildcount = false;
            childcount = 0;
        } else if (reloadChildcount) {
            try {
                childcount = loadChildcount();
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            reloadChildcount = false;
        }
    }

    private void ensureChildrenLoaded() {
        if (nodeModel.getNode() == null) {
            reloadChildren = false;
            reloadChildcount = false;
            children = new ArrayList<TreeNode>();
        } else if (reloadChildren) {
            try {
                children = loadChildren();
                childcount = children.size();
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            reloadChildren = false;
            reloadChildcount = false;
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
        return new EqualsBuilder().append(nodeModel, treeNode.nodeModel).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(467, 17).append(nodeModel).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("nodeModel", nodeModel.toString())
                .toString();
    }

}
