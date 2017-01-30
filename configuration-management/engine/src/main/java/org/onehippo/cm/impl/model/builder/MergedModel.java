/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.impl.model.builder;

import java.util.LinkedList;
import java.util.List;

import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.impl.model.ConfigurationNodeImpl;

public class MergedModel {
    private final List<NamespaceDefinition> namespaceDefinitions = new LinkedList<>();
    private final List<NodeTypeDefinition> nodeTypeDefinitions = new LinkedList<>();
    private final ConfigurationNodeImpl configurationNode = new ConfigurationNodeImpl();

    public MergedModel() {
        configurationNode.setName("");
        configurationNode.setPath("/");
    }

    public void addNamespaceDefinition(final NamespaceDefinition definition) {
        namespaceDefinitions.add(definition);
    }

    public List<NamespaceDefinition> getNamespaceDefinitions() {
        return namespaceDefinitions;
    }

    public void addNodeTypeDefinition(final NodeTypeDefinition definition) {
        nodeTypeDefinitions.add(definition);
    }

    public List<NodeTypeDefinition> getNodeTypeDefinitions() {
        return nodeTypeDefinitions;
    }

    public ConfigurationNodeImpl getConfigurationNode() {
        return configurationNode;
    }
}
