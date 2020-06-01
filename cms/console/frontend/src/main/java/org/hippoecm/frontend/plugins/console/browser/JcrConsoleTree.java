/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.browser;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.hippoecm.frontend.widgets.JcrTree;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

public abstract class JcrConsoleTree extends JcrTree {

    public JcrConsoleTree(String id, TreeModel treeModel) {
        super(id, treeModel);
    }

    @Override
    protected Component newNodeIcon(final MarkupContainer parent, final String id, final TreeNode node) {
        return NodeIconUtils.createJcrNodeIcon(id, node);
    }
}
