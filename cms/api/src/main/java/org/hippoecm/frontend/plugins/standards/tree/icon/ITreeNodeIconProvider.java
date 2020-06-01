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

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.util.io.IClusterable;

/**
 * Service that provides icons for nodes in the CMS folder tree.
 */
public interface ITreeNodeIconProvider extends IClusterable {

    /**
     * @return a component that represents a tree node.
     * Implementations should return null when no representation can be
     * found. Clients should have a fall-back mechanism, e.g. by
     * traversing a list of services until a component is found.
     */
    Component getNodeIcon(String id, TreeNode treeNode, ITreeState state);

}
