/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.tree;

import org.onehippo.cm.model.path.JcrPathSegment;

/**
 * Represents the (potential) state of a JCR Node as specified in a DefinitionItem tree.
 */
public interface DefinitionNode<N extends DefinitionNode, P extends DefinitionProperty> extends DefinitionItem<N>, ModelNode<N,P> {

    /**
     * @return The property string representing an ordering dependency constraint for this node, or null of no constraint
     * has been defined.
     */
    String getOrderBefore();

    /**
     * @return true iff this node definition indicates a deletion of a node defined in a module upon which the
     * module containing this node depends
     * TODO: resolve inconsistency between isDelete() here and DefinitionNodeImpl.isDeleted()
     */
    boolean isDelete();

    /**
     * @return A {@link ConfigurationItemCategory} that should be applied to any node not specified explicitly as a
     * child of this node, or null if unspecified
     */
    ConfigurationItemCategory getResidualChildNodeCategory();

}
