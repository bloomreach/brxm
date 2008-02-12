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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreeNode extends AbstractTreeNode {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FolderTreeNode.class);

    private static Set<String> restrictedChildTypes = new HashSet<String>();
    private static Set<String> unrestrictedChildTypes = new HashSet<String>();
    static {
        restrictedChildTypes.add(HippoNodeType.NT_HANDLE);
        restrictedChildTypes.add(HippoNodeType.NT_FACETSEARCH);
        restrictedChildTypes.add(HippoNodeType.NT_FACETSELECT);
        unrestrictedChildTypes.add("nt:base");
    }

    private boolean restricted = false;
    private FolderTreeNode parent;

    public FolderTreeNode(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public FolderTreeNode(JcrNodeModel nodeModel, FolderTreeNode parent) {
        super(nodeModel);
        this.parent = parent;
        try {
            this.restricted = parent.restricted;
            if (restricted) {
                if (nodeModel.getNode().isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    restricted = false;
                }
            } else {
                if (nodeModel.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                    restricted = true;
                }
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

        List<Node> subNodes = filter(nodeModel.getNode());
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
                result = node.getDisplayName();
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

    private List<Node> filter(Node node) throws RepositoryException {
        List<Node> result = new ArrayList<Node>();
        NodeIterator subNodes = node.getNodes();
        while (subNodes.hasNext()) {
            Node subNode = subNodes.nextNode();
            if (fits(subNode)) {
                result.add(subNode);
            } else {
                result.addAll(filter(subNode));
            }
        }
        return result;
    }

    private boolean fits(Node node) throws RepositoryException {
        Set<String> types = restricted ? restrictedChildTypes : unrestrictedChildTypes;
        for (String type : types) {
            if (node.isNodeType(type)) {
                return true;
            }
        }
        return false;
    }

}
