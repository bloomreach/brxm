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

import java.util.ArrayList;
import java.util.List;

import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ConfigurationNodeImpl;
import org.onehippo.cm.impl.model.NamespaceDefinitionImpl;
import org.onehippo.cm.impl.model.NodeTypeDefinitionImpl;

public class MergedModel {
    private final List<NamespaceDefinitionImpl> namespaceDefinitions = new ArrayList<>();
    private final List<NodeTypeDefinitionImpl> nodeTypeDefinitions = new ArrayList<>();
    private ConfigurationNodeImpl configurationRootNode;
    private List<ConfigurationImpl> sortedConfigurations;

    public List<ConfigurationImpl> getSortedConfigurations() {
        return sortedConfigurations;
    }

    void setSortedConfigurations(final List<ConfigurationImpl> sortedConfigurations) {
        this.sortedConfigurations = sortedConfigurations;
    }

    public void addNamespaceDefinitions(final List<NamespaceDefinitionImpl> definitions) {
        namespaceDefinitions.addAll(definitions);
    }

    public List<NamespaceDefinitionImpl> getNamespaceDefinitions() {
        return namespaceDefinitions;
    }

    public void addNodeTypeDefinitions(final List<NodeTypeDefinitionImpl> definitions) {
        nodeTypeDefinitions.addAll(definitions);
    }

    public List<NodeTypeDefinitionImpl> getNodeTypeDefinitions() {
        return nodeTypeDefinitions;
    }

    public ConfigurationNodeImpl getConfigurationRootNode() {
        return configurationRootNode;
    }

    public void setConfigurationRootNode(final ConfigurationNodeImpl configurationRootNode) {
        this.configurationRootNode = configurationRootNode;
    }
}
