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
package org.onehippo.cm.impl.model.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionItem;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.impl.model.builder.sorting.DefinitionSorter;
import org.onehippo.cm.impl.model.builder.sorting.Sorter;

/**
 * Class that is capable of transforming a {@link Configuration} into its composite {@link ConfigurationNode} model
 * representation
 */
public class ConfigurationNodeBuilder {

    public ConfigurationNode build(final Collection<Configuration> configurations) {

        new DependencyVerifier().verifyConfigurationDependencies(configurations);

        final DefinitionTriple orderedDefinitions = extractOrderedDefinitions(configurations);

        // TODO: accumulate the ordered content definitions into a configuration node tree

        return null;
    }

    protected DefinitionTriple extractOrderedDefinitions(final Collection<Configuration> configurations) {
        final DefinitionTriple orderedDefinitions =  new DefinitionTriple();
        new Sorter<Configuration>()
                .sort(configurations)
                .stream()
                .forEach(configuration -> addDefinitionsForConfiguration(configuration, orderedDefinitions));
        return orderedDefinitions;
    }

    private void addDefinitionsForConfiguration(final Configuration configuration,
                                                final DefinitionTriple orderedDefinitions) {
        new Sorter<Project>()
                .sort(configuration.getProjects())
                .stream()
                .forEach(project -> addDefinitionsForProject(project, orderedDefinitions));
    }

    private void addDefinitionsForProject(final Project project,
                                          final DefinitionTriple orderedDefinitions) {
        new Sorter<Module>()
                .sort(project.getModules())
                .stream()
                .forEach(module -> addDefinitionsForModule(module, orderedDefinitions));
    }

    private void addDefinitionsForModule(final Module module, final DefinitionTriple orderedDefinitions) {
        final List<Definition> definitions = new ArrayList<>();
        final SortedSet<Source> sortedSources = new TreeSet<>((s1, s2) -> s1.getPath().compareTo(s2.getPath()));

        sortedSources.addAll(module.getSources().values());
        sortedSources.stream()
                .map(Source::getDefinitions)
                .forEachOrdered(definitions::addAll);

        new DefinitionSorter(module).sort(definitions, orderedDefinitions);
    }

    /**
     *
     * @param configurationNode the existing {@link ConfigurationNode} to be augmented
     * @param definitionItem the {@link DefinitionItem} to augment
     * @return the augmented {@link ConfigurationNode} which is the same instance as {@code configurationNode}
     */
    public ConfigurationNode augment(final ConfigurationNode configurationNode, final DefinitionItem definitionItem) {
        return configurationNode;
    }

}
