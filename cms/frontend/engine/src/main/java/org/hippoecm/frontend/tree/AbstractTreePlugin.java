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

import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;

public abstract class AbstractTreePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private JcrTree tree;
    private AbstractTreeNode rootNode;

    public AbstractTreePlugin(PluginDescriptor pluginDescriptor, AbstractTreeNode rootNode, Plugin parentPlugin) {
        super(pluginDescriptor, rootNode.getNodeModel(), parentPlugin);

        this.rootNode = rootNode;
        JcrTreeModel treeModel = new JcrTreeModel(rootNode);
        tree = new JcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                AbstractTreeNode treeNodeModel = (AbstractTreeNode) clickedNode;
                AbstractTreePlugin.this.onSelect(treeNodeModel, target);
            }
        };
        add(tree);
    }

    protected void onSelect(AbstractTreeNode treeNodeModel, AjaxRequestTarget target) {
    	Channel channel = getDescriptor().getIncoming();
    	if(channel != null) {
	        // create a "select" request with the node path as a parameter
	        Request request = channel.createRequest("select",
	        		treeNodeModel.getNodeModel().getMapRepresentation());
	        
	        // send the request to the incoming channel
	        channel.send(request);
	
	        // add all components that have changed (and are visible!)
	        request.getContext().apply(target);
    	}
    }

    @Override
    public void receive(Notification notification) {
        Request request = notification.getRequest();
        if (request != null) {
            if ("select".equals(notification.getOperation())) {
                JcrNodeModel model = new JcrNodeModel(request.getData());
                AbstractTreeNode node = null;
                while (model != null) {
                    node = rootNode.getTreeModel().lookup(model);
                    if (node != null) {
                        tree.getTreeState().selectNode(node, true);
                        break;
                    } else {
                        model = model.getParentModel();
                    }
                }
            } else if ("flush".equals(notification.getOperation())) {
                AbstractTreeNode node = rootNode.getTreeModel().lookup(new JcrNodeModel(request.getData()));
                if (node != null) {
                    node.markReload();
                    node.getTreeModel().nodeStructureChanged(node);
                    request.getContext().addRefresh(tree, "updateTree");
                }
            }
        }
        super.receive(notification);
    }
}
