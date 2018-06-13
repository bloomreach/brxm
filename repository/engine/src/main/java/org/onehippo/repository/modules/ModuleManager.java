/*
 *  Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.impl.SessionDecorator;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CMS_ONLY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MODULECONFIG;

public class ModuleManager {

    private static final Logger log = LoggerFactory.getLogger(ModuleManager.class);

    private static final String MODULES_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.MODULES_PATH;
    private static final String HIPPOSYS_CLASSNAME = "hipposys:className";

    private final Session session;
    private final ModuleRegistry registry = new ModuleRegistry();
    private final boolean isCmsWebapp;

    public ModuleManager(final Session session) {
        this.session = session;
        boolean cms;
        try {
            Class.forName("org.hippoecm.frontend.Main", false, getClass().getClassLoader());
            cms = true;
            log.info("Current application is CMS webapp : Modules that require CMS code will be initialized");
        } catch (ClassNotFoundException e) {
            log.info("Current application is not a CMS webapp : Modules that require CMS code *won't* be initialized");
            cms = false;
        }
        isCmsWebapp = cms;
    }

    ModuleManager(final Session session, final boolean isCmsWebapp) {
        this.session = session;
        this.isCmsWebapp = isCmsWebapp;
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
        for (DaemonModule module : new Modules<>(Modules.getModules(), DaemonModule.class)) {
            registerModule(module);
        }
        final Node modules = JcrUtils.getNodeIfExists(MODULES_PATH, session);
        if (modules != null) {
            for (Node node : new NodeIterable(modules.getNodes())) {
                registerModule(node);
            }
        }
        registry.checkDependencyGraph(true);
    }

    ModuleRegistration registerModule(DaemonModule module) throws RepositoryException {
        return registry.registerModule(module.getClass().getName(), module);
    }

    ModuleRegistration registerModule(Node node) {
        String moduleName = null;
        try {
            moduleName = node.getName();
            final String className = JcrUtils.getStringProperty(node, HIPPOSYS_CLASSNAME, null);
            final Boolean cmsOnly = JcrUtils.getBooleanProperty(node, HIPPO_CMS_ONLY, Boolean.FALSE);
            if (cmsOnly && !isCmsWebapp) {
                log.info("Skipping '{}' because requires CMS webapp but current webapp is not a CMS webapp", moduleName);
                return null;
            }
            if (className != null) {
                try {
                    final Class<?> moduleClass = Class.forName(className);
                    if (DaemonModule.class.isAssignableFrom(moduleClass)) {
                        final DaemonModule module = (DaemonModule) moduleClass.newInstance();
                        return registry.registerModule(moduleName, module);
                    } else {
                        log.warn("Cannot register {}: not a DaemonModule", moduleName);
                    }
                } catch (ClassNotFoundException e) {
                    log.error("Cannot register module {} of class {}: class not found", moduleName, className);
                } catch (IllegalAccessException | InstantiationException e) {
                    log.error("Failed to create daemon module {}: " + e, moduleName);
                }

            }
        } catch (RepositoryException e) {
            log.error("Failed to register module {}", moduleName, e);
        }
        return null;
    }

    private void startModules() {
        for (ModuleRegistration registration : registry.getModuleRegistrations()) {
            startModule(registration);
        }
    }

    void startModule(final ModuleRegistration registration) {
        final String moduleName = registration.getModuleName();
        final DaemonModule module = registration.getModule();
        if (module != null) {
            log.info("Starting module {}", moduleName);
            try {
                final SimpleCredentials credentials = new SimpleCredentials("system", new char[]{});
                Session moduleSession = session.impersonate(credentials);
                moduleSession = SessionDecorator.newSessionDecorator(moduleSession);
                if (module instanceof ConfigurableDaemonModule) {
                    final String moduleConfigPath = MODULES_PATH + "/" + moduleName + "/" + HIPPO_MODULECONFIG;
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
                                "module is configurable but there is no module configuration", moduleName);
                    }
                }
                try {
                    module.initialize(moduleSession);
                    registration.setSession(moduleSession);
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
                stopModule(registration);
            } catch (Exception e) {
                log.error("Error while shutting down daemon module", e);
            }
        }
    }

    void stopModule(final ModuleRegistration registration) {
        registration.getModule().shutdown();
        final Session moduleSession = registration.getSession();
        if (moduleSession != null && moduleSession.isLive()) {
            moduleSession.logout();
        }
    }
}
