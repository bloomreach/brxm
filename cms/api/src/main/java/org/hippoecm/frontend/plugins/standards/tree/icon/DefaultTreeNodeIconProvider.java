/*
 *  Copyright 2010-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.tree.icon;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.tree.FolderTreeNode;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultTreeNodeIconProvider extends AbstractJcrTreeNodeIconProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultTreeNodeIconProvider.class);

    public HippoIcon getNodeIcon(final String id, final TreeNode treeNode, final ITreeState state) {
        if (state.isNodeExpanded(treeNode)) {
            return isXPageFolder(treeNode)
                    ? HippoIcon.fromSprite(id, Icon.XPAGE_FOLDER_OPEN)
                    : HippoIcon.fromSprite(id, Icon.FOLDER_OPEN);
        }
        return isXPageFolder(treeNode)
                ? HippoIcon.fromSprite(id, Icon.XPAGE_FOLDER)
                : HippoIcon.fromSprite(id, Icon.FOLDER);
    }

    private static boolean isXPageFolder(TreeNode treeNode){
        if (treeNode instanceof FolderTreeNode){
            FolderTreeNode folderTreeNode = ((FolderTreeNode) treeNode);
            final Node node = folderTreeNode.getNodeModel().getObject();
            try {
                return node.isNodeType(HippoStdNodeType.NT_XPAGE_FOLDER);
            } catch (RepositoryException exception) {
                log.warn("Could not determine node type for node: { path: {} }", JcrUtils.getNodePathQuietly(node));
            }
        }
        return false;
    }

}
