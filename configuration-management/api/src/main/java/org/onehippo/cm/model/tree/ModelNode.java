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

import org.onehippo.cm.model.path.JcrPathSegment;

/**
 * Represents the (potential) state of a JCR Node as specified in either a ConfigurationItem or DefinitionItem tree.
 */
public interface ModelNode extends ModelItem {

    /**
     * @return An ordered Collection of child nodes of this model node. Note the ordering is according to encounter order
     *      in the serialized yaml format.
     */
    Collection<? extends ModelNode> getNodes();

    /**
     * @return An ordered Collection of properties of this model node. Note the ordering is according to encounter order in
     *      the serialized yaml format. These properties may include JCR properties or metadata properties (identifiable
     *      with the ".meta:" namespace prefix).
     */
    Collection<? extends ModelProperty> getProperties();

    /**
     * @param name the name of the child node
     * @return the child {@link ModelNode node} requested, or null if not configured
     */
    ModelNode getNode(JcrPathSegment name);

    /**
     * @param name the name of the property
     * @return the {@link ModelProperty} requested, or null if not configured
     */
    ModelProperty getProperty(JcrPathSegment name);

    /**
     * @param name the name of the property
     * @return the {@link ModelProperty} requested, or null if not configured
     */
    ModelProperty getProperty(String name);

    /**
     * @return Boolean.TRUE if and only if the order of child nodes of this node can be ignored on detecting changes,
     * even if its primary node type indicates otherwise, or null if unspecified.
     */
    Boolean getIgnoreReorderedChildren();

}
