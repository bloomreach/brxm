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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.user.AbstractUserManager;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.User;

public class GroupImpl implements Group {

    private final String id;
    private final GroupManager groupManager;
    private final AbstractUserManager userManager;

    public GroupImpl(final String id, final GroupManager groupManager, final AbstractUserManager userManager) {
        this.id = id;
        this.groupManager = groupManager;
        this.userManager = userManager;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Set<User> getMembers() throws RepositoryException {
        final Set<User> members = new HashSet<User>();
        for (String userId : groupManager.getMembers(groupManager.getGroup(id))) {
            members.add(new UserImpl(userId, userManager, groupManager));
        }
        return Collections.unmodifiableSet(members);
    }

}
