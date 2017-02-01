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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Orderable;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.impl.model.builder.exceptions.CircularDependencyException;
import org.onehippo.cm.impl.model.builder.exceptions.MissingDependencyException;


public class DependencyVerifier<T extends Configuration> {

    public void verifyDependencies(final Map<String, ? extends Orderable> orderables) {
        for (Orderable orderable : orderables.values()) {
            if (orderable.getAfter().isEmpty()) {
                continue;
            }
            Set<Orderable> checked = new HashSet<>();
            recurse(orderables, orderable, orderable, checked);
        }
    }

    public void verifyConfigurationDependencies(final Collection<T> configurations) {
        doVerifyDependencies(configurations);
        for (Configuration configuration : configurations) {
            if (configuration.getProjects() == null) {
                continue;
            }
            Collection<Project> projects = configuration.getProjects().values();
            verifyProjectDependencies(projects);
        }
    }

    private void verifyProjectDependencies(final Collection<Project> projects) {
        doVerifyDependencies(projects);
        for (Project project : projects) {
            if (project.getModules() == null) {
                continue;
            }
            Collection<Module> modules = project.getModules().values();
            verifyModuleDependencies(modules);
        }
    }

    private void verifyModuleDependencies(final Collection<Module> modules) {
        doVerifyDependencies(modules);
    }

    private void doVerifyDependencies(final Collection<? extends Orderable> orderableList) {
        Map<String, Orderable> objectMap = new HashMap<>();
        for (Orderable orderable : orderableList) {
            objectMap.put(orderable.getName(), orderable);
        }
        for (Orderable orderable : orderableList) {
            if (orderable.getAfter().isEmpty()) {
                continue;
            }
            Set<Orderable> checked = new HashSet<>();
            recurse(objectMap, orderable, orderable, checked);
        }
    }

    private void recurse(final Map<String, ? extends Orderable> configurationMap,
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
            if (dependsOnOrderable.getAfter().isEmpty()) {
                continue;
            }
            recurse(configurationMap, investigate, dependsOnOrderable, checked);
        }
    }
}
