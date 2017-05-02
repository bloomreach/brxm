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
package org.onehippo.cm.api;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.WebFileBundleDefinition;

/**
 * Represents a combined representation of configuration from multiple groups, projects, modules, and sources.
 */
public interface MergedModel extends Closeable {

    /**
     * @return a List of top-level configuration groups, pre-sorted in processing order
     */
    List<Configuration> getSortedConfigurations();

    /**
     * @return a List of all namespace definitions found anywhere in the merged configuration
     */
    List<NamespaceDefinition> getNamespaceDefinitions();

    /**
     * @return a List of all node type definitions found anywhere in the merged configuration
     */
    List<NodeTypeDefinition> getNodeTypeDefinitions();

    /**
     * TODO: explain this
     * @return
     */
    ConfigurationNode getConfigurationRootNode();

    /**
     * @return a List of all webfile bundle definitions found anywhere in the merged configuration
     */
    List<WebFileBundleDefinition> getWebFileBundleDefinitions();

    /**
     * @return a Map of ResourceInputProviders by Module to provide access to raw source data streams
     */
    Map<Module, ResourceInputProvider> getResourceInputProviders();

    /**
     * When processing of this model is complete, this method must be closed to free up resources used by
     * ResourceInputProviders to access raw data streams from underlying storage.
     * @throws IOException
     */
    void close() throws IOException;
}
