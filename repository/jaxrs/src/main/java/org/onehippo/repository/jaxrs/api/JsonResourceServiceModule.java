/*
 * Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.jaxrs.CXFRepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.jaxrs.event.JcrEventListener;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Registers a JsonResource with as endpoint the value of the {@link #ENDPOINT_ADDRESS} property.
 * <p>
 * Custom implementations should implement {@link #getRestResource(SessionRequestContextProvider)} to provide their own
 * Resource.
 */
public abstract class JsonResourceServiceModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(JsonResourceServiceModule.class);

    private static final String ENDPOINT_ADDRESS = "jaxrs.endpoint.address";

    private String endpointAddress;
    private RepositoryJaxrsEndpoint jaxrsEndpoint;

    private final List<JcrEventListener> listeners = new ArrayList<>();

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        endpointAddress = RepositoryJaxrsEndpoint.qualifiedAddress(
                JcrUtils.getStringProperty(moduleConfig, ENDPOINT_ADDRESS, moduleConfig.getParent().getName()));
        if (jaxrsEndpoint != null) {
            final String currentAddress = jaxrsEndpoint.getAddress();
            if (!endpointAddress.equals(currentAddress)) {
                RepositoryJaxrsService.removeEndpoint(currentAddress);
                jaxrsEndpoint.address(endpointAddress);
                RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);
            }
        }
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        if (endpointAddress == null) {
            throw new IllegalStateException(String.format("%s requires a hippo:moduleconfig", getClass().getSimpleName()));
        }
        final ManagedUserSessionInvoker managedUserSessionInvoker = new ManagedUserSessionInvoker(session);
        jaxrsEndpoint = new CXFRepositoryJaxrsEndpoint(endpointAddress)
                .invoker(managedUserSessionInvoker)
                .singleton(getRestResource(managedUserSessionInvoker))
                .singleton(createJacksonJsonProvider());
        RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);

        final ObservationManager observationManager = session.getWorkspace().getObservationManager();
        listeners.forEach(listener -> listener.attach(observationManager));
    }

    protected abstract Object getRestResource(final SessionRequestContextProvider sessionRequestContextProvider);

    @Override
    protected void doShutdown() {
        try {
            final ObservationManager observationManager = session.getWorkspace().getObservationManager();
            listeners.forEach(listener -> listener.detach(observationManager));
        } catch (final RepositoryException ignore) {
            log.info("Failed to retrieve observation manager, can not detach event listeners.");
        }

        if (jaxrsEndpoint != null) {
            RepositoryJaxrsService.removeEndpoint(jaxrsEndpoint.getAddress());
        }
        jaxrsEndpoint = null;
    }

    protected void addEventListener(final JcrEventListener listener) {
        listeners.add(listener);
    }

    private JacksonJsonProvider createJacksonJsonProvider() {
        return new JacksonJsonProvider(createObjectMapper());
    }

    protected ObjectMapper createObjectMapper() {
       return new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
    }
}
