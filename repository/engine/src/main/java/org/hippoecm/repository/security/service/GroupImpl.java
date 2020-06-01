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
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.user.HippoUserManager;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GroupImpl implements Group {

    private static final Logger log = LoggerFactory.getLogger(GroupImpl.class);

    private static final String HIPPOSYS_DESCRIPTION = "hipposys:description";

    private final String id;
    private final SecurityServiceImpl securityService;
    private final Node node;

    GroupImpl(final Node node, final SecurityServiceImpl securityService) throws RepositoryException {
        this.node = node;
        this.id = node.getName();
        this.securityService = securityService;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Iterable<User> getMembers() throws RepositoryException {
        return new Iterable<User>() {
            @Override
            public Iterator<User> iterator() {
                try {
                    return new Iterator<User>() {

                        private Iterator<String> membersIterator = getInternalGroupManager().getMembers(node).iterator();
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
                            while (next == null && membersIterator.hasNext()) {
                                final String nextMember = membersIterator.next();
                                try {
                                    final Node nextUser = getInternalUserManager().getUser(nextMember);
                                    if (nextUser != null) {
                                        next = new UserImpl(nextUser, securityService);
                                    }
                                } catch (RepositoryException e) {
                                    log.warn("Failed to load next member of group: " + e);
                                }
                            }
                        }
                    };
                } catch (RepositoryException e) {
                    log.error("Failed to initialize group members iterator: " + e);
                }
                return Collections.<User>emptyList().iterator();
            }
        };
    }

    @Override
    public String getDescription() throws RepositoryException {
        return JcrUtils.getStringProperty(node, HIPPOSYS_DESCRIPTION, null);
    }

    @Override
    public boolean isSystemGroup() throws RepositoryException {
        return JcrUtils.getBooleanProperty(node, HippoNodeType.HIPPO_SYSTEM, false);
    }

    @Override
    public String getProperty(final String propertyName) throws RepositoryException {
        return JcrUtils.getStringProperty(node, propertyName, null);
    }

    private String getProviderId() throws RepositoryException {
        return JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_SECURITYPROVIDER, null);
    }

    private GroupManager getInternalGroupManager() {
        return securityService.getInternalGroupManager();
    }

    private HippoUserManager getInternalUserManager() {
        return securityService.getInternalUserManager();
    }

    private GroupManager getGroupManager() throws RepositoryException {
        return securityService.getGroupManager(getProviderId());
    }

    @Override
    public String toString() {
        return "Group: " + getId();
    }

}
