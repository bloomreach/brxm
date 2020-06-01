/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.bloomreach.xm.repository.security.impl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import com.bloomreach.xm.repository.security.RepositorySecurityProviders;
import com.bloomreach.xm.repository.security.RolesProvider;
import com.bloomreach.xm.repository.security.UserRolesProvider;

import static org.onehippo.repository.security.SecurityConstants.CONFIG_ROLES_PATH;
import static org.onehippo.repository.security.SecurityConstants.CONFIG_USERROLES_PATH;

public class RepositorySecurityProvidersImpl implements RepositorySecurityProviders {

    private final RolesProviderImpl rolesProvider;
    private final UserRolesProviderImpl userRolesProvider;
    private final Session systemSession;

    private boolean closed;

    public RepositorySecurityProvidersImpl(final Session systemSession) throws RepositoryException {
        this.systemSession = systemSession.impersonate(new SimpleCredentials("system", new char[0]));
        rolesProvider = new RolesProviderImpl(this.systemSession, CONFIG_ROLES_PATH);
        userRolesProvider = new UserRolesProviderImpl(this.systemSession, CONFIG_USERROLES_PATH);
    }

    @Override
    public RolesProvider getRolesProvider() {
        return rolesProvider;
    }

    @Override
    public UserRolesProvider getUserRolesProvider() {
        return userRolesProvider;
    }

    public void close() {
        if (!closed) {
            rolesProvider.close();
            userRolesProvider.close();
            if (systemSession.isLive()) {
                systemSession.logout();
            }
            closed = true;
        }
    }
}
