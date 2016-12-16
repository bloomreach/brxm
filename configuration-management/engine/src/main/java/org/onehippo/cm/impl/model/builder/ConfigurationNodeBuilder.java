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

import java.util.Collection;
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
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Orderable;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.impl.model.builder.exceptions.CircularDependencyException;
import org.onehippo.cm.impl.model.builder.exceptions.MissingDependencyException;

/**
 * Class that is capable of transforming a {@link Configuration} into its composite {@link ConfigurationNode} model
 * representation
 */
public class ConfigurationNodeBuilder {

    public ConfigurationNode build(final Collection<Configuration> configurations) {

        verifyConfigurationDependencies(configurations);
        SortedSet<Orderable> sortedConfigurations = sort(configurations);

        // TODO get the sorted list of ALL DefinitionItem PER Configuration
        // FROM this list we can build the ConfigurationNode model

        return null;
    }

    void verifyConfigurationDependencies(final Collection<Configuration> configurations) {
        doVerifyDependencies(configurations);
        for (Configuration configuration : configurations) {
            if (configuration.getProjects() == null) {
                continue;
            }
            Collection<Project> projects = configuration.getProjects().values();
            verifyProjectDependencies(projects);
        }
    }

    void verifyProjectDependencies(final Collection<Project> projects) {
        doVerifyDependencies(projects);
        for (Project project : projects) {
            if (project.getModules() == null) {
                continue;
            }
            Collection<Module> modules = project.getModules().values();
            verifyModuleDependencies(modules);
        }
    }

    void verifyModuleDependencies(final Collection<Module> modules) {
        doVerifyDependencies(modules);
    }

    void doVerifyDependencies(final Collection<? extends Orderable> orderableList) {
        Map<String, Orderable> objectMap = new HashMap<>();
        for (Orderable orderable : orderableList) {
            objectMap.put(orderable.getName(), orderable);
        }
        for (Orderable orderable : orderableList) {
            if (isEmptyDependsOn(orderable)) {
                continue;
            }
            Set<Orderable> checked = new HashSet<>();
            recurse(objectMap, orderable, orderable, checked);
        }

    }

    private void recurse(final Map<String, Orderable> configurationMap,
                         final Orderable investigate,
                         final Orderable current,
                         final Set<Orderable> checked) {
        if (checked.contains(current)) {
            return;
        }
        checked.add(current);
        for (String dependsOn : current.getAfter()) {
            Orderable dependsOnOrderable = configurationMap.get(dependsOn);
            if (dependsOnOrderable == null) {
                throw new MissingDependencyException(String.format("Dependency '%s' has missing dependency '%s'",
                        current.getName(), dependsOn));
            }
            if (dependsOnOrderable == investigate) {
                // TODO in the message add which configurations are part of the circular dependencies
                throw new CircularDependencyException(String.format("'%s' '%s' has circular dependency",
                        dependsOnOrderable.getClass().getName(), investigate.getName()));
            }
            if (isEmptyDependsOn(dependsOnOrderable)) {
                continue;
            }
            recurse(configurationMap, investigate, dependsOnOrderable, checked);
        }
    }

    SortedSet<Orderable> sort(final Collection<? extends Orderable> list) {
        SortedSet<Orderable> sortedConfigurations = new TreeSet<>(new Comparator<Orderable>() {
            @Override
            public int compare(final Orderable orderable1, final Orderable orderable2) {
                if (orderable1.equals(orderable2)) {
                    return 0;
                }
                if (isEmptyDependsOn(orderable1)) {
                    if (isEmptyDependsOn(orderable2)) {
                        return orderable1.getName().compareTo(orderable2.getName());
                    } else {
                        return -1;
                    }
                }
                if (isEmptyDependsOn(orderable2)) {
                    return 1;
                }
                boolean config1DependsOnConfig2 = false;
                boolean config2DependsOnConfig1 = false;
                for (String dependsOn : orderable1.getAfter()) {
                    if (orderable2.getName().equals(dependsOn)) {
                        // orderable1 depends on orderable2 : Now exclude circular dependency
                        config1DependsOnConfig2 = true;
                    }
                }
                for (String dependsOn : orderable2.getAfter()) {
                    if (orderable1.getName().equals(dependsOn)) {
                        // orderable2 depends on orderable1
                        config2DependsOnConfig1 = true;
                    }
                }
                if (config2DependsOnConfig1) {
                    return -1;
                }
                if (config1DependsOnConfig2) {
                    return 1;
                }
                return orderable1.getName().compareTo(orderable2.getName());
            }


        });
        sortedConfigurations.addAll(list);
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

    private boolean isEmptyDependsOn(final Orderable orderable) {
        return orderable.getAfter() == null || orderable.getAfter().size() == 0;
    }
}
