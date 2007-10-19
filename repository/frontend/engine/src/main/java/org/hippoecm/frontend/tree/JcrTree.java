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
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public abstract class JcrTree extends Tree {
    private static final long serialVersionUID = 1L;

    public JcrTree(String id, TreeNode rootNode) {
        super(id, new DefaultTreeModel(rootNode));
        setLinkType(LinkType.AJAX);

        ITreeState treeState = getTreeState();
        treeState.setAllowSelectMultiple(false);
        treeState.collapseAll();
        treeState.expandNode(rootNode);
    }

    protected String renderNode(TreeNode treeNode) {
        JcrNodeModel nodeModel = (JcrNodeModel) treeNode;
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

    public void nodeStructureChanged(TreeNode treeNode) {
        DefaultTreeModel treeModel = (DefaultTreeModel) getModelObject();
        treeModel.nodeStructureChanged(treeNode);
    }

}
