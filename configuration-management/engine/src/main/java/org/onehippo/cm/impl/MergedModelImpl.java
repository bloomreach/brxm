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

package org.onehippo.cm.impl;

import java.util.ArrayList;
import java.util.List;

import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.NamespaceDefinitionImpl;
import org.onehippo.cm.impl.model.NodeTypeDefinitionImpl;

public class MergedModelImpl implements MergedModel {

    private final List<NamespaceDefinition> namespaceDefinitions = new ArrayList<>();
    private final List<NodeTypeDefinition> nodeTypeDefinitions = new ArrayList<>();
    private ConfigurationNode configurationRootNode;
    private List<Configuration> sortedConfigurations;

    @Override
    public List<Configuration> getSortedConfigurations() {
        return sortedConfigurations;
    }

    @Override
    public List<NamespaceDefinition> getNamespaceDefinitions() {
        return namespaceDefinitions;
    }

    @Override
    public List<NodeTypeDefinition> getNodeTypeDefinitions() {
        return nodeTypeDefinitions;
    }

    @Override
    public ConfigurationNode getConfigurationRootNode() {
        return configurationRootNode;
    }

    public void setSortedConfigurations(final List<ConfigurationImpl> sortedConfigurations) {
        this.sortedConfigurations = new ArrayList<>(sortedConfigurations);
    }

    public void addNamespaceDefinitions(final List<NamespaceDefinitionImpl> definitions) {
        namespaceDefinitions.addAll(definitions);
    }

    public void addNodeTypeDefinitions(final List<NodeTypeDefinitionImpl> definitions) {
        nodeTypeDefinitions.addAll(definitions);
    }

    public void setConfigurationRootNode(final ConfigurationNode configurationRootNode) {
        this.configurationRootNode = configurationRootNode;
    }

}
