/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.jaxrs.api;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.jaxrs.CXFRepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Registers a JsonResource with as endpoint the value of the {@link #ENDPOINT_ADDRESS} property.
 *
 * Custom implementations should implement {@link #getRestResource(ManagedUserSessionInvoker)} to provide their own
 * Resource.
 */
public abstract class JsonResourceServiceModule extends AbstractReconfigurableDaemonModule {
    private static final String ENDPOINT_ADDRESS = "jaxrs.endpoint.address";
    private String endpointAddress;
    private RepositoryJaxrsEndpoint jaxrsEndpoint;

    @Override
    protected final synchronized void doConfigure(final Node moduleConfig) throws RepositoryException {
        endpointAddress = RepositoryJaxrsEndpoint.qualifiedAddress(
                JcrUtils.getStringProperty(moduleConfig, ENDPOINT_ADDRESS, moduleConfig.getParent().getName()));
        if (jaxrsEndpoint != null) {
            String currentAddress = jaxrsEndpoint.getAddress();
            if (!endpointAddress.equals(currentAddress)) {
                RepositoryJaxrsService.removeEndpoint(currentAddress);
                jaxrsEndpoint.address(endpointAddress);
                RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);
            }
        }
    }

    @Override
    protected final void doInitialize(final Session session) throws RepositoryException {
        if (endpointAddress == null) {
            throw new IllegalStateException(String.format("%s requires a hippo:moduleconfig",getClass().getSimpleName()));
        }
        final ManagedUserSessionInvoker managedUserSessionInvoker = new ManagedUserSessionInvoker(session);
        jaxrsEndpoint = new CXFRepositoryJaxrsEndpoint(endpointAddress)
                .invoker(managedUserSessionInvoker)
                .singleton(getRestResource(managedUserSessionInvoker))
                .singleton(new JacksonJsonProvider());
        RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);
    }

    protected abstract Object getRestResource(ManagedUserSessionInvoker managedUserSessionInvoker);

    @Override
    protected final void doShutdown() {
        if (jaxrsEndpoint != null) {
            RepositoryJaxrsService.removeEndpoint(jaxrsEndpoint.getAddress());
        }
        jaxrsEndpoint = null;
    }
}
