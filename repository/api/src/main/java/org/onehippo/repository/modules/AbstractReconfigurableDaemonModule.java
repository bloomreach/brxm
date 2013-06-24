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
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for {@link ConfigurableDaemonModule}s that wish to
 * reconfigure when their module configuration changes in the repository.
 * <p>
 * Note that implementations must make sure to take care of thread safety
 * issues that arise as a consequence of a reconfiguration callback.
 * This can happen at any time, possibly in the middle of servicing other threads.
 * </p>
 */
public abstract class AbstractReconfigurableDaemonModule implements ConfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(AbstractReconfigurableDaemonModule.class);

    protected String moduleName;
    protected String moduleConfigPath;
    protected Session session;
    private ModuleConfigurationListener listener;

    @Override
    public final void configure(final Node moduleConfig) throws RepositoryException {
        moduleName = moduleConfig.getName();
        moduleConfigPath = moduleConfig.getPath();
        doConfigure(moduleConfig);
    }

    /**
     * Lifecycle callback to allow a {@link DaemonModule} to configure itself.
     * This method is called on startup iff there is module config node,
     * and before {@link #initialize} is called. This method is also called when
     * the module config node or any of its descendants changes.
     * @param moduleConfig  the node containing the configuration of this module
     * @throws javax.jcr.RepositoryException
     */
    protected abstract void doConfigure(final Node moduleConfig) throws RepositoryException;

    @Override
    public final void initialize(final Session session) throws RepositoryException {
        this.session = session;
        doInitialize(session);
        this.listener = new ModuleConfigurationListener(moduleName, moduleConfigPath);
        this.listener.start();
    }

    /**
     * Lifecycle callback that is called when this module is started.
     * @param session  a {@link Session} that can be used throughout this module's life.
     * @throws RepositoryException
     */
    protected abstract void doInitialize(final Session session) throws RepositoryException;

    @Override
    public final void shutdown() {
        try {
            if (listener != null) {
                listener.stop();
            }
        } catch (RepositoryException e) {
            log.warn("Error while stopping configuration listener for module {}", moduleName);
        }
        doShutdown();
    }

    /**
     * Lifecycle callback method that is called by the repository before shutting down  .
     */
    protected abstract void doShutdown();

    /**
     * Called when the module configuration has changed. The default implementation
     * just calls {@link #doConfigure(javax.jcr.Node)} but this method may be overridden.
     * @param moduleConfig  the new module configuration node
     * @throws RepositoryException
     */
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        doConfigure(moduleConfig);
    }

    /**
     * Called by the module configuration listener on event. As an optimization, may be overridden
     * to return false for events that are actually not a configuration change, so that they don't trigger
     * a reconfiguration.
     * @param event event returned by the EventIterator
     * @return true if this event requires reloading of the configuration
     * @throws RepositoryException
     */
    protected boolean isReconfigureEvent(Event event) throws RepositoryException {
        return true;
    }

    private class ModuleConfigurationListener implements EventListener {

        private static final int EVENT_TYPES = Event.NODE_ADDED | Event.NODE_REMOVED | Event.NODE_MOVED
                | Event.PROPERTY_REMOVED | Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED;

        private final String moduleConfigPath;
        private final String moduleName;

        private ModuleConfigurationListener(String moduleName, String moduleConfigPath) {
            this.moduleConfigPath = moduleConfigPath;
            this.moduleName = moduleName;
        }

        private void start() throws RepositoryException {
            session.getWorkspace().getObservationManager().
                    addEventListener(this, EVENT_TYPES, moduleConfigPath, true, null, null, false);
        }

        private void stop() throws RepositoryException {
            session.getWorkspace().getObservationManager().removeEventListener(this);
        }

        @Override
        public void onEvent(final EventIterator events) {
            boolean reconfigure = false;
            while (events.hasNext()) {
                try {
                    if (isReconfigureEvent(events.nextEvent())) {
                        reconfigure = true;
                        break;
                    }
                } catch (RepositoryException e) {
                    log.error("Failed to determine if event is a reconfigure event", e);
                }
            }
            if (reconfigure) {
                try {
                    final Node moduleConfig = JcrUtils.getNodeIfExists(moduleConfigPath, session);
                    if (moduleConfig != null) {
                        onConfigurationChange(moduleConfig);
                    } else {
                        log.warn("Configuration for module {} not found", moduleName);
                    }
                } catch (RepositoryException e) {
                    log.error("Failed to reconfigure module {}", moduleName, e);
                }
            }
        }

    }

}
