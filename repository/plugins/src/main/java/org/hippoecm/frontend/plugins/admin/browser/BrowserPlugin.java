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
package org.hippoecm.frontend.plugins.admin.browser;

import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrTreeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.tree.JcrTree;

public class BrowserPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    JcrTreeModel treeModel;
    JcrTree tree;
    
    public BrowserPlugin(String id, JcrNodeModel model) {
        super(id, model);
        JcrNodeModel modelForTree = (JcrNodeModel) model.clone();
        treeModel = new JcrTreeModel(modelForTree);
        tree = new JcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
                Home home = (Home)getWebPage();
                home.update(target, (JcrNodeModel)treeNode);
            }
        };
        tree.getTreeState().expandNode((TreeNode) modelForTree.getRoot());
        add(tree);
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        /*
        HippoNode node = model.getNode();
        JcrNodeModel originalNode = treeModel.findJcrNode(node);
        if (originalNode != null) {
            try {
                System.out.println("original node: " + originalNode.getNode().getPath());
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            treeModel.nodeChanged(originalNode);
            tree.updateTree(target);
        }
        */
    }

}
