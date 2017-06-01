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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.onehippo.cm.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.onehippo.cm.engine.autoexport.Constants.CONFIG_ENABLED_PROPERTY_NAME;
import static org.onehippo.cm.engine.autoexport.Constants.SERVICE_CONFIG_PATH;

public final class AutoExportServiceImpl implements EventListener {

    static final Logger log = LoggerFactory.getLogger(Constants.LOGGER_NAME);

    private static final int EVENT_TYPES = PROPERTY_ADDED | PROPERTY_CHANGED | PROPERTY_REMOVED;

    private final Session autoExportSession;
    private final ObservationManager manager;

    private NodeTypeChangesMonitor nodeTypeChangesMonitor;
    private Configuration configuration;
    private EventJournalProcessor eventJournalProcessor;


    public AutoExportServiceImpl(final Session configurationSession, final ConfigurationService configurationService)
            throws RepositoryException {
        final SimpleCredentials credentials = new SimpleCredentials(configurationSession.getUserID(), new char[]{});
        autoExportSession = configurationSession.impersonate(credentials);
        configuration = new Configuration(autoExportSession.getNode(SERVICE_CONFIG_PATH));
        manager = autoExportSession.getWorkspace().getObservationManager();
        nodeTypeChangesMonitor = new NodeTypeChangesMonitor(configuration);
        eventJournalProcessor = new EventJournalProcessor(configurationService, configuration, Collections.emptySet());
        manager.addEventListener(this, EVENT_TYPES, SERVICE_CONFIG_PATH, false, null, null, false);
        if (configuration.isEnabled()) {
            log.info("autoexport service enabled");
            eventJournalProcessor.start();
        } else {
            log.info("autoexport service disabled");
        }
    }

    public void onEvent(EventIterator iter) {
        while (iter.hasNext()) {
            Event event = iter.nextEvent();
            try {
                if ((SERVICE_CONFIG_PATH+"/"+CONFIG_ENABLED_PROPERTY_NAME).equals(event.getPath())) {
                    if (configuration.isEnabled() != configuration.checkEnabled()) {
                        if (configuration.isEnabled()) {
                            log.info("autoexport service enabled");
                            eventJournalProcessor.start();
                        } else {
                            log.info("autoexport service disabled");
                            eventJournalProcessor.stop();
                            eventJournalProcessor.runOnce();
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error occurred getting path from event.", e);
            }
        }
    }

    public void close() {
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
        if (autoExportSession != null && autoExportSession.isLive()) {
            autoExportSession.logout();
        }
    }
}
