/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.onehippo.cm.engine.ConfigurationServiceImpl;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.CONFIG_ENABLED_PROPERTY_NAME;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.SERVICE_CONFIG_PATH;

public final class AutoExportServiceImpl implements EventListener {

    static final Logger log = LoggerFactory.getLogger(AutoExportConstants.LOGGER_NAME);

    private static final int EVENT_TYPES = PROPERTY_ADDED | PROPERTY_CHANGED | PROPERTY_REMOVED;

    private final Session autoExportConfigSession;
    private final ObservationManager manager;

    private NodeTypeChangesMonitor nodeTypeChangesMonitor;
    private AutoExportConfig autoExportConfig;
    private boolean configIsValid;
    private EventJournalProcessor eventJournalProcessor;
    private AtomicBoolean running = new AtomicBoolean(false);


    public AutoExportServiceImpl(final Session configurationSession, final ConfigurationServiceImpl configurationService)
            throws RepositoryException {
        final SimpleCredentials credentials = new SimpleCredentials(configurationSession.getUserID(), new char[]{});
        autoExportConfigSession = configurationSession.impersonate(credentials);
        autoExportConfig = new AutoExportConfig(autoExportConfigSession.getNode(SERVICE_CONFIG_PATH));
        manager = autoExportConfigSession.getWorkspace().getObservationManager();
        nodeTypeChangesMonitor = new NodeTypeChangesMonitor(autoExportConfig);
        eventJournalProcessor = new EventJournalProcessor(configurationService, autoExportConfig, Collections.emptySet());
        manager.addEventListener(this, EVENT_TYPES, SERVICE_CONFIG_PATH, false, null, null, false);

        configIsValid = checkModules(configurationService.getRuntimeConfigurationModel());

        if (!configIsValid) {
            log.error("autoexport config is invalid -- see previous error log messages");
            throw new IllegalStateException("autoexport config is invalid");
        }

        if (autoExportConfig.isEnabled()) {
            log.info("autoexport service enabled");
            eventJournalProcessor.start();
            running.set(true);
        } else {
            log.info("autoexport service disabled");
        }
    }

    /**
     * Confirm that at least one module is configured for auto-export, and that exported modules are at the end of the
     * sequence of applied modules.
     * @param baseline
     */
    private boolean checkModules(final ConfigurationModelImpl baseline) {
        if (autoExportConfig.getModules().keySet().isEmpty()) {
            log.error("autoexport is configured with zero modules to export!");
            return false;
        }

        // confirm that auto-export modules have no modules following them that are not also being exported
        // (unless the trailing module only has webfiles)
        boolean startedAutoExport = false;
        for (final ModuleImpl module : baseline.getModules()) {
            // once we encounter an exported module, make sure all following modules are auto-exported
            if (!startedAutoExport && module.getMvnPath() != null) {
                startedAutoExport = true;
            }
            if (startedAutoExport && module.getMvnPath() == null) {
                // check definitions for that module to see if there's anything other than webfiles
                final int nonWebfilesDefCount = module.getContentDefinitions().size()
                        + module.getConfigDefinitions().size()
                        + module.getNamespaceDefinitions().size();
                if (nonWebfilesDefCount > 0) {
                    // cannot have a sequence from exported to not-exported!
                    log.error("autoexport modules must be the last modules applied to configuration or content, but found additional module: {}", module.getFullName());
                    return false;
                }
                else {
                    // but webfiles-only modules are okay, even if mixed in with or trailing exported modules
                    log.debug("autoexport detected a dependent module, allowed because it is either empty or includes webfilebundle definitions only: {}", module.getFullName());
                }
            }
        }

        return true;
    }

    public void onEvent(EventIterator iter) {
        while (iter.hasNext()) {
            Event event = iter.nextEvent();
            try {
                if ((SERVICE_CONFIG_PATH+"/"+CONFIG_ENABLED_PROPERTY_NAME).equals(event.getPath())) {
                    if (autoExportConfig.isEnabled() != autoExportConfig.checkEnabled()) {
                        if (configIsValid) {
                            if (autoExportConfig.isEnabled()) {
                                log.info("autoexport service enabled");
                                eventJournalProcessor.start();
                                running.set(true);
                            } else {
                                log.info("autoexport service disabled, processing remaining changes");
                                running.set(false);
                                eventJournalProcessor.stop();
                            }
                        }
                        else {
                            log.error("autoexport config is invalid -- see error log messages at repository startup");
                        }
                    }
                }
                // todo: should we reload auto-export config on config changes other than enablement?
            } catch (RepositoryException e) {
                log.error("Error occurred getting path from event.", e);
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public void runOnce() {
        if (configIsValid) {
            log.info("running single autoexport cycle");
            eventJournalProcessor.runOnce();
        }
        else {
            log.error("autoexport has invalid config and cannot run -- see error log messages at repository startup");
        }
    }

    public void close() {
        running.set(false);
        if (manager != null) {
            try {
                manager.removeEventListener(this);
            } catch (RepositoryException e) {
            }
        }
        if (eventJournalProcessor != null) {
            eventJournalProcessor.shutdown();
        }
        if (nodeTypeChangesMonitor != null) {
            nodeTypeChangesMonitor.shutdown();
        }
        if (autoExportConfigSession != null && autoExportConfigSession.isLive()) {
            autoExportConfigSession.logout();
        }
    }
}
