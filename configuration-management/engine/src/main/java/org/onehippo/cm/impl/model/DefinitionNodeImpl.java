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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.ValueType;

public class DefinitionNodeImpl extends DefinitionItemImpl implements DefinitionNode {

    private final LinkedHashMap<String, DefinitionNodeImpl> modifiableNodes = new LinkedHashMap<>();
    private final Map<String, DefinitionNode> nodes = Collections.unmodifiableMap(modifiableNodes);
    private final Map<String, DefinitionPropertyImpl> modifiableProperties = new LinkedHashMap<>();
    private final Map<String, DefinitionProperty> properties = Collections.unmodifiableMap(modifiableProperties);
    private boolean delete = false;
    private String orderBefore = null;

    public DefinitionNodeImpl(final String path, final String name, final Definition definition) {
        super(path, name, definition);
    }

    public DefinitionNodeImpl(final String name, final DefinitionNodeImpl parent) {
        super(name, parent);
    }

    @Override
    public Map<String, DefinitionNode> getNodes() {
        return nodes;
    }

    public LinkedHashMap<String, DefinitionNodeImpl> getModifiableNodes() {
        return modifiableNodes;
    }

    @Override
    public Map<String, DefinitionProperty> getProperties() {
        return properties;
    }

    public Map<String, DefinitionPropertyImpl> getModifiableProperties() {
        return modifiableProperties;
    }

    @Override
    public boolean isDelete() {
        return delete;
    }

    public void setDelete(final boolean delete) {
        this.delete = delete;
    }

    @Override
    public Optional<String> getOrderBefore() {
        return Optional.ofNullable(orderBefore);
    }

    public void setOrderBefore(final String orderBefore) {
        this.orderBefore = orderBefore;
    }

    public DefinitionNodeImpl addNode(final String name) {
        final DefinitionNodeImpl node = new DefinitionNodeImpl(name, this);
        modifiableNodes.put(name, node);
        return node;
    }

    public DefinitionPropertyImpl addProperty(final String name, final ValueImpl value) {
        final DefinitionPropertyImpl property = new DefinitionPropertyImpl(name, value, this);
        modifiableProperties.put(name, property);
        return property;
    }

    public DefinitionPropertyImpl addProperty(final String name, final ValueType type, final ValueImpl[] values) {
        final DefinitionPropertyImpl property = new DefinitionPropertyImpl(name, type, values, this);
        modifiableProperties.put(name, property);
        return property;
    }

    public void delete() {
        delete = true;
        modifiableNodes.clear();
        modifiableProperties.clear();
        orderBefore = null;
    }

    public boolean isEmpty() {
        return modifiableNodes.isEmpty() && modifiableProperties.isEmpty() && orderBefore == null;
    }

    public boolean isDeleted() {
        return isDelete() && isEmpty();
    }
}
