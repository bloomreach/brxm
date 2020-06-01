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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.security.HippoSecurityManager;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.user.HippoUserManager;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class SecurityServiceImpl implements SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityServiceImpl.class);
    private static final String INTERNAL_PROVIDER = "internal";

    private final Session session;
    private final HippoSecurityManager securityManager;
    private final HippoUserManager internalUserManager;
    private final GroupManager internalGroupManager;

    private final Map<String, HippoUserManager> userManagers = new HashMap<String, HippoUserManager>(2);
    private final Map<String, GroupManager> groupManagers = new HashMap<String, GroupManager>(2);

    public SecurityServiceImpl(HippoSecurityManager securityManager, Session session) throws RepositoryException {
        this.securityManager = securityManager;
        this.session = session;
        this.internalUserManager = securityManager.getUserManager(session, INTERNAL_PROVIDER);
        this.internalGroupManager = securityManager.getGroupManager(session, INTERNAL_PROVIDER);
    }

    @Override
    public boolean hasUser(final String userId) throws RepositoryException {
        return internalUserManager.hasUser(userId);
    }

    @Override
    public User getUser(final String userId) throws RepositoryException {
        final Node node = internalUserManager.getUser(userId);
        if (node == null) {
            throw new ItemNotFoundException("No such user: " + userId);
        }
        return new UserImpl(node, this);
    }

    @Override
    public Iterable<User> getUsers(final long offset, final long limit) throws RepositoryException {
        return new Iterable<User>() {
            @Override
            public Iterator<User> iterator() {
                try {
                    return new Iterator<User>() {

                        private final NodeIterator nodeIterator = internalUserManager.listUsers(offset, limit);
                        private User next;

                        @Override
                        public boolean hasNext() {
                            fetchNext();
                            return next != null;
                        }

                        @Override
                        public User next() {
                            fetchNext();
                            if (next == null) {
                                throw new NoSuchElementException();
                            }
                            final User result = next;
                            next = null;
                            return result;
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }

                        private void fetchNext() {
                            while (next == null && nodeIterator.hasNext()) {
                                final Node node = nodeIterator.nextNode();
                                try {
                                    next = new UserImpl(node, SecurityServiceImpl.this);
                                } catch (RepositoryException e) {
                                    log.warn("Failed to load next user in iterator: " + e);
                                }
                            }
                        }
                    };
                } catch (RepositoryException e) {
                    log.error("Failed to initialize user iterator: " + e);
                }
                return Collections.<User>emptyList().iterator();
            }
        };
    }

    @Override
    public Iterable<Group> getGroups(final long offset, final long limit) throws RepositoryException {
        return new Iterable<Group>() {
            @Override
            public Iterator<Group> iterator() {
                try {
                    return new Iterator<Group>() {

                        private final NodeIterator nodeIterator = internalGroupManager.listGroups(offset, limit);
                        private Group next;

                        @Override
                        public boolean hasNext() {
                            fetchNext();
                            return next != null;
                        }

                        @Override
                        public Group next() {
                            fetchNext();
                            if (next == null) {
                                throw new NoSuchElementException();
                            }
                            final Group result = next;
                            next = null;
                            return result;
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }

                        private void fetchNext() {
                            while (next == null && nodeIterator.hasNext()) {
                                final Node node = nodeIterator.nextNode();
                                try {
                                    next = new GroupImpl(node, SecurityServiceImpl.this);
                                } catch (RepositoryException e) {
                                    log.warn("Failed to load next group in iterator: " + e);
                                }
                            }
                        }
                    };
                } catch (RepositoryException e) {
                    log.error("Failed to initialize group iterator: " + e);
                }
                return Collections.<Group>emptyList().iterator();
            }
        };
    }

    @Override
    public boolean hasGroup(final String groupId) throws RepositoryException {
        return internalGroupManager.hasGroup(groupId);
    }

    @Override
    public Group getGroup(final String groupId) throws RepositoryException {
        final Node node = internalGroupManager.getGroup(groupId);
        if (node == null) {
            throw new ItemNotFoundException("No such group: " + groupId);
        }
        return new GroupImpl(node, this);
    }

    GroupManager getInternalGroupManager() {
        return internalGroupManager;
    }

    HippoUserManager getInternalUserManager() {
        return internalUserManager;
    }

    GroupManager getGroupManager(final String providerId) throws RepositoryException {
        if (providerId == null || providerId.equals(INTERNAL_PROVIDER)) {
            return internalGroupManager;
        }
        if (groupManagers.containsKey(providerId)) {
            return groupManagers.get(providerId);
        }
        final GroupManager groupManager = securityManager.getGroupManager(session, providerId);
        if (groupManager != null) {
            groupManagers.put(providerId, groupManager);
        }
        return groupManager;
    }

    HippoUserManager getUserManager(final String providerId) throws RepositoryException {
        if (providerId == null || providerId.equals(INTERNAL_PROVIDER)) {
            return internalUserManager;
        }
        if (userManagers.containsKey(providerId)) {
            return userManagers.get(providerId);
        }
        final HippoUserManager userManager = securityManager.getUserManager(session, providerId);
        if (userManager != null) {
            userManagers.put(providerId, userManager);
        }
        return userManager;
    }

}
