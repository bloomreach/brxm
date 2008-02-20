/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.foldertree;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreeNode extends AbstractTreeNode {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FolderTreeNode.class);

    private boolean onlyHandles = false;
    private FolderTreeNode parent;

    public FolderTreeNode(JcrNodeModel model) {
        super(model);
    }

    public FolderTreeNode(JcrNodeModel model, FolderTreeNode parent) {
        super(model);
        this.parent = parent;
        this.onlyHandles = parent.onlyHandles;
        try {
            Node node = nodeModel.getNode();
            if (onlyHandles) {
                if (isFacetSelect(node) && !isReferenceToHandle(node)) {
                    onlyHandles = false;
                }
            } else {
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    onlyHandles = true;
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        setTreeModel(parent.getTreeModel());
        getTreeModel().register(this);
    }

    @Override
    protected int loadChildcount() throws RepositoryException {
        return loadChildren().size();
    }

    @Override
    protected List<AbstractTreeNode> loadChildren() throws RepositoryException {
        List<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();

        List<Node> subNodes = subNodes(nodeModel.getNode());
        for (Node subNode : subNodes) {
            FolderTreeNode subfolder = new FolderTreeNode(new JcrNodeModel(subNode), this);
            result.add(subfolder);
        }
        return result;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public TreeNode getParent() {
        return parent;
    }

    @Override
    public String renderNode() {
        HippoNode node = getNodeModel().getNode();
        String result = "null";
        if (node != null) {
            try {
                result = ISO9075Helper.decodeLocalName(node.getDisplayName());
                if (node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                    result += " [" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong() + "]";
                }
            } catch (RepositoryException e) {
                result = e.getMessage();
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("nodeModel", nodeModel.toString())
                .toString();
    }

    // privates

    private List<Node> subNodes(Node node) throws RepositoryException {
        List<Node> result = new ArrayList<Node>();
        NodeIterator subNodes = node.getNodes();
        while (subNodes.hasNext()) {
            Node subNode = subNodes.nextNode();
            if (subNode.isNodeType(HippoNodeType.NT_HANDLE) || isReferenceToHandle(subNode)) {
                result.add(subNode);
            } else {
                if (onlyHandles) {
                    result.addAll(subNodes(subNode));
                } else {
                    result.add(subNode);
                }
            }
        }
        return result;
    }

    private boolean isFacetSelect(Node node) {
        boolean result;
        try {
            result = node.isNodeType(HippoNodeType.NT_FACETSELECT);
        } catch (RepositoryException e) {
            result = false;
        }
        return result;
    }

    private boolean isReferenceToHandle(Node node) {
        boolean result;
        try {
            result = node.isNodeType(HippoNodeType.NT_FACETSELECT);
            HippoNode virtualchild = (HippoNode) node.getNodes().nextNode();
            Node referencedNode = virtualchild.getCanonicalNode().getParent();
            result &= referencedNode.isNodeType(HippoNodeType.NT_HANDLE);
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

}
