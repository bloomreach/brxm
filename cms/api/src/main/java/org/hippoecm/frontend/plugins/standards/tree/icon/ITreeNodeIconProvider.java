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

import org.apache.wicket.IClusterable;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.tree.ITreeState;

/**
 * Service that provides icons for nodes in the CMS folder tree.
 */
public interface ITreeNodeIconProvider extends IClusterable {

    /**
     * Retrieve a 16px icon to represent the tree node.
     * Implementations should return null when no representation can be
     * found.  Clients should have a fall-back mechanism, e.g. by
     * traversing a list of services until an icon is found.
     */
    ResourceReference getNodeIcon(TreeNode treeNode, ITreeState state);

}
