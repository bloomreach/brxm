/*
 *  Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.jcr.Session;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModuleRegistration {

    private static final Logger log = LoggerFactory.getLogger(ModuleRegistration.class);

    private final String moduleName;
    private final Class<? extends DaemonModule> moduleClass;
    private DaemonModule module;
    private Session session;
    private volatile boolean cancelled;
    private final Semaphore lock = new Semaphore(1);

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

    List<Class<?>> requirements() {
        final RequiresService annotation = moduleClass.getAnnotation(RequiresService.class);
        if (annotation != null) {
            return Arrays.asList(annotation.types());
        }
        return Collections.emptyList();
    }

    Collection<Class<?>> after() {
        final After annotation = moduleClass.getAnnotation(After.class);
        if (annotation != null) {
            return Arrays.asList(annotation.modules());
        }
        return Collections.emptyList();
    }

    Boolean optional(Class<?> type) {
        final List<Class<?>> requirements = requirements();
        final int i = requirements.indexOf(type);
        final List<Boolean> optional = optional();
        if (i >= optional.size()) {
            if (!optional.isEmpty()) {
                log.warn("The optional attribute of @RequiresService should be " +
                        "absent or equal to the length of the types attribute");
            }
            return false;
        }
        return optional.get(i);
    }

    List<Boolean> optional() {
        final RequiresService annotation = moduleClass.getAnnotation(RequiresService.class);
        if (annotation != null) {
            final Boolean[] optional = ArrayUtils.toObject(annotation.optional());
            if (optional != null) {
                return Arrays.asList(optional);
            }
        }
        return Collections.emptyList();
    }

    boolean after(ModuleRegistration other) {
        final Collection<Class<?>> requirements = requirements();
        if (!requirements.isEmpty()) {
            final Collection<Class<?>> provides = other.provides();
            for (Class<?> requirement : requirements) {
                if (provides.contains(requirement)) {
                    return true;
                }
            }
        }
        final Collection<Class<?>> after = after();
        if (!after.isEmpty()) {
            for (Class<?> aClass : after) {
                if (other.getModuleClass().equals(aClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    void setSession(final Session session) {
        this.session = session;
    }

    Session getSession() {
        return session;
    }

    void cancel() {
        cancelled = true;
    }

    boolean isCancelled() {
        return cancelled;
    }

    void acquire() {
        try {
            lock.acquire();
        } catch (InterruptedException ignore) {
        }
    }

    void release() {
        lock.release();
    }

}
