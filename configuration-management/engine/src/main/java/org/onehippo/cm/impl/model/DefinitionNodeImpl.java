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
package org.onehippo.cm.impl.model;

import java.util.Collections;
import java.util.Map;

import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

public class DefinitionNodeImpl extends DefinitionItemImpl implements DefinitionNode {

    private Map<String, DefinitionNode> nodes;
    private Map<String, DefinitionProperty> properties;

    @Override
    public Map<String, DefinitionNode> getNodes() {
        if (nodes == null) {
            return emptyMap();
        }
        return unmodifiableMap(nodes);
    }

    public void setNodes(final Map<String, DefinitionNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public Map<String, DefinitionProperty> getProperties() {
        if (properties == null) {
            return emptyMap();
        }
        return unmodifiableMap(properties);
    }

    public void setProperties(final Map<String, DefinitionProperty> properties) {
        this.properties = properties;
    }
}
