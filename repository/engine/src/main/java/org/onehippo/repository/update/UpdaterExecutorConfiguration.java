/*
 *  Copyright 2015-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.update;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.spi.Event;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdaterExecutorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(UpdaterExecutorConfiguration.class);

    private static final String ILLEGAL_PACKAGES_PROPERTY = "illegalPackages";
    private static final String ILLEGAL_METHODS_PROPERTY = "illegalMethods";
    private static final String ILLEGAL_CLASSES_PROPERTY = "illegalClasses";

    private Collection<Package> illegalPackages;
    private Collection<Class> illegalClasses;
    private Collection<Method> illegalMethods;
    private final Node config;

    UpdaterExecutorConfiguration(final Node config) throws RepositoryException {
        this.config = config;
        init();
    }

    void start() throws RepositoryException {
        config.getSession().getWorkspace().getObservationManager().addEventListener(new EventListener() {
            @Override
            public void onEvent(final EventIterator events) {
                try {
                    init();
                } catch (RepositoryException e) {
                    log.error("Failed to reinitialize UpdateExpressionCheckerConfiguration after configuration change", e);
                }
            }
        }, Event.ALL_TYPES, config.getPath(), false, null, null, false);
    }

    private synchronized void init() throws RepositoryException {
        initIllegalPackages(JcrUtils.getPropertyIfExists(config, ILLEGAL_PACKAGES_PROPERTY));
        initIllegalClasses(JcrUtils.getPropertyIfExists(config, ILLEGAL_CLASSES_PROPERTY));
        initIllegalMethods(JcrUtils.getPropertyIfExists(config, ILLEGAL_METHODS_PROPERTY));
    }

    private void initIllegalPackages(final Property property) throws RepositoryException {
        if (property == null) {
            log.debug("No custom illegal packages");
            this.illegalPackages = Collections.emptyList();
            return;
        }
        final Collection<Package> illegalPackages = new ArrayList<>();
        for (Value value : property.getValues()) {
            String packageName = value.getString();
            final Package p = Package.getPackage(packageName);
            if (p == null) {
                log.warn("No such package found: {}", packageName);
            } else {
                illegalPackages.add(p);
            }
        }
        this.illegalPackages = illegalPackages;
    }

    private void initIllegalMethods(final Property illegalMethodsProperty) throws RepositoryException {
        if (illegalMethodsProperty == null) {
            log.debug("No custom illegal methods");
            illegalMethods = Collections.emptyList();
            return;
        }
        final Collection<Method> illegalMethods = new ArrayList<>();
        for (Value value : illegalMethodsProperty.getValues()) {
            final String illegalMethod = value.getString();
            final int offset = illegalMethod.indexOf('#');
            if (offset != -1) {
                final String className = illegalMethod.substring(0, offset);
                final String methodName = illegalMethod.substring(offset+1);
                boolean found = false;
                try {
                    final Class klass = Class.forName(className);
                    for (Method method : klass.getMethods()) {
                        if (method.getName().equals(methodName)) {
                            found = true;
                            log.debug("Adding method '{}' to list of illegal methods", method);
                            illegalMethods.add(method);
                        }
                    }
                    if (!found) {
                        log.warn("Method not found: '{}'", illegalMethod);
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("Class not found for unauthorized call expression '{}'", illegalMethod);
                }
            } else {
                log.warn("No method expression: '{}'", illegalMethod);
            }
        }
        this.illegalMethods = illegalMethods;
    }

    private synchronized void initIllegalClasses(final Property illegalClassesProperty) throws RepositoryException {
        if (illegalClassesProperty == null) {
            log.debug("No custom illegal classes");
            illegalClasses = Collections.emptyList();
            return;
        }
        final Collection<Class> illegalClasses = new ArrayList<>();
        for (Value value : illegalClassesProperty.getValues()) {
            try {
                illegalClasses.add(Class.forName(value.getString()));
            } catch (ClassNotFoundException e) {
                log.warn("Class not found '{}'", value.getString());
            }
        }
        this.illegalClasses = illegalClasses;
    }

    synchronized Collection<Package> getIllegalPackages() {
        return illegalPackages;
    }

    synchronized Collection<Class> getIllegalClasses() {
        return illegalClasses;
    }

    synchronized Collection<Method> getIllegalMethods() {
        return illegalMethods;
    }

}
