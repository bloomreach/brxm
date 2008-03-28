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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTreePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AbstractTreePlugin.class);

    protected Tree tree;
    protected AbstractTreeNode rootNode;

    public AbstractTreePlugin(PluginDescriptor pluginDescriptor, AbstractTreeNode rootNode, Plugin parentPlugin) {
        super(pluginDescriptor, rootNode.getNodeModel(), parentPlugin);

        this.rootNode = rootNode;
        JcrTreeModel treeModel = new JcrTreeModel(rootNode);
        tree = newTree(treeModel);
        add(tree);
    }

    /**
     * factory method which tree implementation to use. Override to use your own
     * @param treeModel
     * @return org.apache.wicket.extensions.markup.html.tree.Tree 
     */
    protected Tree newTree(JcrTreeModel treeModel) {
        return new JcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                AbstractTreeNode treeNodeModel = (AbstractTreeNode) clickedNode;
                AbstractTreePlugin.this.onSelect(treeNodeModel, target);
            }
        };
    }

    protected void onSelect(final AbstractTreeNode treeNodeModel, AjaxRequestTarget target) {
        Channel channel = getTopChannel();
        if (channel != null) {
            // create and send a "select" request with the node path as a parameter
            Request select = channel.createRequest("select", treeNodeModel.getNodeModel());
            channel.send(select);            
            select.getContext().apply(target);
            
            // create and send a "list" request with the children 
            // of the selected node as a parameters
            IPluginModel listModel = new PluginModel() {
                private static final long serialVersionUID = 1L;

                public Map<String, Object> getMapRepresentation() {
                    Map<String, Object> map = new HashMap<String, Object>();
                    try {
                        map.put("node", treeNodeModel.getNodeModel());
                        List<String> entries = new ArrayList<String>();
                        Enumeration<AbstractTreeNode> childNodes = treeNodeModel.children();
                        while (childNodes.hasMoreElements()) {
                            entries.add(childNodes.nextElement().getNodeModel().getNode().getPath());
                        }
                        map.put("entries", entries);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                    return map;
                }
            };
            Request list = channel.createRequest("list", listModel);
            channel.send(list);
            list.getContext().apply(target);
        }
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getModel());
            AbstractTreeNode node = null;
            while (model != null) {
                node = rootNode.getTreeModel().lookup(model);
                if (node != null) {
                    tree.getTreeState().selectNode(node, true);
                    break;
                }
                model = model.getParentModel();
            }
        } else if ("flush".equals(notification.getOperation())) {
            AbstractTreeNode node = rootNode.getTreeModel().lookup(new JcrNodeModel(notification.getModel()));
            if (node != null) {
                node.markReload();
                node.getTreeModel().nodeStructureChanged(node);
                notification.getContext().addRefresh(tree, "updateTree");
            }
        }
        super.receive(notification);
    }

}
