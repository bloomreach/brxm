/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.swing.tree.TreeNode;

import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.skin.Icon;

public class DefaultTreeNodeIconProvider extends AbstractJcrTreeNodeIconProvider {

    private static final long serialVersionUID = 1L;

    public ResourceReference getNodeIcon(TreeNode treeNode, ITreeState state) {
        if (treeNode instanceof IJcrTreeNode) {
            if (isVirtual((IJcrTreeNode) treeNode)) {
                if (state.isNodeExpanded(treeNode)) {
                    // TODO: use specific icon for virtual folder
                    return new IconResourceReference(Icon.FOLDER_OPEN_TINY);
                } else {
                    // TODO: use specific icon for virtual folder
                    return new IconResourceReference(Icon.FOLDER_TINY);
                }
            } else {
                if (state.isNodeExpanded(treeNode)) {
                    return new IconResourceReference(Icon.FOLDER_OPEN_TINY);
                } else {
                    return new IconResourceReference(Icon.FOLDER_TINY);
                }
            }
        } else {
            return null;
        }
    }

}
