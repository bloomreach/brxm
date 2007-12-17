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
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.frontend.tree.JcrTree;

public class BrowserPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private JcrTree tree;
    private AbstractTreeNode rootNodeModel;

    public BrowserPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel nodeModel, Plugin parentPlugin) {
        super(pluginDescriptor, nodeModel, parentPlugin);

		rootNodeModel = new JcrTreeNode(nodeModel);
        //rootNodeModel = new DocumentTreeNode(nodeModel);
        JcrTreeModel treeModel = new JcrTreeModel(rootNodeModel);

        tree = new JcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                AbstractTreeNode treeNodeModel = (AbstractTreeNode) clickedNode;
                PluginEvent event = new PluginEvent(BrowserPlugin.this, JcrEvent.NEW_MODEL, treeNodeModel.getNodeModel());
                getPluginManager().update(target, event);
            }
        };
        add(tree);
    }

    public void update(AjaxRequestTarget target, PluginEvent event) {
        JcrTreeModel treeModel = rootNodeModel.getTreeModel();
        
        JcrNodeModel nodeToBeReloaded = event.getNodeModel(JcrEvent.NEEDS_RELOAD);
        if (nodeToBeReloaded != null) {    
            AbstractTreeNode treeNodeModel = treeModel.lookup(nodeToBeReloaded);

            treeNodeModel.markReload();
            tree.getTreeModel().nodeStructureChanged(treeNodeModel);
            if (target != null && findPage() != null) {
                tree.updateTree(target);
            }
        }
        
        JcrNodeModel newSelection = event.getNodeModel(JcrEvent.NEW_MODEL);
        if(newSelection != null) {
            AbstractTreeNode node = treeModel.lookup(newSelection);
            if (node != null) {
                tree.getTreeState().selectNode(node, true);
            }
        }
    }

}
