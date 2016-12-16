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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.DefinitionItem;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.impl.model.builder.exceptions.CircularDependencyException;
import org.onehippo.cm.impl.model.builder.exceptions.MissingDependencyException;

/**
 * Class that is capable of transforming a {@link Configuration} into its composite {@link ConfigurationNode} model
 * representation
 */
public class ConfigurationNodeBuilder {

    public ConfigurationNode build(final Configuration... configurations) {

        verifyDependencies(configurations);
        SortedSet<Configuration> sortedConfigurations = sort(configurations);

        // TODO get the sorted list of ALL DefinitionItem PER Configuration
        // FROM this list we can build the ConfigurationNode model

        return null;
    }

    void verifyDependencies(final Configuration... configurations) {
        Map<String, Configuration> configurationMap = new HashMap<>();
        for (Configuration configuration : configurations) {
            configurationMap.put(configuration.getName(), configuration);
        }
        for (Configuration configuration : configurations) {
            if (isEmptyDependsOn(configuration)) {
                continue;
            }
            Set<Configuration> checked = new HashSet<>();
            recurse(configurationMap, configuration, configuration, checked);
        }
    }

    private void recurse(final Map<String, Configuration> configurationMap,
                         final Configuration investigate,
                         final Configuration current,
                         final Set<Configuration> checked) {
        if (checked.contains(current)) {
            return;
        }
        checked.add(current);
        for (String dependsOn : current.getDependsOn()) {
            Configuration dependsOnConfig = configurationMap.get(dependsOn);
            if (dependsOnConfig == null) {
                throw new MissingDependencyException(String.format("Dependency '%s' has missing dependency '%s'",
                        current.getName(), dependsOn));
            }
            if (dependsOnConfig == investigate) {
                // TODO in the message add which configurations are part of the circular dependencies
                throw new CircularDependencyException(String.format("Configuration '%s' has circular dependency",
                        investigate.getName()));
            }
            if (isEmptyDependsOn(dependsOnConfig)) {
                continue;
            }
            recurse(configurationMap, investigate, dependsOnConfig, checked);
        }
    }

    SortedSet<Configuration> sort(final Configuration... configurations) {
        SortedSet<Configuration> sortedConfigurations = new TreeSet<>(new Comparator<Configuration>() {
            @Override
            public int compare(final Configuration config1, final Configuration config2) {
                if (config1.equals(config2)) {
                    return 0;
                }
                if (isEmptyDependsOn(config1)) {
                    if (isEmptyDependsOn(config2)) {
                        return config1.getName().compareTo(config2.getName());
                    } else {
                        return -1;
                    }
                }
                if (isEmptyDependsOn(config2)) {
                    return 1;
                }
                boolean config1DependsOnConfig2 = false;
                boolean config2DependsOnConfig1 = false;
                for (String dependsOn : config1.getDependsOn()) {
                    if (config2.getName().equals(dependsOn)) {
                        // config1 depends on config2 : Now exclude circular dependency
                        config1DependsOnConfig2 = true;
                    }
                }
                for (String dependsOn : config2.getDependsOn()) {
                    if (config1.getName().equals(dependsOn)) {
                        // config2 depends on config1
                        config2DependsOnConfig1 = true;
                    }
                }
                if (config2DependsOnConfig1) {
                    return -1;
                }
                if (config1DependsOnConfig2) {
                    return 1;
                }
                return config1.getName().compareTo(config2.getName());
            }


        });
        sortedConfigurations.addAll(Arrays.asList(configurations));
        return sortedConfigurations;
    }


    private void sort(final Map<String, Project> projects) {
        // sort the projects
        for (Project project : projects.values()) {

        }
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

    private boolean isEmptyDependsOn(final Configuration config) {
        return config.getDependsOn() == null || config.getDependsOn().size() == 0;
    }
}
