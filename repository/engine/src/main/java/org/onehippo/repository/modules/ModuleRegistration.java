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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jcr.Session;

class ModuleRegistration {

    private final String moduleName;
    private final Class<? extends DaemonModule> moduleClass;
    private DaemonModule module;
    private Session session;

    ModuleRegistration(final String moduleName, final DaemonModule module) {
        this.moduleName = moduleName;
        this.moduleClass = module.getClass();
        this.module = module;
    }

    String getModuleName() {
        return moduleName;
    }

    Class<? extends DaemonModule> getModuleClass() {
        return moduleClass;
    }

    DaemonModule getModule() {
        return module;
    }

    Collection<Class<?>> provides() {
        final ProvidesService annotation = moduleClass.getAnnotation(ProvidesService.class);
        if (annotation != null) {
            return Arrays.asList(annotation.types());
        }
        return Collections.emptyList();
    }

    Collection<Class<?>> requirements() {
        final RequiresService annotation = moduleClass.getAnnotation(RequiresService.class);
        if (annotation != null) {
            return Arrays.asList(annotation.types());
        }
        return Collections.emptyList();
    }

    boolean requires(ModuleRegistration other) {
        final Collection<Class<?>> requirements = requirements();
        if (!requirements.isEmpty()) {
            final Collection<Class<?>> provides = other.provides();
            for (Class<?> requirement : requirements) {
                if (provides.contains(requirement)) {
                    return true;
                }
            }
        }
        return false;
    }

    int compare(ModuleRegistration other, List<ModuleRegistration> all) {
        if (this.requires(other)) {
            return 1;
        }
        if (other.requires(this)) {
            return -1;
        }
        final List<ModuleRegistration> dependencies = new ArrayList<ModuleRegistration>();
        final List<ModuleRegistration> dependents = new ArrayList<ModuleRegistration>();
        for (ModuleRegistration registration : all) {
            if (registration == this || registration == other) {
                continue;
            }
            if (this.requires(registration)) {
                dependencies.add(registration);
            }
            if (registration.requires(this)) {
                dependents.add(registration);
            }
        }
        final ArrayList<ModuleRegistration> rest = new ArrayList<ModuleRegistration>(all);
        rest.remove(this);
        for (ModuleRegistration dependency : dependencies) {
            if (dependency.compare(other, rest) > 0) {
                return 1;
            }
        }
        for (ModuleRegistration dependent : dependents) {
            if (dependent.compare(other, rest) < 0) {
                return -1;
            }
        }
        return 0;
    }

    void setSession(final Session session) {
        this.session = session;
    }

    Session getSession() {
        return session;
    }
}
