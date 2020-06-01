/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.scxml;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;

/**
 * SCXMLRegistryModule is a repository daemon module providing access to the {@link SCXMLRegistry} and
 * {@link SCXMLExecutorFactory} services, implemented through {@link RepositorySCXMLRegistry} and
 * {@link RepositorySCXMLExecutorFactory}.
 * <p>
 * The SCXMLRegistry service is automatically refreshed when this daemon module its configuration (node) is changed,
 * thereby leading to auto-refresh/re-load of SCXML state machine definitions.
 * </p>
 */
@ProvidesService( types = { SCXMLRegistry.class, SCXMLExecutorFactory.class } )
public class SCXMLRegistryModule extends AbstractReconfigurableDaemonModule {

    private RepositorySCXMLRegistry scxmlRegistry = new RepositorySCXMLRegistry();
    private RepositorySCXMLExecutorFactory scxmlExecutorFactory = new RepositorySCXMLExecutorFactory();

    @Override
    protected void doConfigure(Node moduleConfig) throws RepositoryException {
        scxmlRegistry.reconfigure(moduleConfig);
    }

    @Override
    protected void doInitialize(Session session) throws RepositoryException {
        scxmlRegistry.initialize();
        HippoServiceRegistry.registerService(scxmlRegistry, SCXMLRegistry.class);

        scxmlExecutorFactory.initialize();
        HippoServiceRegistry.registerService(scxmlExecutorFactory, SCXMLExecutorFactory.class);
    }

    @Override
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        super.onConfigurationChange(moduleConfig);
        scxmlRegistry.refresh();
    }

    @Override
    protected void doShutdown() {
        HippoServiceRegistry.unregisterService(scxmlExecutorFactory, SCXMLExecutorFactory.class);
        scxmlExecutorFactory.destroy();

        HippoServiceRegistry.unregisterService(scxmlRegistry, SCXMLRegistry.class);
        scxmlRegistry.destroy();
    }
}
