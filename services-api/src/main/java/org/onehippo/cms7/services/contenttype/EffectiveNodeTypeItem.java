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

/**
 * Represents the common characteristics shared between the {@link EffectiveNodeTypeChild} and {@link EffectiveNodeTypeProperty} sub types.
 * @see javax.jcr.nodetype.ItemDefinition
 */
public interface EffectiveNodeTypeItem {

    /**
     * @return A qualified JCR Item definition name or "*" when this Item represents a residual definition
     * @see javax.jcr.nodetype.ItemDefinition#getName()
     */
    String getName();

    /**
     * @return The NodeType name which defines this item, which not necessarily is the same as the containing (aggregated or effective) NodeType.
     * @see javax.jcr.nodetype.ItemDefinition#getDeclaringNodeType()
     */
    String getDefiningType();

    /**
     * @return The type of this item, corresponding to a Property item requiredType or the list of required primary type names as [<name>,...] for a Child item
     */
    String getType();

    /**
     * @return True if this item represents a residual child or property definition (when name == "*")
     */
    boolean isResidual();

    /**
     * @return True for a multi-valued Property or same-name-sibling allowing Child definition
     * @see javax.jcr.nodetype.PropertyDefinition#isMultiple()
     * @see javax.jcr.nodetype.NodeDefinition#allowsSameNameSiblings()
     */
    boolean isMultiple();

    /**
     * @return True if this item is required
     * @see javax.jcr.nodetype.ItemDefinition#isMandatory()
     */
    boolean isMandatory();

    /**
     * @return True if this item is autoCreated
     * @see javax.jcr.nodetype.ItemDefinition#isAutoCreated()
     */
    boolean isAutoCreated();

    /**
     * @return True if this item is protected
     * @see javax.jcr.nodetype.ItemDefinition#isProtected()
     */
    boolean isProtected();
}
