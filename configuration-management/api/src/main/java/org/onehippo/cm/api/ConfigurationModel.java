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

import org.onehippo.cm.api.model.Group;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.WebFileBundleDefinition;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents a combined representation of configuration from multiple groups, projects, modules, and sources.
 */
public interface ConfigurationModel extends Closeable {

    /**
     * @return a List of top-level configuration groups, pre-sorted in processing order
     */
    List<Group> getSortedGroups();

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

    List<ContentDefinition> getContentDefinitions();

    void addContentDefinitions(Collection<ContentDefinition> definitions);

    void addContentDefinition(ContentDefinition definition);

    /**
     * @return a List of all webfile bundle definitions found anywhere in the merged configuration
     */
    List<WebFileBundleDefinition> getWebFileBundleDefinitions();

    /**
     * @return a Map of ResourceInputProviders by Module to provide access to raw source data streams
     * TODO these use the config folder as base, not the module root! there's no way to access content source streams!
     */
    Map<Module, ResourceInputProvider> getResourceInputProviders();

    /**
     * Compile a manifest of contents including all referenced Modules, Sources, and resource files.
     * A cryptographic digest of this manifest should be sufficient to detect changes in any config definitions,
     * actions, or the root definition paths for content definitions, at minimum.
     * @return a String containing a manifest of model contents, in a format determined by the implementation
     */
    String compileManifest();

    /**
     * When processing of this model is complete, this method must be closed to free up resources used by
     * ResourceInputProviders to access raw data streams from underlying storage.
     * @throws IOException
     */
    void close() throws IOException;
}
