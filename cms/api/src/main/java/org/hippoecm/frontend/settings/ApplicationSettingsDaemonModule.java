/*
 *  Copyright 2022 Bloomreach (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.settings;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = ApplicationSettings.class)
public class ApplicationSettingsDaemonModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(ApplicationSettingsDaemonModule.class);

    private final Object configurationLock = new Object();

    private ApplicationSettings settings;

    @Override
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        doShutdown();
        doConfigure(moduleConfig);
        doInitialize(session);
    }

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        synchronized (configurationLock) {
            log.info("Reconfiguring application settings daemon module.");

            settings = new ApplicationSettingsImpl(moduleConfig);
        }
    }

    @Override
    protected void doInitialize(final Session session) {
        HippoServiceRegistry.register(settings, ApplicationSettings.class);
    }

    @Override
    protected void doShutdown() {
        HippoServiceRegistry.unregister(settings, ApplicationSettings.class);
    }

    private static final class ApplicationSettingsImpl implements ApplicationSettings {

        private ContentSecurityPolicy contentSecurityPolicy;

        public ApplicationSettingsImpl(final Node moduleConfig) throws RepositoryException {
            if (moduleConfig.hasNode("content-security-policy")) {
                contentSecurityPolicy = new ContentSecurityPolicy(moduleConfig.getNode("content-security-policy"));
            }
        }

        @Override
        public ContentSecurityPolicy getContentSecurityPolicy() {
            return contentSecurityPolicy;
        }
    }
}
