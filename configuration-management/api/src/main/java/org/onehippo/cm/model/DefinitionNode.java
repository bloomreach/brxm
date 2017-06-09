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
package org.onehippo.cm.model;

import java.util.Map;

public interface DefinitionNode extends DefinitionItem, ModelNode {

    /**
     * @return The <strong>ordered</strong> map of {@link DefinitionNode}s by name for this {@link DefinitionNode} as an immutable map
     * and empty immutable map if none present. Note the ordering is according to serialized yaml format and not in
     * model processing order.
     */
    @Override
    Map<String, ? extends DefinitionNode> getNodes();

    /**
     * @param name the name of the child node
     * @return the child {@link DefinitionNode node} requested, or null if not configured
     */
    @Override
    DefinitionNode getNode(final String name);

    /**
     * @return The <strong>ordered</strong> map of {@link DefinitionProperty}s by name for this {@link DefinitionNode} as an immutable map
     * and empty immutable map if none present. Note the ordering is according to serialized yaml format and not in
     * model processing order.
     */
    @Override
    Map<String, ? extends DefinitionProperty> getProperties();

    /**
     * @param name the name of the property
     * @return the {@link DefinitionProperty} requested, or null if not configured
     */
    @Override
    DefinitionProperty getProperty(final String name);

    /**
     * @return Boolean.TRUE iff for this node the order of its children can be ignored on detecting changes,
     * even if its primary node type indicates otherwise, or null if unspecified.
     */
    @Override
    Boolean getIgnoreReorderedChildren();

    /**
     * @return true iff this node definition indicates a deletion of a node defined in a module upon which the
     * module containing this node depends
     * TODO: resolve inconsistency between isDelete() here and DefinitionNodeImpl.isDeleted(), then push up to ModelItem
     */
    boolean isDelete();

    /**
     * @return The property string representing an ordering dependency constraint for this node, or null of no constraint
     * has been defined.
     */
    String getOrderBefore();

    /**
     * @return The {@link ConfigurationItemCategory} for configured for this node, or null if unspecified.
     */
    ConfigurationItemCategory getResidualChildNodeCategory();

}
