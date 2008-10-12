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
package org.hippoecm.frontend.plugins.cms.browse.tree;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
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
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(FolderTreeNode.class);

    private FolderTreeNode parent;
    private FolderTreeConfig config;

    public FolderTreeNode(JcrNodeModel model, FolderTreeConfig config) {
        super(model);
        this.parent = null;
        this.config = config;
    }

    public FolderTreeNode(JcrNodeModel model, FolderTreeNode parent) {
        super(model);
        this.parent = parent;
        this.config = parent.config;
        setTreeModel(parent.getTreeModel());
        getTreeModel().register(this);
    }

    @Override
    protected int loadChildcount() throws RepositoryException {
        HippoNode jcrNode = nodeModel.getNode();
        // do not count for virtual nodes w.r.t performance
        if (jcrNode.isNodeType(HippoNodeType.NT_FACETRESULT)
                || jcrNode.isNodeType(HippoNodeType.NT_FACETSEARCH)
                || jcrNode.getCanonicalNode() == null
                || !jcrNode.getCanonicalNode().isSame(jcrNode) ) {
            return  1;
        } else {
            return loadChildren().size();
        }
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

    @Override
    public int getChildCount() {
        HippoNode jcrNode = this.nodeModel.getNode();
        try {
            // do not count for virtual nodes w.r.t performance
            if (jcrNode.getCanonicalNode() == null || !jcrNode.getCanonicalNode().isSame(jcrNode)) {
               return 1;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
       return super.getChildCount();
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
                result = config.getDisplayName(node);
                result = ISO9075Helper.decodeLocalName(result);
                if (node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                    result += " [" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong() + "]";
                }
            } catch (ValueFormatException e) {
                // ignore the hippo count if not of type long
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

    private List<Node> subNodes(Node node) throws RepositoryException {
        List<Node> result = new ArrayList<Node>();

        NodeIterator subNodes = config.filter(node, node.getNodes());
        while (subNodes.hasNext()) {
            Node subNode = subNodes.nextNode();
            result.add(subNode);
        }
        return result;
    }
}
