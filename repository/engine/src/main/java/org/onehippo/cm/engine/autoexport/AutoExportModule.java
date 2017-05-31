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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cm.ConfigurationService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.ConfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AutoExportModule implements ConfigurableDaemonModule {

    static final Logger log = LoggerFactory.getLogger(Constants.LOGGER_NAME);

    private NodeTypeChangesMonitor nodeTypeChangesMonitor;
    private Configuration configuration;
    private AutoExportService autoExportService;
    private EventJournalProcessor eventJournalProcessor;

    public AutoExportModule() {}

    @Override
    public void initialize(Session session) throws RepositoryException {
        // nothing to do here, already configured
    }

    @Override
    public void configure(final Node moduleConfigNode) throws RepositoryException {
        if (!Boolean.getBoolean(Constants.SYSTEM_ALLOWED_PROPERTY_NAME)) {
            log.info("AutoExport system property "+ Constants.SYSTEM_ALLOWED_PROPERTY_NAME +
                    " not set to true: AutoExport Service not started.");
            return;
        }
        final ConfigurationService configurationService = HippoServiceRegistry.getService(ConfigurationService.class);
        if (configurationService == null) {
            log.error("ConfigurationService not available: AutoExport Service cannot be started");
            return;
        }
        configuration = new Configuration(moduleConfigNode);
        nodeTypeChangesMonitor = new NodeTypeChangesMonitor(configuration);
        autoExportService = new AutoExportService() {
            @Override
            public boolean isEnabled() {
                return configuration.isEnabled();
            }

            @Override
            public void setEnabled(final boolean enabled) throws RepositoryException {
                configuration.setEnabled(enabled);
                if (enabled) {
                    eventJournalProcessor.start();
                } else {
                    eventJournalProcessor.stop();
                    eventJournalProcessor.runOnce();
                }
            }
        };
        eventJournalProcessor = new EventJournalProcessor(configurationService, configuration, Collections.emptySet());
        if (configuration.isEnabled()) {
            eventJournalProcessor.start();
        }
        HippoServiceRegistry.registerService(autoExportService, AutoExportService.class);
    }

    @Override
    public void shutdown() {
        if (autoExportService != null) {
            HippoServiceRegistry.unregisterService(autoExportService, AutoExportService.class);
        }
        if (eventJournalProcessor != null) {
            eventJournalProcessor.shutdown();
        }
        if (nodeTypeChangesMonitor != null) {
            nodeTypeChangesMonitor.shutdown();
        }
    }
}
