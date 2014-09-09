/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.cms.dev.updater

import org.onehippo.repository.update.BaseNodeUpdateVisitor
import javax.jcr.Node
import javax.jcr.NodeIterator

class ContentUpdater extends BaseNodeUpdateVisitor {

    private static final OLD_NODE_NAME = '{{oldNodeName}}'
    private static final NEW_NODE_NAME = '{{newNodeName}}'

    boolean doUpdate(Node node) {
        return update(node, OLD_NODE_NAME, NEW_NODE_NAME)
    }

    boolean undoUpdate(Node node) {
        return update(node, NEW_NODE_NAME, OLD_NODE_NAME)
    }

    boolean update(Node node, String from, String to) {
        NodeIterator it = node.getNodes(from);
        int nodes = 0;
        while (it.hasNext()) {
            Node child = it.nextNode();
            node.getSession().move(child.getPath(), node.getPath() + "/" + to);
            nodes++;
        }
        return nodes > 0;
    }
}