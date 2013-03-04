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

import java.util.Iterator;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.security.HippoSecurityManager;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.user.AbstractUserManager;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SecurityServiceImpl implements SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityServiceImpl.class);

    private final AbstractUserManager userManager;
    private final GroupManager groupManager;

    public SecurityServiceImpl(HippoSecurityManager securityManager, Session session) throws RepositoryException {
        this.userManager = (AbstractUserManager) securityManager.getUserManager(session);
        this.groupManager = securityManager.getGroupManager(session);
    }

    @Override
    public boolean hasUser(final String userId) throws RepositoryException {
        return userManager.hasUser(userId);
    }

    @Override
    public User getUser(final String userId) throws RepositoryException {
        if (!userManager.hasUser(userId)) {
            throw new ItemNotFoundException("No such user: " + userId);
        }
        return new UserImpl(userId, userManager, groupManager);
    }

    @Override
    public Iterable<User> listUsers() throws RepositoryException {
        return new Iterable<User>() {
            final NodeIterator nodeIterator = userManager.listUsers();
            @Override
            public Iterator<User> iterator() {
                return new Iterator<User>() {
                    @Override
                    public boolean hasNext() {
                        return nodeIterator.hasNext();
                    }

                    @Override
                    public User next() {
                        final Node node = nodeIterator.nextNode();
                        try {
                            return new UserImpl(node.getName(), userManager, groupManager);
                        } catch (RepositoryException e) {
                            log.warn("Failed to load next user in iterator: " + e);
                            return null;
                        }
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public boolean hasGroup(final String groupId) throws RepositoryException {
        return groupManager.hasGroup(groupId);
    }

    @Override
    public Group getGroup(final String groupId) throws RepositoryException {
        if (!groupManager.hasGroup(groupId)) {
            throw new ItemNotFoundException("No such group: " + groupId);
        }
        return new GroupImpl(groupId, groupManager, userManager);
    }

}
