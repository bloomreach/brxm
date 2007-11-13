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
import org.hippoecm.frontend.model.JcrTreeNode;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.tree.JcrTree;

public class BrowserPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private JcrTree tree;

    public BrowserPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        JcrNodeModel treeNode = new JcrTreeNode(null, model.getNode());
        
        tree = new JcrTree("tree", treeNode) {
            private static final long serialVersionUID = 1L;

            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                JcrNodeModel jcrTreeNode = (JcrNodeModel) clickedNode;
                JcrEvent jcrEvent = new JcrEvent(jcrTreeNode, false);
                
                getPluginManager().update(target, jcrEvent);
            }
        };
        add(tree);
    }

    public void update(AjaxRequestTarget target, JcrEvent jcrEvent) {
        if (jcrEvent.structureChanged()) {
            JcrNodeModel treeNode = jcrEvent.getModel();
            tree.nodeStructureChanged(treeNode);
            tree.updateTree(target);
        }
    }

}
