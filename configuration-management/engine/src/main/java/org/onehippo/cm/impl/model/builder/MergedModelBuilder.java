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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ConfigurationNodeImpl;
import org.onehippo.cm.impl.model.builder.sorting.OrderableComparator;

/**
 * ConfigurationBuilder accumulates {@link ConfigurationImpl}s into a map of configurations and, when building
 * the model, sorts the involved objects into processing order (based on "after" dependencies) to construct
 * the tree of {@link ConfigurationNodeImpl}s.
 */
public class MergedModelBuilder {
    private final List<ConfigurationImpl> configurations = new ArrayList<>();
    private final Map<String, ConfigurationImpl> configurationMap = new HashMap<>();

    // Add DI?
    private DependencyVerifier dependencyVerifier = new DependencyVerifier();

    public MergedModel build() {
        verifyDependencies();
        sort();

        final MergedModel mergedModel = new MergedModel();
        mergedModel.setSortedConfigurations(configurations);

        final ConfigurationTreeBuilder configurationTreeBuilder = new ConfigurationTreeBuilder();
        configurations.forEach(configuration ->
                configuration.getModifiableProjects().forEach(project ->
                        project.getModifiableModules().forEach(module -> {
                            mergedModel.addNamespaceDefinitions(module.getNamespaceDefinitions());
                            mergedModel.addNodeTypeDefinitions(module.getNodeTypeDefinitions());
                            module.getContentDefinitions().forEach(configurationTreeBuilder::push);
                        })
                )
        );
        mergedModel.setConfigurationRootNode(configurationTreeBuilder.build());

        return mergedModel;
    }

    public MergedModelBuilder push(final ConfigurationImpl configuration) {
        final String name = configuration.getName();
        final ConfigurationImpl consolidated = configurationMap.containsKey(name)
                ? configurationMap.get(name) : createConfiguration(name);

        consolidated.addAfter(configuration.getAfter());
        configuration.getModifiableProjects().forEach(consolidated::pushProject);
        return this;
    }

    private ConfigurationImpl createConfiguration(final String name) {
        final ConfigurationImpl configuration = new ConfigurationImpl(name);

        configurationMap.put(name, configuration);
        configurations.add(configuration);

        return configuration;
    }

    private void verifyDependencies() {
        dependencyVerifier.verify(configurations);
        configurations.stream()
                .map(Configuration::getProjects)
                .forEach(projects -> {
                    dependencyVerifier.verify(projects);
                    projects.stream()
                            .map(Project::getModules)
                            .forEach(dependencyVerifier::verify);
                });
    }

    private void sort() {
        configurations.sort(new OrderableComparator<>());
        configurations.forEach(ConfigurationImpl::sortProjects);
    }
}
