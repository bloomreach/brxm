/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.tree;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreeNode extends JcrTreeNode {
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(FolderTreeNode.class);

    private DocumentListFilter config;

    public FolderTreeNode(JcrNodeModel model, DocumentListFilter config) {
        super(model, null);
        this.config = config;
    }

    private FolderTreeNode(JcrNodeModel model, FolderTreeNode parent) {
        super(model, parent);
        this.config = parent.config;
    }

    @Override
    public IJcrTreeNode getChild(String name) throws RepositoryException {
        if (getChainedModel().getObject().hasNode(name)) {
            JcrNodeModel childModel = new JcrNodeModel(getChainedModel().getObject().getNode(name));
            return new FolderTreeNode(childModel, this);
        }
        return null;
    }

    @Override
    protected List<IJcrTreeNode> loadChildren() throws RepositoryException {
        List<IJcrTreeNode> result = new ArrayList<IJcrTreeNode>();
        List<Node> subNodes = subNodes(nodeModel.getObject());
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
        Node jcrNode = this.nodeModel.getObject();
        if (jcrNode instanceof HippoNode) {
            try {
                HippoNode hippoNode = (HippoNode) jcrNode;
                // do not count for virtual nodes w.r.t performance
                if (hippoNode.getCanonicalNode() == null || !hippoNode.getCanonicalNode().isSame(hippoNode)) {
                    return 0;
                }
            } catch (RepositoryException e) {
                log.warn("Unable to get child count: " + e.getMessage());
            }
        }
        return super.getChildCount();
    }

    private List<Node> subNodes(Node node) throws RepositoryException {
        List<Node> result = new ArrayList<Node>();

        NodeIterator subNodes = config.filter(node, node.getNodes());
        while (subNodes.hasNext()) {
            Node subNode = subNodes.nextNode();
            if (subNode.isNodeType(HippoNodeType.NT_TRANSLATION)) {
                continue;
            }
            result.add(subNode);
        }
        return result;
    }
}
