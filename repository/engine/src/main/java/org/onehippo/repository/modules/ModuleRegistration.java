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


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

class ModuleRegistration {

    private final String moduleName;
    private final Class<? extends DaemonModule> moduleClass;
    private DaemonModule module;

    ModuleRegistration(String moduleName, Class<? extends DaemonModule> moduleClass) {
        this.moduleName = moduleName;
        this.moduleClass = moduleClass;
    }

    ModuleRegistration(DaemonModule module) {
        this(module.getClass().getName(), module.getClass());
        this.module = module;
    }

    String getModuleName() {
        return moduleName;
    }

    Class<? extends DaemonModule> getModuleClass() {
        return moduleClass;
    }

    DaemonModule getModule() throws IllegalAccessException, InstantiationException {
        if (module == null) {
            module = moduleClass.newInstance();
        }
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

}
