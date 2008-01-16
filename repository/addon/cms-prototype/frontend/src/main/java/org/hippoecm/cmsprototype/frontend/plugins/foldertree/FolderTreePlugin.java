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
package org.hippoecm.cmsprototype.frontend.plugins.foldertree;

import org.hippoecm.cmsprototype.frontend.model.content.Folder;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
import org.hippoecm.cmsprototype.frontend.model.tree.FolderTreeNode;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.tree.AbstractTreePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tree containing nodes of type FolderTreeNode
 *
 */
public class FolderTreePlugin extends AbstractTreePlugin {
    private static final long serialVersionUID = 1L;
    
    static final Logger log = LoggerFactory.getLogger(FolderTreePlugin.class);

    public FolderTreePlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new FolderTreeNode(model), parentPlugin);
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getData());
            try {
                Folder folder = new Folder(model);
                AbstractTreeNode node = rootNode.getTreeModel().lookup(folder.getNodeModel());
                if (node != null) {
                    // node exists in tree -> select it
                    tree.getTreeState().selectNode(node, true);
                    notification.getContext().addRefresh(tree, "updateTree");
                }
                else {
                    // find first ancestor which does exist in tree and select it
                    JcrNodeModel folderModel = folder.getNodeModel();
                    while (node == null) {
                        folderModel = folderModel.getParentModel();
                        node = rootNode.getTreeModel().lookup(folderModel);
                    }
                    if (node != null) {
                        tree.getTreeState().selectNode(node, true);
                        notification.getContext().addRefresh(tree, "updateTree");
                    }
                }
            } catch (ModelWrapException e) {
                log.error(e.getMessage());
            }
        }
    }
    
}
