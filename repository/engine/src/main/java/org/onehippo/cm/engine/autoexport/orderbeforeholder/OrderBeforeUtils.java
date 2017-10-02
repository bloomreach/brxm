/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.engine.autoexport.orderbeforeholder;

import java.util.List;

import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.model.Constants.META_ORDER_BEFORE_FIRST;

public class OrderBeforeUtils {

    private static final Logger log = LoggerFactory.getLogger(OrderBeforeUtils.class);

    /**
     * Insert a node into the a current list of node names using the node's orderBefore setting, reinsert the node if
     * it already exists. In case the target orderBefore is not found, an error is logged.
     */
    public static void insert(final DefinitionNodeImpl definitionNode, final List<JcrPathSegment> intermediate) {
        intermediate.remove(definitionNode.getJcrName());

        if (definitionNode.getOrderBefore() == null) {
            intermediate.add(definitionNode.getJcrName());
            return;
        }

        if (META_ORDER_BEFORE_FIRST.equals(definitionNode.getOrderBefore())) {
            intermediate.add(0, definitionNode.getJcrName());
            return;
        }

        final int position = intermediate.indexOf(JcrPaths.getSegment(definitionNode.getOrderBefore()));
        if (position == -1) {
            // todo: add logic for delayed ordering mechanism for config
            log.error("Cannot find order-before target '{}' for node '{}' from '{}', ordering node as last",
                    definitionNode.getOrderBefore(), definitionNode.getPath(), definitionNode.getSourceLocation());
            intermediate.add(definitionNode.getJcrName());
        } else {
            intermediate.add(position, definitionNode.getJcrName());
        }
    }
}
