/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.console.tree;

import javax.jcr.Node;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.console.browser.NodeIconUtils;

public class StyledTreeNode extends Folder<Node> {
    
    public StyledTreeNode(final String id, final AbstractTree<Node> tree, final IModel<Node> model) {
        super(id, tree, model);
        final MarkupContainer link = (MarkupContainer) get("link");
        link.add(NodeIconUtils.createJcrNodeIcon("icon", model));
    }
}