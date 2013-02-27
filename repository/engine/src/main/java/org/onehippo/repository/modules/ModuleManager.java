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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.Modules;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.impl.DecoratorFactoryImpl;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleManager {

    private static final Logger log = LoggerFactory.getLogger(ModuleManager.class);

    private static final String MODULES_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.MODULES_PATH;
    private static final String CLASSNAME_PROPERTY = "hipposys:className";
    private static final String MODULECONFIG = "hippo:moduleconfig";

    private final Session session;
    private final ModuleRegistry registry = new ModuleRegistry();

    public ModuleManager(Session session) {
        this.session = session;
    }

    public void start() throws RepositoryException {
        registerModules();
        startModules();
    }

    public void stop() {
        stopModules();
        session.logout();
    }

    private void registerModules() throws RepositoryException {
        for (DaemonModule module : new Modules<DaemonModule>(Modules.getModules(), DaemonModule.class)) {
            registerModule(module);
        }
        final Node modules = JcrUtils.getNodeIfExists(MODULES_PATH, session);
        if (modules != null) {
            for (Node node : new NodeIterable(modules.getNodes())) {
                if (node != null) {
                    registerModule(node);
                }
            }
        }
        registry.checkDependencyGraph();
    }

    private void registerModule(DaemonModule module) {
        registry.registerModule(module);
    }

    private void registerModule(Node node) {
        String moduleName = null;
        try {
            moduleName = node.getName();
            final String className = JcrUtils.getStringProperty(node, CLASSNAME_PROPERTY, null);
            if (className != null) {
                try {
                    final Class<?> moduleClass = Class.forName(className);
                    if (DaemonModule.class.isAssignableFrom(moduleClass)) {
                        registry.registerModule(moduleName, (Class<? extends DaemonModule>) moduleClass);
                    } else {
                        log.warn("Cannot register {}: not a DaemonModule", moduleName);
                    }
                } catch (ClassNotFoundException e) {
                    log.error("Cannot start module {} of class {}: class not found", moduleName, className);
                }

            }
        } catch (RepositoryException e) {
            log.error("Failed to register module {}", moduleName, e);
        }
    }

    private void startModules() {
        for (ModuleRegistration registration : registry.getModuleRegistrations()) {
            startModule(registration);
        }
    }

    private void startModule(final ModuleRegistration registration) {
        final String moduleName = registration.getModuleName();
        DaemonModule module = null;
        try {
            module = registration.getModule();
        } catch (IllegalAccessException e) {
            log.error("Failed to create daemon module {}: " + e, moduleName);
        } catch (InstantiationException e) {
            log.error("Failed to create daemon module {}: " + e, moduleName);
        }
        if (module != null) {
            log.info("Starting module {}", moduleName);
            try {
                Session moduleSession = session.impersonate(new SimpleCredentials("system", new char[]{}));
                moduleSession = DecoratorFactoryImpl.getSessionDecorator(moduleSession);
                if (module instanceof ConfigurableDaemonModule) {
                    final String moduleConfigPath = MODULES_PATH + "/" + moduleName + "/" + MODULECONFIG;
                    final Node moduleConfig = JcrUtils.getNodeIfExists(moduleConfigPath, moduleSession);
                    if (moduleConfig != null) {
                        ConfigurableDaemonModule configurable = (ConfigurableDaemonModule) module;
                        try {
                            configurable.configure(moduleConfig);
                        } catch (Exception e) {
                            log.error("Configuring module {} failed", moduleName, e);
                        }
                    } else {
                        log.warn("Cannot configure daemon module {}: " +
                                "module is configurable but there is no module configuration");
                    }
                }
                try {
                    module.initialize(moduleSession);
                } catch (Exception e) {
                    log.error("Initializing module {} failed", moduleName, e);
                }
            } catch (RepositoryException e) {
                log.error("Failed to start module {}", moduleName, e);
            }
        }
    }

    private void stopModules() {
        for (ModuleRegistration registration : registry.getModuleRegistrationsReverseOrder()) {
            log.info("Shutting down module {}", registration.getModuleName());
            try {
                registration.getModule().shutdown();
            } catch (Exception e) {
                log.error("Error while shutting down daemon module", e);
            }
        }
    }
}
