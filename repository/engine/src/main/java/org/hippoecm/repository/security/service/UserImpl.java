/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.service;

import java.util.Set;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.user.AbstractUserManager;
import org.onehippo.repository.security.User;

public class UserImpl implements User {

    private final String id;
    private final AbstractUserManager userManager;
    private final GroupManager groupManager;

    public UserImpl(final String id, final AbstractUserManager userManager, final GroupManager groupManager) {
        this.id = id;
        this.userManager = userManager;
        this.groupManager = groupManager;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isSystemUser() throws RepositoryException {
        return userManager.isSystemUser(id);
    }

    @Override
    public boolean isActive() throws RepositoryException {
        return userManager.isActive(id);
    }

    @Override
    public Set<String> getMemberships() throws RepositoryException {
        return groupManager.getMemberships(id);
    }

}
