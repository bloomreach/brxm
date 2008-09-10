/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.security;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * The context for the security providers for initilization
 *
 */
public class SecurityProviderContext {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final Session session;
    private final String providerId;
    private final String providerPath;
    private final String securityPath;
    private final String usersPath;
    private final String groupsPath;
    private final String rolesPath;
    private final String domainsPath;

    public SecurityProviderContext(Session session, String providerId, String securityPath, String usersPath,
            String groupsPath, String rolesPath, String domainsPath) throws RepositoryException {
        this.session = session;
        this.providerId = providerId;
        this.securityPath = securityPath;
        this.providerPath = securityPath + "/" + providerId;
        this.usersPath = usersPath;
        this.groupsPath = groupsPath;
        this.rolesPath = rolesPath;
        this.domainsPath = domainsPath;
    }

    public Session getSession() {
        return session;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getProviderPath() {
        return providerPath;
    }

    public String getSecurityPath() {
        return securityPath;
    }

    public String getUsersPath() {
        return usersPath;
    }

    public String getGroupsPath() {
        return groupsPath;
    }

    public String getRolesPath() {
        return rolesPath;
    }

    public String getDomainsPath() {
        return domainsPath;
    }
}
