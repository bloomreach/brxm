/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.jackrabbit.api.security.user.UserManager;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.group.RepositoryGroupManager;
import org.hippoecm.repository.security.user.AbstractUserManager;
import org.hippoecm.repository.security.user.RepositoryUserManager;

public class RepositorySecurityProvider extends AbstractSecurityProvider {

    private SecurityProviderContext context;

    public void init(SecurityProviderContext context) throws RepositoryException {
        this.context = context;

        ManagerContext mgrContext;

        mgrContext = new ManagerContext(context.getSession(), context.getProviderPath(), context.getUsersPath(), context.isMaintenanceMode());
        userManager = new RepositoryUserManager();
        ((AbstractUserManager)userManager).init(mgrContext);

        mgrContext = new ManagerContext(context.getSession(), context.getProviderPath(), context.getGroupsPath(), context.isMaintenanceMode());
        groupManager = new RepositoryGroupManager();
        groupManager.init(mgrContext);
    }

    @Override
    public UserManager getUserManager(final Session session) throws RepositoryException {
        final ManagerContext mgrContext = new ManagerContext(session, context.getProviderPath(),
                context.getUsersPath(), context.isMaintenanceMode());
        final RepositoryUserManager userManager = new RepositoryUserManager();
        userManager.init(mgrContext);
        return userManager;
    }

    @Override
    public GroupManager getGroupManager(final Session session) throws RepositoryException {
        final ManagerContext mgrContext = new ManagerContext(session, context.getProviderPath(),
                context.getGroupsPath(), context.isMaintenanceMode());
        final RepositoryGroupManager groupManager = new RepositoryGroupManager();
        groupManager.init(mgrContext);
        return groupManager;
    }
}
