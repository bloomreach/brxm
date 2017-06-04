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

public interface ConfigurationNode extends ConfigurationItem {

    /**
     * @return The <strong>ordered</strong> map of child {@link ConfigurationNode}s by name for this
     * {@link ConfigurationNode} as an immutable map and empty immutable map if none present.
     * Nodes names are always indexed names, e.g. <code>node[1]</code>.
     */
    Map<String, ? extends ConfigurationNode> getNodes();

    /**
     * @param name the indexed name of the child node
     * @return the child {@link ConfigurationNode node} requested, or null if not configured
     */
    ConfigurationNode getNode(final String name);

    /**
     * @return The <strong>ordered</strong> map of {@link ConfigurationProperty}s by name for this
     * {@link ConfigurationNode} as an immutable map and empty immutable map if none present.
     */
    Map<String, ? extends ConfigurationProperty> getProperties();

    /**
     * @param name the name of the property
     * @return the {@link ConfigurationProperty} requested, or null if not configured
     */
    ConfigurationProperty getProperty(final String name);

    /**
     * @return Boolean.TRUE if for this node the order of its children can be ignored on detecting changes,
     * even if its primary node type indicates otherwise. Returns null if unspecified.
     */
    Boolean getIgnoreReorderedChildren();

    /**
     * Get the {@link ConfigurationItemCategory} of a child node by name. Node names must be indexed, e.g.
     * <code>node[1]</code>.
     * @param nodeName A child node name; node names must be indexed, e.g. <code>node[1]</code>.
     * @return The {@link ConfigurationItemCategory} of the child node, never returns null.
     */
    ConfigurationItemCategory getChildNodeCategory(final String nodeName);

    /**
     * Get the {@link ConfigurationItemCategory} of a child property by name.
     * @param propertyName A child property name.
     * @return The {@link ConfigurationItemCategory} of the child property, never returns null.
     */
    ConfigurationItemCategory getChildPropertyCategory(final String propertyName);

}
