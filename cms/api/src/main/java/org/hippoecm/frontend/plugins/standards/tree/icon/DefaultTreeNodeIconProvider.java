/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.skin.Icon;

public class DefaultTreeNodeIconProvider extends AbstractJcrTreeNodeIconProvider {

    private static final long serialVersionUID = 1L;

    public HippoIcon getNodeIcon(final String id, final TreeNode treeNode, final ITreeState state) {
        if (state.isNodeExpanded(treeNode)) {
            return HippoIcon.fromSprite(id, Icon.FOLDER_OPEN);
        } else {
            return HippoIcon.fromSprite(id, Icon.FOLDER);
        }
    }

}
