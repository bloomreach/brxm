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

import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.extensions.markup.html.tree.DefaultAbstractTree;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JcrTree extends Tree {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrTree.class);

    public JcrTree(String id, JcrTreeModel treeModel) {
        super(id, treeModel);

        setLinkType(LinkType.AJAX);

        ITreeState treeState = getTreeState();
        treeState.setAllowSelectMultiple(false);
        treeState.collapseAll();
        treeState.expandNode((TreeNode) treeModel.getRoot());
    }

    @Override
    protected String renderNode(TreeNode treeNode) {
        AbstractTreeNode treeNodeModel = (AbstractTreeNode) treeNode;
        return treeNodeModel.renderNode();
    }

    @Override
    protected ResourceReference getNodeIcon(TreeNode node) {
        boolean isVirtual = false;
        try {
            if (node instanceof AbstractTreeNode) {
                AbstractTreeNode treeNode = (AbstractTreeNode) node;
                HippoNode jcrNode = treeNode.getNodeModel().getNode();
                isVirtual = jcrNode.getCanonicalNode() == null || !jcrNode.getCanonicalNode().isSame(jcrNode);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        if (isVirtual) {
            return new ResourceReference(JcrTree.class, "icons/virtual.gif");
        }
        return super.getNodeIcon(node);

    }

}
