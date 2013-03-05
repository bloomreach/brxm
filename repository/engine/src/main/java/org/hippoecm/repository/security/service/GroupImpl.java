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
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.User;

public class GroupImpl implements Group {

    private final String id;
    private final SecurityServiceImpl securityService;
    private Node node;

    public GroupImpl(final String id, final SecurityServiceImpl securityService) {
        this.id = id;
        this.securityService = securityService;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Iterable<User> getMembers() throws RepositoryException {
        return new Iterable<User>() {
            private final Iterator<String> membersIterator = getInternalGroupManager().getMembers(getNode()).iterator();
            @Override
            public Iterator<User> iterator() {
                return new Iterator<User>() {
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
                            next = new UserImpl(membersIterator.next(), securityService);
                        }
                    }
                };
            }
        };
    }

    @Override
    public String getDescription() throws RepositoryException {
        return JcrUtils.getStringProperty(getNode(), "hipposys:description", null);
    }

    @Override
    public boolean isSystemGroup() throws RepositoryException {
        return JcrUtils.getBooleanProperty(getNode(), HippoNodeType.HIPPO_SYSTEM, false);
    }

    private String getProviderId() throws RepositoryException {
        return JcrUtils.getStringProperty(getNode(), HippoNodeType.HIPPO_SECURITYPROVIDER, null);
    }

    private Node getNode() throws RepositoryException {
        if (node == null) {
            node = getInternalGroupManager().getGroup(id);
        }
        return node;
    }

    private GroupManager getInternalGroupManager() {
        return securityService.getInternalGroupManager();
    }

    private GroupManager getGroupManager() throws RepositoryException {
        return securityService.getGroupManager(getProviderId());
    }

}
