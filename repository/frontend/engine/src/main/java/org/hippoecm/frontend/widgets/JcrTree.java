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
package org.hippoecm.frontend.widgets;

import javax.swing.tree.TreeNode;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JcrTree extends Tree {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    /** Reference to the icon of open tree folder */
    private static final ResourceReference VIRTUAL_FOLDER_OPEN = new ResourceReference(JcrTree.class,
            "icons/folder-open-virtual.gif");

    private static final ResourceReference VIRTUAL_FOLDER_CLOSED = new ResourceReference(JcrTree.class,
            "icons/folder-closed-virtual.gif");
    
    /** Reference to the icon of tree item (not a folder) */
    private static final ResourceReference VIRTUAL_ITEM = new ResourceReference(JcrTree.class,
            "icons/item-virtual.gif");


    static final Logger log = LoggerFactory.getLogger(JcrTree.class);

    private JcrTreeModel treeModel;

    public JcrTree(String id, JcrTreeModel treeModel) {
        super(id, treeModel);
        this.treeModel = treeModel;
        setLinkType(LinkType.AJAX);

        ITreeState treeState = getTreeState();
        treeState.setAllowSelectMultiple(false);
        treeState.collapseAll();
        treeState.expandNode((TreeNode) treeModel.getRoot());
    }

    protected abstract void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode);

    @Override
    public void onDetach() {
        treeModel.detach();
        super.onDetach();
    }
    
    @Override
    protected String renderNode(TreeNode treeNode) {
        AbstractTreeNode treeNodeModel = (AbstractTreeNode) treeNode;
        return treeNodeModel.renderNode();
    }

    /**
     * Returns the resource reference for icon of specified tree node.
     * 
     * @param node
     *            The node
     * @return The package resource reference
     */
    protected ResourceReference getNodeIcon(TreeNode node) {
        if (node instanceof JcrTreeNode && ((JcrTreeNode) node).isVirtual()) {
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
