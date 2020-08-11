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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.jcr.RepositoryException;

class ModuleRegistry {

    private List<ModuleRegistration> registrations = new ArrayList<ModuleRegistration>();

    ModuleRegistry() {}

    ModuleRegistration registerModule(final String moduleName, final DaemonModule module) throws RepositoryException {
        final ModuleRegistration registration = new ModuleRegistration(moduleName, module);
        addRegistration(registration);
        return registration;
    }

    private void addRegistration(final ModuleRegistration registration) throws RepositoryException {
        final List<ModuleRegistration> updated = new ArrayList<ModuleRegistration>(registrations);
        updated.add(registration);
        checkDependencyGraph(updated, false);
        registrations = updated;
    }

    /*
     * Implementation of the Kahn's topological sort algorithm as described at http://en.wikipedia.org/wiki/Topological_sorting
     */
    private List<ModuleRegistration> sortModules(final List<ModuleRegistration> regs) {
        if (regs.size() <= 1) {
            return regs;
        }
        final List<ModuleRegistration> result = new ArrayList<ModuleRegistration>(regs.size());
        final Queue<ModuleRegistration> startNodes = new LinkedList<ModuleRegistration>();
        final List<Edge> edges = new LinkedList<Edge>();
        for (ModuleRegistration reg : regs) {
            if (reg.requirements().isEmpty() && reg.after().isEmpty()) {
                startNodes.add(reg);
            } else {
                for (ModuleRegistration other : regs) {
                    if (reg.after(other)) {
                        edges.add(new Edge(reg, other));
                    }
                }
            }
        }
        while (!startNodes.isEmpty()) {
            ModuleRegistration reg = startNodes.remove();
            result.add(reg);
            List<ModuleRegistration> deps = new ArrayList<ModuleRegistration>();
            for (Edge edge : edges) {
                if (edge.dependency.equals(reg)) {
                    deps.add(edge.dependent);
                }
            }
            for (ModuleRegistration dep : deps) {
                boolean hasMoreDeps = false;
                Iterator<Edge> iter = edges.iterator();
                while (iter.hasNext()) {
                    Edge edge = iter.next();
                    if (!edge.dependent.equals(dep)) {
                        continue;
                    }
                    if (edge.dependency.equals(reg)) {
                        iter.remove();
                        if (hasMoreDeps) {
                            break;
                        }
                    } else {
                        hasMoreDeps = true;
                    }
                }
                if (!hasMoreDeps) {
                    startNodes.add(dep);
                }
            }
        }
        // add registrations with unsatisfied requirements (because they are optional)
        for (ModuleRegistration reg : regs) {
            if (!result.contains(reg)) {
                result.add(reg);
            }
        }

        return result;
    }

    List<ModuleRegistration> getModuleRegistrations() {
        registrations = sortModules(registrations);
        return Collections.unmodifiableList(registrations);
    }

    List<ModuleRegistration> getModuleRegistrationsReverseOrder() {
        registrations = sortModules(registrations);
        final ArrayList<ModuleRegistration> moduleRegistrations = new ArrayList<ModuleRegistration>(registrations);
        Collections.reverse(moduleRegistrations);
        return Collections.unmodifiableList(moduleRegistrations);
    }

    void checkDependencyGraph(boolean failOnUnsatisfiedRequirement) throws RepositoryException {
        checkDependencyGraph(registrations, failOnUnsatisfiedRequirement);
    }

    private void checkDependencyGraph(List<ModuleRegistration> registrations, boolean failOnUnsatisfiedRequirement) throws RepositoryException {
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
                } else if (failOnUnsatisfiedRequirement && !registration.optional(requiredClass)) {
                    throw new RepositoryException("Unsatisfied dependency: " + registration.getModuleName() + " requires " + requiredClass);
                }
            }
            requirements.put(registration, requires);
        }
        for (ModuleRegistration registration : registrations) {
            detectCircularDependency(registration, requirements, new HashSet<>());
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
            detectCircularDependency(requirement, requirements, new HashSet<>(registrations));
        }
    }

    private static class Edge {
        private final ModuleRegistration dependent;
        private final ModuleRegistration dependency;
        private Edge(final ModuleRegistration dependent, final ModuleRegistration dependency) {
            this.dependent = dependent;
            this.dependency = dependency;
        }
    }
}
