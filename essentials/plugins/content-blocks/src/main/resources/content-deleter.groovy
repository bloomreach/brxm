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

class ContentDeleter extends BaseNodeUpdateVisitor {

    private static final NODE_NAME = '{{nodePath}}'

    boolean doUpdate(Node node) {
        NodeIterator it = node.getNodes(NODE_NAME);
        int nodes = 0;
        while (it.hasNext()) {
            Node child = it.nextNode();
            child.remove();
            nodes++;
        }
        return nodes > 0;
    }

    boolean undoUpdate(Node node) {
        throw new UnsupportedOperationException('Updater does not implement undoUpdate method');
    }
}