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

import java.util.LinkedHashMap;
import java.util.Map;

import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Value;

import static java.util.Collections.unmodifiableMap;

public class DefinitionNodeImpl extends DefinitionItemImpl implements DefinitionNode {

    private Map<String, DefinitionNode> nodes = new LinkedHashMap<>();
    private Map<String, DefinitionProperty> properties = new LinkedHashMap<>();

    public DefinitionNodeImpl(final String path, final String name, final Definition definition) {
        super(path, name, definition);
    }

    public DefinitionNodeImpl(final String name, final DefinitionNodeImpl parent) {
        super(name, parent);
    }

    @Override
    public Map<String, DefinitionNode> getNodes() {
        return unmodifiableMap(nodes);
    }

    @Override
    public Map<String, DefinitionProperty> getProperties() {
        return unmodifiableMap(properties);
    }

    public DefinitionNodeImpl addNode(final String name) {
        final DefinitionNodeImpl node = new DefinitionNodeImpl(name, this);
        nodes.put(name, node);
        return node;
    }

    public DefinitionPropertyImpl addProperty(final String name, final Value value) {
        final DefinitionPropertyImpl property = new DefinitionPropertyImpl(name, value, this);
        properties.put(name, property);
        return property;
    }

    public DefinitionPropertyImpl addProperty(final String name, final Value[] values) {
        final DefinitionPropertyImpl property = new DefinitionPropertyImpl(name, values, this);
        properties.put(name, property);
        return property;
    }

}
