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

import java.util.Set;

/**
 * Represents a Child NodeDefinition for its containing EffectiveNodeType
 * @see javax.jcr.nodetype.NodeDefinition
 */
public interface EffectiveNodeTypeChild extends EffectiveNodeTypeItem {

    /**
     * @return The Qualified name of the default NodeType that will be assigned to the child node if it is created without explicitly specified primary node type.
     * @see javax.jcr.nodetype.NodeDefinition#getDefaultPrimaryTypeName()
     */
    String getDefaultPrimaryType();

    /**
     * @return The natural ordered set of qualified names of the required primary node types. Never returns null or even an empty set (nt:base returned as a minimum)
     * @see javax.jcr.nodetype.NodeDefinition#getRequiredPrimaryTypeNames()
     */
    Set<String> getRequiredPrimaryTypes();
}
