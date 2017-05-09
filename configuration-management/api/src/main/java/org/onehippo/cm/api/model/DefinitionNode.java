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
package org.onehippo.cm.api.model;

import java.util.Map;
import java.util.Optional;

public interface DefinitionNode extends DefinitionItem {
    /**
     * @return The <strong>ordered</strong> map of {@link DefinitionNode}s by name for this {@link DefinitionNode} as an immutable map
     * and empty immutable map if none present. Note the ordering is according to serialized yaml format and not in
     * model processing order.
     */
    Map<String, DefinitionNode> getNodes();
    /**
     * @return The <strong>ordered</strong> map of {@link DefinitionProperty}s by name for this {@link DefinitionNode} as an immutable map
     * and empty immutable map if none present. Note the ordering is according to serialized yaml format and not in
     * model processing order.
     */
    Map<String, DefinitionProperty> getProperties();

    boolean isDelete();

    /**
     * @return The property string representing an ordering dependency constraint for this node, or null of no constraint
     * has been defined.
     */
    String getOrderBefore();

    /**
     * @return Boolean.TRUE if for this node the order of its children can be ignored on detecting changes,
     * even if its primary node type indicates otherwise. Returns null if unspecified.
     */
    Boolean getIgnoreReorderedChildren();
}
