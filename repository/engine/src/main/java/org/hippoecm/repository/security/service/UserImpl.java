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

import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.user.AbstractUserManager;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.User;

public class UserImpl implements User {

    private final String id;
    private final SecurityServiceImpl securityService;
    private Node node;

    public UserImpl(final String id, final SecurityServiceImpl securityService) {
        this.id = id;
        this.securityService = securityService;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isSystemUser() throws RepositoryException {
        return JcrUtils.getBooleanProperty(getNode(), HippoNodeType.HIPPO_SYSTEM, false);
    }

    @Override
    public boolean isActive() throws RepositoryException {
        return JcrUtils.getBooleanProperty(getNode(), HippoNodeType.HIPPO_ACTIVE, true);
    }

    @Override
    public String getFirstName() throws RepositoryException {
        return JcrUtils.getStringProperty(getNode(), "hipposys:firstname", null);
    }

    @Override
    public String getLastName() throws RepositoryException {
        return JcrUtils.getStringProperty(getNode(), "hipposys:lastname", null);
    }

    @Override
    public String getEmail() throws RepositoryException {
        return JcrUtils.getStringProperty(getNode(), "hipposys:email", null);
    }

    @Override
    public Calendar getLastLogin() throws RepositoryException {
        return JcrUtils.getDateProperty(getNode(), "hipposys:lastlogin", null);
    }

    @Override
    public Iterable<Group> getMemberships() throws RepositoryException {
        final Set<Group> memberships = new HashSet<Group>();
        for (String groupId : getInternalGroupManager().getMemberships(id)) {
            memberships.add(new GroupImpl(groupId, securityService));
        }
        return Collections.unmodifiableCollection(memberships);
    }

    private String getProviderId() throws RepositoryException {
        return JcrUtils.getStringProperty(getNode(), HippoNodeType.HIPPO_SECURITYPROVIDER, null);
    }

    private Node getNode() throws RepositoryException {
        if (node == null) {
            node = getInternalUserManager().getUser(id);
        }
        return node;
    }

    private AbstractUserManager getInternalUserManager() {
        return securityService.getInternalUserManager();
    }

    private AbstractUserManager getUserManager() throws RepositoryException {
        return securityService.getUserManager(getProviderId());
    }

    private GroupManager getInternalGroupManager() throws RepositoryException {
        return securityService.getInternalGroupManager();
    }

}
