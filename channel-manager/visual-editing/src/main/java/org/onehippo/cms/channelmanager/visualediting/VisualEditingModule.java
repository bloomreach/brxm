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

package org.onehippo.cms.channelmanager.visualediting;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;

public class VisualEditingModule extends AbstractReconfigurableDaemonModule {

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
            throw new IllegalStateException("VisualEditingModule requires a hippo:moduleconfig");
        }
        // TODO: add authorization
        jaxrsEndpoint = new RepositoryJaxrsEndpoint(endpointAddress)
                .rootClass(VisualEditingResource.class)
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
