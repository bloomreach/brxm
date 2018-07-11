/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.services.autoreload;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = { AutoReloadService.class })
public class AutoReloadServiceModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(AutoReloadServiceModule.class);

    private final AutoReloadServiceConfig config;
    private AutoReloadService service;

    public AutoReloadServiceModule() {
        config = new AutoReloadServiceConfig();
    }

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        config.reconfigure(moduleConfig);
        log.info("Automatic reload of browsers is {}", config.isEnabled() ? "enabled" : "disabled");
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        service = new AutoReloadServiceImpl(config, new AutoReloadScriptLoader(), AutoReloadServer.getInstance());
        HippoServiceRegistry.register(service, AutoReloadService.class);
    }

    @Override
    protected void doShutdown() {
        if (service != null) {
            HippoServiceRegistry.unregister(service, AutoReloadService.class);
        }
    }
}
