/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.l10n;

import java.util.Locale;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.DaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.l10n.LocalizationService.TRANSLATIONS_PATH;

@ProvidesService(types = LocalizationService.class)
public class LocalizationModule implements DaemonModule {

    private static final Logger log = LoggerFactory.getLogger(LocalizationModule.class);


    private Session session;
    private ModuleConfigurationListener listener;
    private Map<ResourceBundleKey, ResourceBundle> bundles;
    private LocalizationService service;

    @Override
    public void initialize(final Session session) throws RepositoryException {
        this.session = session;
        HippoServiceRegistry.register(service = (name, locale) -> {
            ResourceBundle bundle = null;
            if (bundles != null) {
                bundle = bundles.get(new ResourceBundleKey(name, locale));
                // try less specific locales
                if (bundle == null && locale.getVariant() != null) {
                    bundle = bundles.get(new ResourceBundleKey(name, new Locale(locale.getLanguage(), locale.getCountry())));
                }
                if (bundle == null && locale.getCountry() != null) {
                    bundle = bundles.get(new ResourceBundleKey(name, new Locale(locale.getLanguage())));
                }
                // fall back on default locale
                if (bundle == null) {
                    bundle = bundles.get(new ResourceBundleKey(name, LocalizationService.DEFAULT_LOCALE));
                }
            }
            return bundle;
        }, LocalizationService.class);
        loadBundles();
        listener = new ModuleConfigurationListener();
        listener.start();
    }

    private synchronized void loadBundles() throws RepositoryException {
        bundles = ResourceBundleLoader.load(session.getNode(TRANSLATIONS_PATH));
    }

    @Override
    public void shutdown() {
        if (service != null) {
            HippoServiceRegistry.unregister(service, LocalizationService.class);
        }
        try {
            if (listener != null) {
                listener.stop();
            }
        } catch (RepositoryException e) {
            log.error("Failed to stop listener", e);
        }
    }

    private class ModuleConfigurationListener implements EventListener {

        private static final int EVENT_TYPES = Event.NODE_ADDED | Event.NODE_REMOVED | Event.NODE_MOVED
                | Event.PROPERTY_REMOVED | Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED;


        private ModuleConfigurationListener() {
        }

        private void start() throws RepositoryException {
            session.getWorkspace().getObservationManager().
                    addEventListener(this, EVENT_TYPES, TRANSLATIONS_PATH, true, null, null, false);
        }

        private void stop() throws RepositoryException {
            session.getWorkspace().getObservationManager().removeEventListener(this);
        }

        @Override
        public void onEvent(final EventIterator events) {
            try {
                loadBundles();
            } catch (RepositoryException e) {
                log.error("Failed to reload resource bundles", e);
            }
        }

    }


}
