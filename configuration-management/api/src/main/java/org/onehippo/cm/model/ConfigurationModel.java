/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model;

import java.io.Closeable;
import java.util.List;
import java.util.Set;

import org.onehippo.cm.model.definition.ContentDefinition;
import org.onehippo.cm.model.definition.NamespaceDefinition;
import org.onehippo.cm.model.definition.WebFileBundleDefinition;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.tree.ConfigurationNode;
import org.onehippo.cm.model.tree.ConfigurationProperty;

/**
 * Represents a combined representation of configuration data from multiple groups, projects, modules, and sources.
 */
public interface ConfigurationModel extends Closeable {

    /**
     * @return a List of top-level configuration groups, pre-sorted in processing order
     */
    List<? extends Group> getSortedGroups();

    /**
     * @return a List of all namespace definitions found anywhere in the merged configuration
     */
    List<? extends NamespaceDefinition> getNamespaceDefinitions();

    /**
     * @return a List of all content definitions found anywhere in the merged configuration
     */
    List<? extends ContentDefinition> getContentDefinitions();

    /**
     * @return a List of all webfile bundle definitions found anywhere in the merged configuration
     */
    List<? extends WebFileBundleDefinition> getWebFileBundleDefinitions();

    /**
     * The set of all names of extensions present in this model. The "core" is always assumed to be present and
     * does not have an explicit representation. Thus, a core-only model will return an empty Set.
     * @return a Set of names for extensions present in this model; does not contain null
     * @since 2.0
     */
    Set<String> getExtensionNames();

    /**
     * @return the root node of the ConfigurationItem tree representing the merged state of nodes of category CONFIG
     */
    ConfigurationNode getConfigurationRootNode();

    /**
     * Find a ConfigurationNode by its absolute path.
     * @param path the path of a node
     * @return a ConfigurationNode or null, if no node exists with this path
     */
    ConfigurationNode resolveNode(JcrPath path);

    /**
     * Find a ConfigurationProperty by its absolute path.
     * @param path the path of a property
     * @return a ConfigurationProperty or null, if no property exists with this path
     */
    ConfigurationProperty resolveProperty(JcrPath path);

    /**
     * @param path a JCR path
     * @return the ContentDefinition defined in this model with the longest starting subpath match with path, or null if
     *      no content definition has a starting subpath match with its root path
     */
    ContentDefinition getNearestContentDefinition(JcrPath path);

    /**
     * Compile cryptographic digest of contents including all referenced Modules, Sources, and resource files.
     * A String.equals() comparison of this digest should be sufficient to detect changes in any config definitions,
     * actions, or the root definition paths for content definitions, at minimum.
     * @param extension the name of an extension whose digest is desired, or null for the core digest
     * @return a String containing a digest of model contents, in a format determined by the implementation
     * @since 2.0
     */
    String getDigest(final String extension);

    /**
     * When processing of this model is complete, this method must be closed to free up resources used by
     * ResourceInputProviders to access raw data streams from underlying storage.
     */
    void close();

}
