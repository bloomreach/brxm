/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.role.RoleManager;

/**
 * Delegating to the internal <code>SecurityProvider</code>.
 * <P>
 * This class can be used to decorate an existing security provider to build a custom security provider easier.
 * </P>
 */
public class DelegatingSecurityProvider implements SecurityProvider {

    private final SecurityProvider delegatee;

    public DelegatingSecurityProvider(final SecurityProvider delegatee) {
        this.delegatee = delegatee;
    }

    @Override
    public void init(SecurityProviderContext securityProviderContext) throws RepositoryException {
        delegatee.init(securityProviderContext);
    }

    @Override
    public void remove() {
        delegatee.remove();
    }

    @Override
    public UserManager getUserManager() throws RepositoryException {
        return delegatee.getUserManager();
    }

    @Override
    public GroupManager getGroupManager() throws RepositoryException {
        return delegatee.getGroupManager();
    }

    @Override
    public RoleManager getRoleManager() throws RepositoryException {
        return delegatee.getRoleManager();
    }

    @Override
    public UserManager getUserManager(Session session) throws RepositoryException {
        return delegatee.getUserManager(session);
    }

    @Override
    public GroupManager getGroupManager(Session session) throws RepositoryException {
        return delegatee.getGroupManager(session);
    }

}