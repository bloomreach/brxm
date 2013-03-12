/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModuleRegistry {

    private List<ModuleRegistration> registrations = new ArrayList<ModuleRegistration>();

    ModuleRegistry() {}

    void registerModule(final String moduleName, final DaemonModule module) throws RepositoryException {
        addRegistration(new ModuleRegistration(moduleName, module));
    }

    void addRegistration(final ModuleRegistration registration) throws RepositoryException {
        final List<ModuleRegistration> updated = new ArrayList<ModuleRegistration>(registrations);
        updated.add(registration);
        checkDependencyGraph(updated);
        registrations = updated;
    }

    private void sortModules(final List<ModuleRegistration> registrations) {
        Collections.sort(registrations, new Comparator<ModuleRegistration>() {
            @Override
            public int compare(final ModuleRegistration o1, final ModuleRegistration o2) {
                return o1.compare(o2, registrations);
            }
        });
    }

    List<ModuleRegistration> getModuleRegistrations() {
        sortModules(registrations);
        return Collections.unmodifiableList(registrations);
    }

    List<ModuleRegistration> getModuleRegistrationsReverseOrder() {
        sortModules(registrations);
        final ArrayList<ModuleRegistration> moduleRegistrations = new ArrayList<ModuleRegistration>(registrations);
        Collections.reverse(moduleRegistrations);
        return Collections.unmodifiableList(moduleRegistrations);
    }

    void checkDependencyGraph(List<ModuleRegistration> registrations) throws RepositoryException {
        final Map<Class<?>, ModuleRegistration> provided = new HashMap<Class<?>, ModuleRegistration>();
        for (ModuleRegistration registration : registrations) {
            for (Class<?> aClass : registration.provides()) {
                provided.put(aClass, registration);
            }
        }
        final Map<ModuleRegistration, List<ModuleRegistration>> requirements = new HashMap<ModuleRegistration, List<ModuleRegistration>>();
        for (ModuleRegistration registration : registrations) {
            final List<ModuleRegistration> requires = new ArrayList<ModuleRegistration>();
            for (Class<?> requiredClass : registration.requirements()) {
                if (provided.containsKey(requiredClass)) {
                    requires.add(provided.get(requiredClass));
                }
            }
            requirements.put(registration, requires);
        }
        for (ModuleRegistration registration : registrations) {
            detectCircularDependency(registration, requirements, new HashSet<ModuleRegistration>());
        }
    }

    private void detectCircularDependency(final ModuleRegistration registration,
                                          final Map<ModuleRegistration, List<ModuleRegistration>> requirements,
                                          final Set<ModuleRegistration> registrations) throws RepositoryException {
        if (registrations.contains(registration)) {
            final StringBuilder sb = new StringBuilder();
            for (ModuleRegistration m : registrations) {
                sb.append(m.getModuleName() != null ? m.getModuleName() : m.getModuleClass());
                sb.append(", ");
            }
            sb.append(registration.getModuleName() != null ? registration.getModuleName() : registration.getModuleClass());
            throw new RepositoryException("Circular dependency detected among modules: " + sb);
        }
        registrations.add(registration);
        for (ModuleRegistration requirement : requirements.get(registration)) {
            detectCircularDependency(requirement, requirements, new HashSet<ModuleRegistration>(registrations));
        }
    }
}
