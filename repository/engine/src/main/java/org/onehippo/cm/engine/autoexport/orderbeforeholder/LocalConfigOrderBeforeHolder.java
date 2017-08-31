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

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;

public class LocalConfigOrderBeforeHolder extends ConfigOrderBeforeHolder {

    private final Consumer<DefinitionNodeImpl> emptyNodeRemover;
    private final String originalOrderBefore;

    public LocalConfigOrderBeforeHolder(final int moduleIndex,
                                 final DefinitionNodeImpl definitionNode,
                                 final Consumer<DefinitionNodeImpl> emptyNodeRemover) {
        super(moduleIndex, definitionNode);

        this.emptyNodeRemover = emptyNodeRemover;
        this.originalOrderBefore = definitionNode.getOrderBefore();

        definitionNode.setOrderBefore(null);
    }

    @Override
    void setOrderBefore(final String orderBefore) {
        getDefinitionNode().setOrderBefore(orderBefore);
    }

    @Override
    public void finish() {
        if (!StringUtils.equals(getDefinitionNode().getOrderBefore(), originalOrderBefore)) {
            getDefinitionNode().getDefinition().getSource().markChanged();
        }
        if (getDefinitionNode().isEmpty()) {
            emptyNodeRemover.accept(getDefinitionNode());
        }
    }
}
