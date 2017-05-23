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
package org.hippoecm.frontend.plugins.standards.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeNameComparator;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreeNode extends JcrTreeNode {

    static final Logger log = LoggerFactory.getLogger(FolderTreeNode.class);

    /**
     * Comparator by folder display name.
     */
    private static final Comparator<IJcrTreeNode> DISPLAY_NAME_COMPARATOR = new JcrTreeNameComparator();

    /**
     * Returns {@link #DISPLAY_NAME_COMPARATOR} if the current folder node is of {@link HippoStdNodeType#NT_DIRECTORY}
     * type. Otherwise, returns null, meaning to follow the natural JCR ordering on child folders.
     * @param model
     * @return
     */
    private static Comparator<IJcrTreeNode> getDisplayNameComparatorIfDirectoryNode(JcrNodeModel model) {
        try {
            if (model.getNode().isNodeType(HippoStdNodeType.NT_DIRECTORY)) {
                return DISPLAY_NAME_COMPARATOR;
            }
        } catch (RepositoryException e) {
            log.error("Failed to check folder node type.", e);
        }

        return null;
    }

    /**
     * Document list filter configuration.
     */
    private DocumentListFilter config;

    /**
     * Custom child comparator which can be provided for custom sorting use cases through configuration.
     */
    private Comparator<IJcrTreeNode> customChildComparator;

    public FolderTreeNode(JcrNodeModel model, DocumentListFilter config) {
        this(model, config, null);
    }

    public FolderTreeNode(JcrNodeModel model, DocumentListFilter config,
            Comparator<IJcrTreeNode> customChildComparator) {
        super(model, null, (customChildComparator != null) ? customChildComparator
                : getDisplayNameComparatorIfDirectoryNode(model));
        this.config = config;
        this.customChildComparator = customChildComparator;
    }

    private FolderTreeNode(JcrNodeModel model, FolderTreeNode parent, Comparator<IJcrTreeNode> customChildComparator) {
        super(model, parent);
        this.config = parent.config;
        this.customChildComparator = customChildComparator;
    }

    @Override
    public IJcrTreeNode getChild(String name) throws RepositoryException {
        final Node chainedModelObject = getChainedModel().getObject();
        if (chainedModelObject.hasNode(name)) {
            JcrNodeModel childModel = new JcrNodeModel(chainedModelObject.getNode(name));
            return new FolderTreeNode(childModel, this, customChildComparator);
        }
        return null;
    }

    @Override
    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    @Override
    public int getChildCount() {
        Node jcrNode = this.nodeModel.getObject();
        if (jcrNode instanceof HippoNode) {
            try {
                final HippoNode hippoNode = (HippoNode) jcrNode;
                // do not count for virtual nodes w.r.t performance
                if (hippoNode.isVirtual()) {
                    return 0;
                }
            } catch (RepositoryException e) {
                log.warn("Unable to get child count", e);
            }
        }
        return super.getChildCount();
    }

    /**
     * {@inheritDoc}
     * <P>
     * Overrides to filter out child nodes based on folder filter configuration.
     * </P>
     */
    @Override
    protected List<Node> loadChildNodes() throws RepositoryException {
        List<Node> result = new ArrayList<Node>();

        NodeIterator subNodes = config.filter(nodeModel.getNode(), nodeModel.getNode().getNodes());
        while (subNodes.hasNext()) {
            Node subNode = subNodes.nextNode();
            if (subNode.isNodeType(HippoNodeType.NT_TRANSLATION)) {
                continue;
            }
            result.add(subNode);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * <P>
     * Overrides to create {@link FolderTreeNode} instead of {@link JcrTreeNode}.
     * </P>
     */
    @Override
    protected JcrTreeNode createChildJcrTreeNode(JcrNodeModel childNodeModel)  {
        return new FolderTreeNode(childNodeModel, this, customChildComparator);
    }

    /**
     * {@inheritDoc}
     * <P>
     * Overrides to return a custom child comparator if set to any in configuration.
     * Otherwise, follows the default behavior of {@link JcrTreeNode}.
     * </P>
     */
    @Override
    protected Comparator<IJcrTreeNode> getChildComparator() {
        if (customChildComparator != null) {
            return customChildComparator;
        }

        return super.getChildComparator();
    }

    /**
     * {@inheritDoc}
     * <P>
     * Overrides to sort children by the {@link #customChildComparator} if set to any.
     * Otherwise, follow the default behavior of {@link JcrTreeNode}.
     * </P>
     */
    @Override
    protected void sortChildTreeNodes(List<IJcrTreeNode> childTreeNodes) throws RepositoryException {
        if (customChildComparator != null) {
            Node baseNode = nodeModel.getNode();

            if (!baseNode.isNodeType(HippoNodeType.NT_FACETRESULT)) {
                Collections.sort(childTreeNodes,  customChildComparator);
            }
        } else {
            super.sortChildTreeNodes(childTreeNodes);
        }
    }

}
