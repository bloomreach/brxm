/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.contenttype;

import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * An immutable and aggregated or effective JCR Repository {@link javax.jcr.nodetype.NodeType} representation
 * or an aggregation thereof to represent a concrete {@link javax.jcr.Node} instance.
 *
 * Note: not all information available from the JCR NodeType model is captured.
 */
public interface EffectiveNodeType {

    /**
     * @return The immutable version of the EffectiveNodeTypes instance used to create this definition
     */
    long version();

    /**
     * @return True if this is an aggregation of multiple EffectiveNodeTypes like to represent a concrete {@link javax.jcr.Node} instance
     */
    boolean isAggregate();

    /**
     * @return the Qualified name of the JCR Repository NodeType (see JCR-2.0 3.2.5.2); or the list of aggregated NodeType names as [<name>,...] if {@link #isAggregate()}
     * @see javax.jcr.nodetype.NodeTypeDefinition#getName()
     */
    String getName();

    /**
     * @return The namespace prefix as used by this registered NodeType (derived from its name); null if {@link #isAggregate()}
     */
    String getPrefix();

    /**
     * @return The natural ordered set of names of all directly or inherited parent NodeTypes or mixins for this NodeType or Node.
     * Never null but may be empty.
     * @see javax.jcr.nodetype.NodeType#getSupertypes()
     */
    SortedSet<String> getSuperTypes();

    /**
     * @return The natural ordered set of aggregated EffectiveNodeTypes, at least containing {@link #getName()} even if not {@link #isAggregate()}
     */
    SortedSet<String> getAggregatedTypes();

    /**
     * @param nodeTypeName The name of a node type
     * @return True if the name of this node type or any of its {@link #getSuperTypes()}  is equal to nodeTypeName
     * @see javax.jcr.nodetype.NodeType#isNodeType(String)
     */
    boolean isNodeType(String nodeTypeName);

    /**
     * @return True if this EffectiveNodeType is marked abstract; false if {@link #isAggregate()}
     * @see javax.jcr.nodetype.NodeTypeDefinition#isAbstract()
     */
    boolean isAbstract();

    /**
     * @return True if this EffectiveNodeType represents a Mixin; false if {@link #isAggregate()}
     * @see javax.jcr.nodetype.NodeTypeDefinition#isMixin()
     */
    boolean isMixin();

    /**
     * @return True if for this aggregated or effective NodeType its child Nodes are ordered
     * @see javax.jcr.nodetype.NodeTypeDefinition#hasOrderableChildNodes()
     *
     */
    boolean isOrdered();

    /**
     * @return The name of the NodeTypeItem to be used as PrimaryItem when creating a Node.
     * May be null.
     * @see javax.jcr.nodetype.NodeTypeDefinition#getPrimaryItemName()
     */
    String getPrimaryItemName();

    /**
     * Returns the aggregated map of all allowable Child Node definitions.
     *
     * The map keys are the Qualified Child Node names, or "*" for the list of residual Child Node definitions.
     * For each child 'name', possible multiple children are ordered by the number of requiredPrimaryTypes and the names thereof.
     * @return The aggregated map of all allowable Child Node definitions.
     * @see javax.jcr.nodetype.NodeType#getChildNodeDefinitions()
     */
    SortedMap<String, List<EffectiveNodeTypeChild>> getChildren();

    /**
     * Returns the aggregated map of all allowable Property definitions.
     *
     * The map keys are the Qualified Property names, or "*" for the list of residual Property definitions.
     * For each property 'name', possible multiple properties are ordered by their type (String first) and their multiplicity (single first)
     * @return The aggregated map of all allowable Property definitions.
     * @see javax.jcr.nodetype.NodeType#getPropertyDefinitions()
     */
    SortedMap<String, List<EffectiveNodeTypeProperty>> getProperties();
}
