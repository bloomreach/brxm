/*
 *  Copyright 2013-2023 Bloomreach
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
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.user.HippoUserManager;
import org.onehippo.repository.InternalHippoRepository;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class SecurityServiceImpl implements SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityServiceImpl.class);
    private static final String INTERNAL_PROVIDER = "internal";

    private final InternalHippoSession systemSession;
    private final HippoUserManager internalUserManager;
    private final GroupManager internalGroupManager;

    public SecurityServiceImpl(final InternalHippoRepository repository) throws RepositoryException {
        this.systemSession = repository.createSystemSession();
        this.internalUserManager = repository.getHippoSecurityManager().getUserManager(systemSession, INTERNAL_PROVIDER);
        this.internalGroupManager = repository.getHippoSecurityManager().getGroupManager(systemSession, INTERNAL_PROVIDER);
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
        return new UserImpl(node, internalGroupManager);
    }

    @Override
    public Iterable<User> getUsers(final long offset, final long limit) {
        return () -> {
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
                                next = new UserImpl(node, internalGroupManager);
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
        };
    }

    @Override
    public Iterable<Group> getGroups(final long offset, final long limit) {
        return () -> {
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
                                next = new GroupImpl(node, internalGroupManager);
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
        return new GroupImpl(node, internalGroupManager);
    }

    public void close() {
        systemSession.logout();
    }
}
