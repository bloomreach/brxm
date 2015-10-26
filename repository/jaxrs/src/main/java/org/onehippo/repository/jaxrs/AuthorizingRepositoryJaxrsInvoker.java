/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.repository.jaxrs;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.message.Exchange;

/**
 * Invoker that uses repository authorization to validate the authenticated user has sufficient permissions to call the
 * application endpoint. To use this invoker, call {@link RepositoryJaxrsEndpoint#authorized(String, String)} when
 * creating the application endpoint or manually create and set this invoker using
 * {@link CXFRepositoryJaxrsEndpoint#invoker(JAXRSInvoker)}.
 *
 * <p>For more detailed documentation and example usage, see the
 * <a href="http://www.onehippo.org/library/concepts/hippo-services/repository-jaxrs-service.html">online
 * documentation</a>.</p>
 */
public class AuthorizingRepositoryJaxrsInvoker extends AuthenticatingRepositoryJaxrsInvoker {

    private String securedNodePath;
    private String requiredPermission;

    public AuthorizingRepositoryJaxrsInvoker(String securedNodePath, String requiredPermission) {
        this.securedNodePath = securedNodePath;
        this.requiredPermission = requiredPermission;
    }

    @Override
    protected void checkAuthorized(final Exchange exchange, final Object requestParams, final Object resourceObject,
                                   final Session session) throws RepositoryException {
        session.checkPermission(securedNodePath, requiredPermission);
    }
}
