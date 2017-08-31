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

import com.google.common.collect.ImmutableList;

import org.onehippo.cm.model.impl.path.JcrPathSegment;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpstreamConfigOrderBeforeHolder extends ConfigOrderBeforeHolder {

    private static final Logger log = LoggerFactory.getLogger(UpstreamConfigOrderBeforeHolder.class);

    public UpstreamConfigOrderBeforeHolder(final int moduleIndex, final DefinitionNodeImpl definitionNode) {
        super(moduleIndex, definitionNode);
    }

    @Override
    public void apply(final ImmutableList<JcrPathSegment> expected, final List<JcrPathSegment> intermediate) {
        // todo: add logic for delayed ordering mechanism
        final DefinitionNodeImpl definitionNode = getDefinitionNode();
        if (intermediate.size() == 0 || "".equals(definitionNode.getOrderBefore())) {
            intermediate.add(0, definitionNode.getJcrName());
            return;
        }
        if (definitionNode.getOrderBefore() == null) {
            intermediate.add(definitionNode.getJcrName());
            return;
        }
        final int position = intermediate.indexOf(JcrPathSegment.get(definitionNode.getOrderBefore()));
        if (position == -1) {
            // if the target cannot be found, we are in a weird situation as the model should not have loaded in
            // the first place, log an error but continue
            log.error("Cannot find order-before target '{}' for node '{}' from '{}', ordering node as last",
                    definitionNode.getOrderBefore(), definitionNode.getPath(), definitionNode.getSourceLocation());
            intermediate.add(definitionNode.getJcrName());
        } else {
            intermediate.add(position, definitionNode.getJcrName());
        }
    }

    @Override
    void setOrderBefore(final String orderBefore) {
        log.error("Unexpected call to setOrderBefore with value '{}' for node '{}' from '{}'", orderBefore,
                getDefinitionNode().getPath(), getDefinitionNode().getSourceLocation());
    }

    @Override
    public void finish() {
        // intentionally empty
    }
}
