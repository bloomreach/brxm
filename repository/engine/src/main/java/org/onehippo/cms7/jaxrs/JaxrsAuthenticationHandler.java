/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.jaxrs;

import java.io.IOException;
import java.security.AccessControlException;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxrsAuthenticationHandler implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JaxrsAuthenticationHandler.class);

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        final Message message = PhaseInterceptorChain.getCurrentMessage();
        final AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);
        if (policy == null) {
            requestContext.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        } else {
            Session session = null;
            try {
                final RepositoryService repository = HippoServiceRegistry.getService(RepositoryService.class);
                session = repository.login(new SimpleCredentials(policy.getUserName(), policy.getPassword().toCharArray()));
//                session.checkPermission("/content/document", "hippo:rest");
            } catch (AccessControlException | LoginException e) {
                requestContext.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
            } catch (RepositoryException e) {
                log.error("Error during login", e);
                requestContext.abortWith(Response.serverError().build());
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }
    }

}
