/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.yui.tree;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.tree.TreeBehavior.DefaultTreeItem;

public class YuiJcrTree extends Panel {
    private static final long serialVersionUID = 1L;


    public YuiJcrTree(String id, IPluginConfig config, final IModel model) {
        super(id);

        TreeSettings settings = new TreeSettings(config);

        add(new TreeBehavior(settings) {
            private static final long serialVersionUID = 1L;

            @Override
            protected TreeItem getRootNode() {
                JcrNodeModel rootModel = new JcrNodeModel(settings.getRoot());
                Node root = rootModel.getNode();
                if (root != null) {
                    try {
                        String selected = model != null && model.getObject() != null ? (String) model.getObject()
                                : root.getUUID();
                        return new JcrTreeItem(root, selected);
                    } catch (RepositoryException e) {
                        log.error("Error creating root node for JcrYuiTree on root[" + settings.getRoot() + "]", e);
                    }
                }
                return null;
            }

            @Override
            protected void onClick(AjaxRequestTarget target, String uuid) {
                YuiJcrTree.this.onClick(target, uuid);
            }

            @Override
            protected void onDblClick(AjaxRequestTarget target, String uuid) {
                YuiJcrTree.this.onDblClick(target, uuid);
            }

        });
    }

    protected void onDblClick(AjaxRequestTarget target, String uuid) {
    }

    protected void onClick(AjaxRequestTarget target, String uuid) {
    }

    public static class JcrTreeItem extends DefaultTreeItem {
        private static final long serialVersionUID = 1L;

        public JcrTreeItem(Node node, String selectedUUID) throws RepositoryException {
            super(getLabel(node), getUUID(node), 0);

            if (selectedUUID != null && selectedUUID.equals(uuid)) {
                setExpanded(true);
            }
            if (node.hasNodes()) {
                NodeIterator it = node.getNodes();
                List<JcrTreeItem> items = new LinkedList<JcrTreeItem>();
                while (it.hasNext()) {
                    //add filter
                    Node childNode = it.nextNode();
                    if (childNode.isNodeType("hippostd:folder")) {
                        items.add(new JcrTreeItem(childNode, selectedUUID));
                    }
                }
                children = new JcrTreeItem[items.size()];
                for (JcrTreeItem item : items) {
                    addChild(item);
                }
            }
        }

        private static String getUUID(Node node) throws RepositoryException {
            try {
                return node.getUUID();
            } catch (UnsupportedRepositoryOperationException e) {
                return "";
            }
        }

        private static String getLabel(Node node) throws RepositoryException {
            return node.getName();
        }

    }

}
