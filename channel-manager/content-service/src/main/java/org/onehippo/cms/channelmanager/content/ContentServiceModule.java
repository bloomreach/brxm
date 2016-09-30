/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.jaxrs.CXFRepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;

/**
 * ContentServiceModule registers and manages a JAX-RS endpoint of the repository module.
 *
 * That endpoint represents the REST resource {@link ContentResource} and the resource's
 * root address (configurable, but defaulting to "content"), and it registers the
 * {@link ManagedUserSessionInvoker} to take care of authentication and authorization.
 */
public class ContentServiceModule extends AbstractReconfigurableDaemonModule {

    private static final String ENDPOINT_ADDRESS = "jaxrs.endpoint.address";

    private String endpointAddress;
    private RepositoryJaxrsEndpoint jaxrsEndpoint;

    @Override
    protected synchronized void doConfigure(final Node moduleConfig) throws RepositoryException {
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
    protected void doInitialize(final Session session) throws RepositoryException {
        if (endpointAddress == null) {
            throw new IllegalStateException("ContentServiceModule requires a hippo:moduleconfig");
        }
        final ManagedUserSessionInvoker managedUserSessionInvoker = new ManagedUserSessionInvoker(session);
        jaxrsEndpoint = new CXFRepositoryJaxrsEndpoint(endpointAddress)
                .invoker(managedUserSessionInvoker)
                .singleton(new ContentResource(managedUserSessionInvoker))
                .singleton(new JacksonJsonProvider());
        RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);
    }

    @Override
    protected void doShutdown() {
        if (jaxrsEndpoint != null) {
            RepositoryJaxrsService.removeEndpoint(jaxrsEndpoint.getAddress());
        }
        jaxrsEndpoint = null;
    }
}
