/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTreeNode extends NodeModelWrapper<JcrTreeNode> implements IJcrTreeNode {

    static final Logger log = LoggerFactory.getLogger(JcrTreeNode.class);

    static final int DETACHING = 0x00000001;

    private List<IJcrTreeNode> children;

    private final int hashCode;
    private boolean reloadChildren = true;
    private boolean reloadChildCount = true;
    private int childCount = -1;
    private IJcrTreeNode parent;
    private transient int flags = 0;
    private Comparator<IJcrTreeNode> childComparator;

    public JcrTreeNode(IModel<Node> nodeModel, IJcrTreeNode parent) {
        super(nodeModel);

        if (nodeModel == null) {
            throw new RuntimeException("JcrTreeNode instantiated with null model");
        }
        this.parent = parent;
        this.hashCode = nodeModel.hashCode();
    }

    public JcrTreeNode(IModel<Node> nodeModel, IJcrTreeNode parent, Comparator<IJcrTreeNode> childComparator) {
        this(nodeModel, parent);
        this.childComparator = childComparator;
    }

    /**
     * Resolve the child that corresponds to a particular name.
     * @param name
     * @return child node
     * @throws RepositoryException
     */
    public IJcrTreeNode getChild(String name) throws RepositoryException {
        final Node chainedModelObject = getChainedModel().getObject();
        if (chainedModelObject.hasNode(name)) {
            JcrNodeModel childModel = new JcrNodeModel(chainedModelObject.getNode(name));
            return createChildJcrTreeNode(childModel);
        }
        return null;
    }

    // implement TreeNode

    public TreeNode getParent() {
        return parent;
    }

    public Enumeration<? extends TreeNode> children() {
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
                Node node = nodeModel.getObject();
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

    /**
     * Ensure child tree nodes sorted properly.
     */
    public void ensureChildrenSorted() {
        try {
            ensureChildrenLoaded();
            sortChildTreeNodes(children);
        } catch (RepositoryException e) {
            log.error("Failed to ensure children sorted.", e);
        }
    }

    // implement IDetachable

    @Override
    public void detach() {
        if ((flags & DETACHING) == 0) {
            flags = flags | DETACHING;
            reloadChildren = true;
            reloadChildCount = true;
            if (children != null) {
                for (TreeNode child : children) {
                    if (child instanceof IDetachable) {
                        ((IDetachable) child).detach();
                    }
                }
                children = null;
            }
            if (parent != null) {
                parent.detach();
            }
            super.detach();
            flags = flags & (0xFFFFFFFF ^ DETACHING);
        }
    }

    /**
     * Loads child tree nodes.
     * @return loaded child tree nodes
     * @throws RepositoryException if repository exception occurs
     */
    protected List<IJcrTreeNode> loadChildren() throws RepositoryException {
        List<IJcrTreeNode> treeNodes = new ArrayList<IJcrTreeNode>();

        for (Node childNode : loadChildNodes()) {
            treeNodes.add(createChildJcrTreeNode(new JcrNodeModel(childNode)));
        }

        sortChildTreeNodes(treeNodes);

        return treeNodes;
    }

    /**
     * Loads child nodes.
     * @return loaded child nodes
     * @throws RepositoryException if repository exception occurs
     */
    protected List<Node> loadChildNodes() throws RepositoryException {
        List<Node> childNodes = null;

        Node childNode;

        for (NodeIterator nodeIt = nodeModel.getObject().getNodes(); nodeIt.hasNext(); ) {
            childNode = nodeIt.nextNode();

            if (childNode != null) {
                if (childNodes == null) {
                    childNodes = new LinkedList<>();
                }

                childNodes.add(childNode);
            }
        }

        return (childNodes != null) ? childNodes : Collections.emptyList();
    }

    /**
     * Creates child tree node.
     * @param childNodeModel child node model
     * @return child tree node.
     */
    protected JcrTreeNode createChildJcrTreeNode(JcrNodeModel childNodeModel) {
        return new JcrTreeNode(childNodeModel, this, childComparator);
    }

    /**
     * Sort child tree nodes.
     * @param childTreeNodes child tree nodes
     * @throws RepositoryException if repository exception occurs
     */
    protected void sortChildTreeNodes(List<IJcrTreeNode> childTreeNodes) throws RepositoryException {
        if (childComparator != null) {
            Node baseNode = nodeModel.getNode();

            if (!baseNode.getPrimaryNodeType().hasOrderableChildNodes()
                    && !baseNode.isNodeType(HippoNodeType.NT_FACETRESULT)) {
                Collections.sort(childTreeNodes,  childComparator);
            }
        }
    }

    private void ensureChildrenLoaded() {
        if (nodeModel.getObject() == null) {
            reloadChildren = false;
            reloadChildCount = false;
            children = new ArrayList<>();
            childCount = 0;
        } else if (children == null || reloadChildren) {
            try {
                children = loadChildren();
                childCount = children.size();
            } catch (RepositoryException e) {
                log.warn("Unable to load children, setting empty list: " + e.getMessage());
                children = new ArrayList<>();
                childCount = 0;
            }
            reloadChildren = false;
            reloadChildCount = false;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof JcrTreeNode)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrTreeNode treeNode = (JcrTreeNode) object;
        if (!nodeModel.equals(treeNode.getChainedModel())) {
            return false;
        }
        if (parent == treeNode.parent) {
            return true;
        } else if (parent == null || treeNode.parent == null) {
            return false;
        } else {
            return parent.equals(treeNode.parent);
        }
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("nodeModel", nodeModel.toString())
                .toString();
    }
}
