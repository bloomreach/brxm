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

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

import org.hippoecm.repository.Modules;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.impl.DecoratorFactoryImpl;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_EXECUTED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MODULECONFIG;

public class ModuleManager {

    private static final Logger log = LoggerFactory.getLogger(ModuleManager.class);

    private static final String MODULES_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.MODULES_PATH;
    private static final String HIPPOSYS_CLASSNAME = "hipposys:className";

    private final Session session;
    private final ModuleRegistry registry = new ModuleRegistry();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private Future keepAliveFuture;
    private volatile boolean execute = true;

    public ModuleManager(Session session) {
        this.session = session;
    }

    public void start() throws RepositoryException {
        registerModules();
        startModules();
        executeModules();
    }

    public void stop() {
        cancelModules();
        stopModules();
        session.logout();
        executorService.shutdown();
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
            final Calendar executed = JcrUtils.getDateProperty(node, HIPPO_EXECUTED, null);
            if (className != null && executed == null) {
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
                moduleSession = DecoratorFactoryImpl.getSessionDecorator(moduleSession, credentials);
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

    private void executeModules() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                for (ModuleRegistration registration : registry.getModuleRegistrations()) {
                    if (!execute) {
                        break;
                    }
                    if (registration.getModule() instanceof ExecutableDaemonModule) {
                        executeModule(registration);
                    }
                }
            }
        });
    }

    private void cancelModules() {
        execute = false;
        for (ModuleRegistration registration : registry.getModuleRegistrations()) {
            if (registration.getModule() instanceof ExecutableDaemonModule) {
                cancelModule(registration);
            }
        }
    }

    void executeModule(final ModuleRegistration registration) {
        final String moduleName = registration.getModuleName();
        final String modulePath = MODULES_PATH + "/" + moduleName;
        final ExecutableDaemonModule module = (ExecutableDaemonModule) registration.getModule();
        log.info("Executing module {}", moduleName);
        if (lock(registration)) {
            startLockKeepAlive(session, modulePath);
            try {
                module.execute();
                if (!registration.isCancelled()) {
                    markExecuted(registration);
                }
            } catch (RepositoryException e) {
                log.error("Executing module {} failed", new String[] { moduleName }, e);
            } finally {
                unlock(registration);
            }
        }
    }

    void cancelModule(final ModuleRegistration registration) {
        log.info("Cancelling module {}", registration.getModuleName());
        final ExecutableDaemonModule module = (ExecutableDaemonModule) registration.getModule();
        registration.cancel();
        module.cancel();
        registration.acquire();
    }

    private boolean lock(final ModuleRegistration registration) {
        final String moduleName = registration.getModuleName();
        final String modulePath = MODULES_PATH + "/" + moduleName;
        log.debug("Trying to obtain lock on module {}", moduleName);
        try {
            final LockManager lockManager = session.getWorkspace().getLockManager();
            if (!lockManager.isLocked(modulePath)) {
                try {
                    ensureIsLockable(modulePath);
                    lockManager.lock(modulePath, false, false, 60 * 2, getClusterNodeId(session));
                    registration.acquire();
                    log.debug("Lock successfully obtained on module {}", moduleName);
                    return true;
                } catch (LockException e) {
                    // happens when other cluster node beat us to it
                    log.debug("Failed to set lock on {}: {}", moduleName, e.getMessage());
                }
            } else {
                log.debug("Module {} already locked", moduleName);
            }
        } catch (RepositoryException e) {
            log.error("Failed to set lock on module " + moduleName, e);
        }
        return false;
    }

    private void unlock(final ModuleRegistration registration) {
        final String moduleName = registration.getModuleName();
        final String modulePath = MODULES_PATH + "/" + moduleName;
        log.debug("Trying to release lock on module {}", moduleName);
        stopLockKeepAlive();
        try {
            final LockManager lockManager = session.getWorkspace().getLockManager();
            if (lockManager.isLocked(modulePath)) {
                final Lock lock = lockManager.getLock(modulePath);
                if (lock.isLockOwningSession()) {
                    lockManager.unlock(modulePath);
                    registration.release();
                    log.debug("Lock successfully released on module {}", moduleName);
                } else {
                    log.debug("We don't own the lock on module {}", moduleName);
                }
            } else {
                log.debug("Module {} not locked", moduleName);
            }
        } catch (RepositoryException e) {
            log.error("Failed to release lock on module " + moduleName, e);
        }
    }

    private void ensureIsLockable(final String nodePath) throws RepositoryException {
        final Node node = session.getNode(nodePath);
        if (!node.isNodeType(JcrConstants.MIX_LOCKABLE)) {
            node.addMixin(JcrConstants.MIX_LOCKABLE);
        }
        session.save();
    }

    private static String getClusterNodeId(Session session) {
        String clusteNodeId = session.getRepository().getDescriptor("jackrabbit.cluster.id");
        if (clusteNodeId == null) {
            clusteNodeId = "default";
        }
        return clusteNodeId;
    }

    private static void refreshLock(final Session session, final String nodePath) {
        try {
            final LockManager lockManager = session.getWorkspace().getLockManager();
            final Lock lock = lockManager.getLock(nodePath);
            lock.refresh();
            log.debug("Lock successfully refreshed");
        } catch (RepositoryException e) {
            log.error("Failed to refresh lock", e);
        }
    }

    private void startLockKeepAlive(final Session session, final String nodePath) {
        keepAliveFuture = executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                refreshLock(session, nodePath);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private void stopLockKeepAlive() {
        if (keepAliveFuture != null && !keepAliveFuture.isDone()) {
            keepAliveFuture.cancel(true);
        }
    }

    private void markExecuted(final ModuleRegistration registration) {
        final String moduleName = registration.getModuleName();
        final String nodePath = MODULES_PATH + "/" + moduleName;
        try {
            final Node node = session.getNode(nodePath);
            node.setProperty(HippoNodeType.HIPPO_EXECUTED, Calendar.getInstance());
            session.save();
        } catch (RepositoryException e) {
            log.error("Failed to mark module {} as executed", moduleName);
            try {
                session.refresh(false);
            } catch (RepositoryException e1) {
                log.error("Failed to refresh session", e1);
            }
        }
    }
}
