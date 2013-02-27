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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModuleRegistry.class);

    private List<ModuleRegistration> registrations = new ArrayList<ModuleRegistration>();

    ModuleRegistry() {}

    void registerModule(DaemonModule module) {
        registrations.add(new ModuleRegistration(module));
    }

    void registerModule(String moduleName, Class<? extends DaemonModule> moduleClass) {
        registrations.add(new ModuleRegistration(moduleName, moduleClass));
    }

    List<ModuleRegistration> getModuleRegistrations() {
        final ArrayList<ModuleRegistration> moduleRegistrations = new ArrayList<ModuleRegistration>(registrations);
        Collections.sort(moduleRegistrations);
        return Collections.unmodifiableList(moduleRegistrations);
    }

    List<ModuleRegistration> getModuleRegistrationsReverseOrder() {
        final ArrayList<ModuleRegistration> moduleRegistrations = new ArrayList<ModuleRegistration>(registrations);
        Collections.sort(moduleRegistrations);
        Collections.reverse(moduleRegistrations);
        return Collections.unmodifiableList(moduleRegistrations);
    }

    void checkDependencyGraph() throws RepositoryException {
        final Map<Class<?>, ModuleRegistration> provided = new HashMap<Class<?>, ModuleRegistration>();
        for (ModuleRegistration registration : registrations) {
            for (Class<?> aClass : registration.provides()) {
                provided.put(aClass, registration);
            }
        }
        final Map<ModuleRegistration, List<ModuleRegistration>> requirements =
                new HashMap<ModuleRegistration, List<ModuleRegistration>>();
        for (ModuleRegistration registration : registrations) {
            final List<ModuleRegistration> requires = new ArrayList<ModuleRegistration>();
            for (Class<?> requiredClass : registration.requirements()) {
                if (provided.containsKey(requiredClass)) {
                    requires.add(provided.get(requiredClass));
                } else {
                    log.warn("Module {} has unsatisfied dependency on service {}",
                            registration.getModuleName(), requiredClass);
                }
            }
            requirements.put(registration, requires);
        }
        for (ModuleRegistration registration : registrations) {
            detectCircularDependency(registration, requirements, new HashSet<ModuleRegistration>());
        }
    }

    private void detectCircularDependency(final ModuleRegistration module,
                                          final Map<ModuleRegistration, List<ModuleRegistration>> requirements,
                                          final Set<ModuleRegistration> modules) throws RepositoryException {
        if (modules.contains(module)) {
            final StringBuilder sb = new StringBuilder();
            for (ModuleRegistration m: modules) {
                sb.append(m.getModuleName() != null ? m.getModuleName() : m.getModuleClass());
                sb.append(", ");
            }
            sb.append(module.getModuleName() != null ? module.getModuleName() : module.getModuleClass());
            throw new RepositoryException("Circular dependency detected among modules: " + sb);
        }
        modules.add(module);
        for (ModuleRegistration requirement : requirements.get(module)) {
            detectCircularDependency(requirement, requirements, new HashSet<ModuleRegistration>(modules));
        }
    }

}
