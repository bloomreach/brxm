/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collection;
import java.util.List;

import org.onehippo.cm.model.path.JcrPathSegment;

/**
 * Represents a node of category CONFIG in the ConfigurationItem tree
 */
public interface ConfigurationNode extends ConfigurationItem, ModelNode {

    @Override
    List<? extends DefinitionNode> getDefinitions();

    @Override
    Collection<? extends ConfigurationNode> getNodes();

    @Override
    ConfigurationNode getNode(JcrPathSegment name);

    @Override
    Collection<? extends ConfigurationProperty> getProperties();

    @Override
    ConfigurationProperty getProperty(JcrPathSegment name);

    @Override
    ConfigurationProperty getProperty(String name);

    /**
     * Get the {@link ConfigurationItemCategory} of a child node by name.
     * @param nodeName A child node name as a {@link JcrPathSegment}
     * @return The {@link ConfigurationItemCategory} of the child node, never returns null.
     */
    ConfigurationItemCategory getChildNodeCategory(JcrPathSegment nodeName);

    /**
     * Get the {@link ConfigurationItemCategory} of a child node by name. Node names must be indexed, e.g.
     * <code>node[1]</code>.
     * @param nodeName A child node name; node names must be indexed, e.g. <code>node[1]</code>.
     * @param residualNodeCategoryOverride Override for this node's residual node category, or null for no override.
     * @return The {@link ConfigurationItemCategory} of the child node, never returns null.
     */
    ConfigurationItemCategory getChildNodeCategory(JcrPathSegment nodeName,
                                                   ConfigurationItemCategory residualNodeCategoryOverride);

    /**
     * Get the {@link ConfigurationItemCategory} of a child property by name.
     * @param propertyName A child property name.
     * @return The {@link ConfigurationItemCategory} of the child property, never returns null.
     */
    ConfigurationItemCategory getChildPropertyCategory(JcrPathSegment propertyName);

    /**
     * Get the {@link ConfigurationItemCategory} of a child property by name.
     * @param propertyName A child property name.
     * @return The {@link ConfigurationItemCategory} of the child property, never returns null.
     */
    ConfigurationItemCategory getChildPropertyCategory(String propertyName);

}
