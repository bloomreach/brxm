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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModuleRegistry.class);

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

    /*
     * Implementation of the Kahn's topological sort algorithm as described at http://en.wikipedia.org/wiki/Topological_sorting
     */
    private List<ModuleRegistration> sortModules(final List<ModuleRegistration> regs) {
        final List<ModuleRegistration> result = new ArrayList<ModuleRegistration>(regs.size());
        final Queue<ModuleRegistration> startNodes = new LinkedList<ModuleRegistration>();
        final List<ModuleRegistration[]> edges = new LinkedList<ModuleRegistration[]>();
        for (ModuleRegistration reg : regs) {
            if (reg.requirements().isEmpty()) {
                startNodes.add(reg);
            } else {
                for (ModuleRegistration other : regs) {
                    if (reg.requires(other)) {
                        edges.add(new ModuleRegistration[] {reg, other});
                    }
                }
            }
        }
        while (!startNodes.isEmpty()) {
            ModuleRegistration reg = startNodes.remove();
            result.add(reg);
            List<ModuleRegistration> deps = new ArrayList<ModuleRegistration>();
            for (ModuleRegistration[] edge : edges) {
                if (edge[1].equals(reg)) {
                    deps.add(edge[0]);
                }
            }
            for (ModuleRegistration dep : deps) {
                boolean hasMoreDeps = false;
                Iterator<ModuleRegistration[]> iter = edges.iterator();
                while (iter.hasNext()) {
                    ModuleRegistration[] edge = iter.next();
                    if (!edge[0].equals(dep)) {
                        continue;
                    }
                    if (edge[1].equals(reg)) {
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
        if (!edges.isEmpty()) {
            StringBuilder buf = new StringBuilder();
            for (ModuleRegistration[] edge : edges) {
                buf.append(edge[0].getModuleName() != null ? edge[0].getModuleName() : edge[0].getModuleClass());
                buf.append(" requires ");
                buf.append(edge[1].getModuleName() != null ? edge[1].getModuleName() : edge[1].getModuleClass());
                buf.append('\n');
            }
            log.error("Circular dependency detected among modules: {}", buf);
        }
        return result;
    }

    /*
    private List<ModuleRegistration> sortModules(final List<ModuleRegistration> registrations) {
        // collect the lists of dependencies of all registrations
        final Map<ModuleRegistration, List<ModuleRegistration>> dependencies =
                new HashMap<ModuleRegistration, List<ModuleRegistration>>();
        for (ModuleRegistration registration : registrations) {
            final List<ModuleRegistration> deps = new ArrayList<ModuleRegistration>();
            for (ModuleRegistration dependency : registrations) {
                if (registration.requires(dependency)) {
                    deps.add(dependency);
                }
            }
            if (!deps.isEmpty()) {
                dependencies.put(registration, deps);
            }
        }
        // sort all the individual lists of dependencies internally
        final List<List<ModuleRegistration>> alldeps = new ArrayList<List<ModuleRegistration>>();
        for (Map.Entry<ModuleRegistration, List<ModuleRegistration>> entry : dependencies.entrySet()) {
            final List<ModuleRegistration> deps = entry.getValue();
            Collections.sort(deps, new Comparator<ModuleRegistration>() {
                @Override
                public int compare(final ModuleRegistration o1, final ModuleRegistration o2) {
                    if (o1.requires(o2)) {
                        return 1;
                    }
                    if (o2.requires(o1)) {
                        return -1;
                    }
                    return 0;
                }
            });
            deps.add(entry.getKey());
            alldeps.add(deps);
        }
        // sort the lists of dependencies among each other
        Collections.sort(alldeps, new Comparator<List<ModuleRegistration>>() {
            @Override
            public int compare(final List<ModuleRegistration> o1, final List<ModuleRegistration> o2) {
                for (ModuleRegistration r1 : o1) {
                    for (ModuleRegistration r2 : o2) {
                        if (r1.requires(r2)) {
                            return 1;
                        }
                        if (r2.requires(r1)) {
                            return -1;
                        }
                    }
                }
                return 0;
            }
        });
        // merge all the lists of dependencies into the result
        List<ModuleRegistration> result = new ArrayList<ModuleRegistration>();
        for (List<ModuleRegistration> deps : alldeps) {
            for (ModuleRegistration dep : deps) {
                if (!result.contains(dep)) {
                    result.add(dep);
                }
            }

        }
        // add the rest of the registrations to the end of the result
        for (ModuleRegistration registration : registrations) {
            if (!result.contains(registration)) {
                result.add(registration);
            }
        }
        return result;
    }
    */

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
