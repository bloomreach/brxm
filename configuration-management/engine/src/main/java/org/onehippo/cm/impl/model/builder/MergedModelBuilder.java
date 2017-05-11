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

import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.impl.MergedModelImpl;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ConfigurationNodeImpl;
import org.onehippo.cm.impl.model.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MergedModelBuilder accumulates {@link ConfigurationImpl}s into a map of configurations and, when building
 * the model, sorts the involved objects into processing order (based on "after" dependencies) to construct
 * the tree of {@link ConfigurationNodeImpl}s.
 */
public class MergedModelBuilder {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTreeBuilder.class);
    private static final OrderableListSorter<ConfigurationImpl> configurationSorter = new OrderableListSorter<>(Configuration.class.getSimpleName());

    private final List<ConfigurationImpl> configurations = new ArrayList<>();
    private final Map<String, ConfigurationImpl> configurationMap = new HashMap<>();
    private final Map<Module, ResourceInputProvider> resourceInputProviders = new HashMap<>();

    // Used for cleanup when done with this MergedModel
    private final Set<FileSystem> filesystems = new HashSet<>();

    public MergedModel build() {
        sort();

        final MergedModelImpl mergedModel = new MergedModelImpl();
        mergedModel.setSortedConfigurations(configurations);

        final ConfigurationTreeBuilder configurationTreeBuilder = new ConfigurationTreeBuilder();
        configurations.forEach(configuration ->
                configuration.getModifiableProjects().forEach(project ->
                        project.getModifiableModules().forEach(module -> {
                            logger.info("Merging module {}", ModelUtils.formatModule(module));
                            mergedModel.addNamespaceDefinitions(module.getNamespaceDefinitions());
                            mergedModel.addNodeTypeDefinitions(module.getNodeTypeDefinitions());
                            module.getConfigDefinitions().forEach(configurationTreeBuilder::push);
                            mergedModel.addContentDefinitions(module.getContentDefinitions().stream().map(x -> (ContentDefinition)x).collect(Collectors.toSet()));
                            mergedModel.addWebFileBundleDefinitions(module.getWebFileBundleDefinitions());
                        })
                )
        );
        mergedModel.setConfigurationRootNode(configurationTreeBuilder.build());
        mergedModel.addResourceInputProviders(resourceInputProviders);

        mergedModel.setFileSystems(filesystems);

        return mergedModel;
    }

    public MergedModelBuilder push(final Map<String, ConfigurationImpl> configurations, final Map<Module, ResourceInputProvider> resourceInputProviders) {
        for (Configuration config : configurations.values()) {
            // TODO: awful needed casting
            push((ConfigurationImpl)config);
        }
        pushResourceInputProviders(resourceInputProviders);
        return this;
    }

    public MergedModelBuilder push(final ConfigurationImpl configuration) {
        final String name = configuration.getName();
        final ConfigurationImpl consolidated = configurationMap.containsKey(name)
                ? configurationMap.get(name) : createConfiguration(name);

        consolidated.addAfter(configuration.getAfter());
        configuration.getModifiableProjects().forEach(consolidated::pushProject);
        return this;
    }

    public void pushResourceInputProviders(Map<Module, ResourceInputProvider> resourceInputProviders) {
        for (Module module : resourceInputProviders.keySet()) {
            if (this.resourceInputProviders.containsKey(module)) {
                final String msg = String.format(
                        "ResourceInputProviders for module '%s' already pushed before.",
                        ModelUtils.formatModule(module)
                );
                throw new IllegalArgumentException(msg);
            }
            this.resourceInputProviders.put(module, resourceInputProviders.get(module));
        }
    }

    private ConfigurationImpl createConfiguration(final String name) {
        final ConfigurationImpl configuration = new ConfigurationImpl(name);

        configurationMap.put(name, configuration);
        configurations.add(configuration);

        return configuration;
    }

    private void sort() {
        configurationSorter.sort(configurations);
        configurations.forEach(ConfigurationImpl::sortProjects);
    }

    /**
     * Track a FileSystem for later cleanup in MergedModel#close().
     * @param fs
     * @see MergedModel#close()
     */
    public void addFileSystem(FileSystem fs) {
        filesystems.add(fs);
    }
}
