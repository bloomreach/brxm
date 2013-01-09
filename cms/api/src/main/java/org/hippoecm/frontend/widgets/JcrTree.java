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
package org.hippoecm.frontend.widgets;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.ILabelTreeNode;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JcrTree extends Tree {
    private static final long serialVersionUID = 1L;

    /** use own styling */
    private static final ResourceReference TREE_STYLE = new ResourceReference(JcrTree.class, "res/tree.css");

    /** Reference to the icon of open tree folder */
    private static final ResourceReference VIRTUAL_FOLDER_OPEN = new ResourceReference(JcrTree.class,
            "icons/folder-open-virtual.gif");

    private static final ResourceReference VIRTUAL_FOLDER_CLOSED = new ResourceReference(JcrTree.class,
            "icons/folder-closed-virtual.gif");

    /** Reference to the icon of tree item (not a folder) */
    private static final ResourceReference VIRTUAL_ITEM = new ResourceReference(JcrTree.class, "icons/item-virtual.gif");

    static final Logger log = LoggerFactory.getLogger(JcrTree.class);

    public JcrTree(String id, TreeModel treeModel) {
        super(id, treeModel);
        setLinkType(LinkType.AJAX);

        ITreeState treeState = getTreeState();
        treeState.setAllowSelectMultiple(false);
        treeState.expandNode(treeModel.getRoot());
    }

    @Override
    protected ResourceReference getCSS() {
        return TREE_STYLE;
    }

    @Override
    protected abstract void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode);

    @Override
    public String renderNode(TreeNode treeNode) {
        String result = "unknown";
        if (treeNode instanceof IJcrTreeNode) {
            Node node = ((IJcrTreeNode) treeNode).getNodeModel().getObject();
            if (node != null) {
                try {
                    result = node.getName();
                    if ((node instanceof HippoNode) && !node.isNodeType(HippoNodeType.NT_FACETSEARCH)) {
                        if (node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                            result += " [" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong() + "]";
                        }
                    }
                } catch (RepositoryException e) {
                    result = e.getMessage();
                }
            }
        } else if (treeNode instanceof ILabelTreeNode) {
            return ((ILabelTreeNode) treeNode).getLabel();
        }
        return result;
    }

    /**
     * Returns the resource reference for icon of specified tree node.
     * 
     * @param node
     *            The node
     * @return The package resource reference
     */
    @Override
    protected ResourceReference getNodeIcon(TreeNode node) {
        if (node instanceof IJcrTreeNode && isVirtual((IJcrTreeNode) node)) {
            if (node.isLeaf()) {
                return getVirtualItem();
            } else {
                if (isNodeExpanded(node)) {
                    return getVirtualFolderOpen();
                } else {
                    return getVirtualFolderClosed();
                }
            }
        } else {
            return super.getNodeIcon(node);
        }
    }

    /**
     * Checks if the wrapped jcr node is a virtual node
     * @return true if the node is virtual else false
     */
    public boolean isVirtual(IJcrTreeNode node) {
        IModel<Node> nodeModel = node.getNodeModel();
        if (nodeModel == null) {
            return false;
        }
        Node jcrNode = nodeModel.getObject();
        if (jcrNode == null || !(jcrNode instanceof HippoNode)) {
            return false;
        }
        try {
            HippoNode hippoNode = (HippoNode) jcrNode;
            Node canonical = hippoNode.getCanonicalNode();
            if (canonical == null) {
                return true;
            }
            return !canonical.isSame(hippoNode);
        } catch (ItemNotFoundException e) {
            // canonical node no longer exists
            return true;
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Returns the resource reference of default closed tree folder.
     * 
     * @return The package resource reference
     */
    protected ResourceReference getVirtualFolderClosed() {
        return VIRTUAL_FOLDER_CLOSED;
    }

    /**
     * Returns the resource reference of default open tree folder.
     * 
     * @return The package resource reference
     */
    protected ResourceReference getVirtualFolderOpen() {
        return VIRTUAL_FOLDER_OPEN;
    };

    /**
     * Returns the resource reference of default tree item (not folder).
     * 
     * @return The package resource reference
     */
    protected ResourceReference getVirtualItem() {
        return VIRTUAL_ITEM;
    }

}
