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
package org.hippoecm.frontend.tree;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.apache.wicket.model.AbstractReadOnlyModel;

import org.hippoecm.frontend.model.JcrNodeModel;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public abstract class JcrTree extends Tree {
    private static final long serialVersionUID = 1L;

    public JcrTree(String id, LazyTreeModel treeModel) {
        super(id, treeModel);
        
        setLinkType(LinkType.AJAX);
        getTreeState().setAllowSelectMultiple(false);
        getTreeState().collapseAll();
        
        getTreeState().addTreeStateListener(new JcrTreeStateListener());
    }

    protected String renderNode(JcrLazyTreeNode treeNode) {
        JcrNodeModel nodeModel = treeNode.getJcrNodeModel();
        HippoNode node = nodeModel.getNode();
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
    
    protected abstract void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode);
    
    private class JcrTreeStateListener implements ITreeStateListener, IClusterable {
        private static final long serialVersionUID = 1L;

        public void nodeExpanded(TreeNode treeNodeModel) {
        }

        public void nodeCollapsed(TreeNode treeNodeModel) {
        }

        public void allNodesCollapsed() {
        }

        public void allNodesExpanded() {
        }

        public void nodeSelected(TreeNode node) {
        }

        public void nodeUnselected(TreeNode node) {
        }

    }

}
