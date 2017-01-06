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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionItem;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.impl.model.builder.sorting.SortedConfiguration;
import org.onehippo.cm.impl.model.builder.sorting.Sorter;

/**
 * Class that is capable of transforming a {@link Configuration} into its composite {@link ConfigurationNode} model
 * representation
 */
public class ConfigurationNodeBuilder {

    public ConfigurationNode build(final Collection<Configuration> configurations) {

        new DependencyVerifier().verifyConfigurationDependencies(configurations);

        SortedSet<Configuration> sortedConfigurations = new Sorter<Configuration>().sort(configurations);
        List<Configuration> sorted = new ArrayList<>();
        for (Configuration configuration : sortedConfigurations) {
            sorted.add(new SortedConfiguration(configuration));
        }

        // ordered PER module first by namespace, then cnd, then per repository path (node/prop)
        List<Definition> orderedDefinitions = getOrderedDefinitions(sortedConfigurations);

        return null;
    }

    private List<Definition> getOrderedDefinitions(final SortedSet<Configuration> sortedConfigurations) {
        List<Definition> definitions = new ArrayList<>();

        for (Configuration sortedConfiguration : sortedConfigurations) {
            for (Project project : sortedConfiguration.getProjects().values()) {
                for (Module module : project.getModules().values()) {
                    List<Definition> orderedModuleDefinitions = getOrderedModuleDefinitions(module);
                    definitions.addAll(orderedModuleDefinitions);
                }
            }
        }

        return definitions;
    }

    private List<Definition> getOrderedModuleDefinitions(final Module module) {
        List<Definition> definitions = new ArrayList<>();
        for (Source source : module.getSources().values()) {
            definitions.addAll(source.getDefinitions());
        }

        Collections.sort(definitions, new Comparator<Definition>() {
            @Override
            public int compare(final Definition def1, final Definition def2) {
                int nsComparison = compareNameSpaceDefs(def1, def2);
                if (nsComparison != 0) {
                    return nsComparison;
                }

                int ntComparison = compareNodeTypeDefs(def1, def2);
                if (ntComparison != 0) {
                    return ntComparison;
                }

                // TODO
                // return compareContentDefs(def1, def2);
                return 0;
            }

            private int compareNameSpaceDefs(final Definition def1, final Definition def2) {
                if (def1 instanceof NamespaceDefinition) {
                    if (def2 instanceof NamespaceDefinition) {
                        return ((NamespaceDefinition)def1).getPrefix().compareTo(((NamespaceDefinition)def2).getPrefix());
                    }
                    // def 1 should be first
                    return -1;
                }
                if (def2 instanceof NamespaceDefinition) {
                    return 1;
                }
                return 0;
            }


            private int compareNodeTypeDefs(final Definition def1, final Definition def2) {
                if (def1 instanceof NodeTypeDefinition) {
                    if (def2 instanceof NodeTypeDefinition) {
                        // TODO both def1 and def2 are of type NodeTypeDefinition : It might be that cnd of def2
                        // TODO depends on the cnd of def1 : Hence we need to parse the actual cnd value to find out
                        // TODO whether def1 or def2 should be loaded first. For now. just compare the cnd string
                        return ((NodeTypeDefinition)def1).getCndString().compareTo(((NodeTypeDefinition)def2).getCndString());
                    }
                    // def1 should be first
                    return -1;
                }
                if (def2 instanceof NamespaceDefinition) {
                    return 1;
                }
                return 0;
            }

        });

        return definitions;
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
