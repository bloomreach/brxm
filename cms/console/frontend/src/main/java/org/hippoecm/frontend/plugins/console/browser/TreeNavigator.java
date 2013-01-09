/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.browser;

import java.util.Collection;

import javax.swing.tree.TreeNode;

import org.apache.wicket.IClusterable;
import org.apache.wicket.markup.html.tree.ITreeState;

public class TreeNavigator implements IClusterable {
    private final ITreeState treeState;

    public TreeNavigator(final ITreeState state) {
        this.treeState = state;
    }

    private TreeNode getSelectedNode() {
        final Collection<Object> selectedNodes = treeState.getSelectedNodes();
        if (selectedNodes.size() != 1) {
            return null;
        }
        return (TreeNode) selectedNodes.iterator().next();
    }

    public void up() {
        TreeNode node = getSelectedNode();
        if (node == null) {
            return;
        }

        final TreeNode parent = node.getParent();
        if (parent != null) {
            final int nodeIndex = parent.getIndex(node);
            TreeNode newSelection;
            if (nodeIndex > 0) {
                newSelection = parent.getChildAt(nodeIndex - 1);
                while (treeState.isNodeExpanded(newSelection) && (newSelection.getChildCount() > 0)) {
                    TreeNode candidate = newSelection.getChildAt(newSelection.getChildCount() - 1);
                    if (candidate.equals(node)) {
                        break;
                    } else {
                        newSelection = candidate;
                    }
                }
            } else {
                newSelection = parent;
            }

            treeState.selectNode(newSelection, true);
        }
    }

    public void down() {
        TreeNode node = getSelectedNode();
        if (node == null) {
            return;
        }

        TreeNode newSelection = null;
        if (treeState.isNodeExpanded(node) && node.getChildCount() > 0) {
            newSelection = node.getChildAt(0);
        } else {
            do {
                final TreeNode parent = node.getParent();
                if (parent == null) {
                    break;
                }
                final int nodeIndex = parent.getIndex(node);
                if (nodeIndex < (parent.getChildCount() - 1)) {
                    newSelection = parent.getChildAt(nodeIndex + 1);
                    break;
                } else {
                    node = parent;
                }
            } while (newSelection == null);
        }

        if (newSelection != null) {
            treeState.selectNode(newSelection, true);
        }
    }

    public void left() {
        TreeNode node = getSelectedNode();
        if (node == null) {
            return;
        }

        if (treeState.isNodeExpanded(node)) {
            treeState.collapseNode(node);
        } else {
            treeState.selectNode(node.getParent(), true);
        }
    }

    public void right() {
        TreeNode node = getSelectedNode();
        if (node == null) {
            return;
        }

        if (!treeState.isNodeExpanded(node)) {
            treeState.expandNode(node);
        } else if (node.getChildCount() > 0) {
            treeState.selectNode(node.getChildAt(0), true);
        }
    }
}
