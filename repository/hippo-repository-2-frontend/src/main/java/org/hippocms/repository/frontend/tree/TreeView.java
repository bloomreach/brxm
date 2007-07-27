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
package org.hippocms.repository.frontend.tree;

import javax.jcr.RepositoryException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.hippocms.repository.frontend.BrowserSession;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.jr.servicing.ServicingNode;

public class TreeView extends Tree {
    private static final long serialVersionUID = 1L;

    public TreeView(String id, TreeModel treeModel) {
        super(id, treeModel);
        
        setLinkType(LinkType.AJAX);
        getTreeState().setAllowSelectMultiple(false);
        getTreeState().collapseAll();
    }

    protected String renderNode(TreeNode treeNode) {
        JcrNodeModel nodeModel = (JcrNodeModel) treeNode;
        ServicingNode node = nodeModel.getNode();
        String result = "null";
        if (node != null) {
            try {
                result = node.getDisplayName();
                if (node.hasProperty("hippo:count")) {
                    result += " [" + node.getProperty("hippo:count").getLong() + "]";
                }
            } catch (RepositoryException e) {
                result = e.getMessage();
            }
        }
        return result;
    }
          
    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
        BrowserSession session = (BrowserSession)getSession();
        session.updateAll(target, (JcrNodeModel)treeNode);
    }

}
