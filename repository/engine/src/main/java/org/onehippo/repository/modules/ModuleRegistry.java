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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModuleRegistry.class);

    private List<ModuleRegistration> registrations = new ArrayList<ModuleRegistration>();

    ModuleRegistry() {}

    void registerModule(DaemonModule module) throws RepositoryException {
        addRegistration(new ModuleRegistration(module));
    }

    void registerModule(String moduleName, Class<? extends DaemonModule> moduleClass) throws RepositoryException {
        addRegistration(new ModuleRegistration(moduleName, moduleClass));
    }

    private void addRegistration(ModuleRegistration registration) throws RepositoryException {
        if (registrations.isEmpty()) {
            registrations.add(registration);
        } else {
            final Collection<Class<?>> requirements = registration.requirements();
            int insertAfter = -1;
            if (!requirements.isEmpty()) {
                for (Class<?> service : requirements) {
                    for (ModuleRegistration other : registrations) {
                        if (other.provides().contains(service)) {
                            int index = registrations.indexOf(other);
                            if (insertAfter == -1 || index > insertAfter) {
                                insertAfter = index;
                            }
                        }
                    }
                }
            }
            final Collection<Class<?>> provides = registration.provides();
            int insertBefore = -1;
            if (!provides.isEmpty()) {
                for (Class<?> service : provides) {
                    for (ModuleRegistration other : registrations) {
                        if (other.requirements().contains(service)) {
                            int index = registrations.indexOf(other);
                            if (insertBefore == -1 || index < insertBefore) {
                                insertBefore = index;
                            }
                        }
                    }
                }
            }

            if (insertBefore > -1 && insertBefore < insertAfter) {
                final String moduleName = registration.getModuleName();
                final String requiredBy = registrations.get(insertBefore).getModuleName();
                final String dependsOn = registrations.get(insertAfter).getModuleName();
                throw new RepositoryException("Circular dependency detected: '"
                        + moduleName + "' " + "depends on '" + dependsOn
                        + "' and is required by '" + requiredBy + "'");
            }

            registrations.add(insertAfter+1, registration);
        }
    }

    List<ModuleRegistration> getModuleRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    List<ModuleRegistration> getModuleRegistrationsReverseOrder() {
        final ArrayList<ModuleRegistration> moduleRegistrations = new ArrayList<ModuleRegistration>(registrations);
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
        for (ModuleRegistration registration : registrations) {
            for (Class<?> requiredClass : registration.requirements()) {
                if (!provided.containsKey(requiredClass)) {
                    log.warn("Module {} has unsatisfied dependency on service {}",
                            registration.getModuleName(), requiredClass);
                }
            }
        }
    }

}
