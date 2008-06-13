/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.legacy.tree;

import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.legacy.plugin.channel.Request;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.widgets.JcrTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated use org.hippoecm.frontend.plugins.cms.browse.sa.* instead
 */
@Deprecated
public abstract class AbstractTreePlugin extends Plugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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
                    TreeNode parentNode = (AbstractTreeNode)node.getParent();
                    while(parentNode!=null && !tree.getTreeState().isNodeExpanded(parentNode)) {
                        tree.getTreeState().expandNode(parentNode);
                        parentNode = parentNode.getParent();
                    }
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
