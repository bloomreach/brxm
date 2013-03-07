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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.user.HippoUserManager;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.User;

public final class UserImpl implements User {

    private static final String HIPPOSYS_FIRSTNAME = "hipposys:firstname";
    private static final String HIPPOSYS_LASTNAME = "hipposys:lastname";
    private static final String HIPPOSYS_EMAIL = "hipposys:email";
    private static final String HIPPOSYS_LASTLOGIN = "hipposys:lastlogin";

    private final String id;
    private final SecurityServiceImpl securityService;
    private final Node node;

    UserImpl(final Node node, final SecurityServiceImpl securityService) throws RepositoryException {
        this.node = node;
        this.id = node.getName();
        this.securityService = securityService;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isSystemUser() throws RepositoryException {
        return JcrUtils.getBooleanProperty(node, HippoNodeType.HIPPO_SYSTEM, false);
    }

    @Override
    public boolean isActive() throws RepositoryException {
        return JcrUtils.getBooleanProperty(node, HippoNodeType.HIPPO_ACTIVE, true);
    }

    @Override
    public String getFirstName() throws RepositoryException {
        return JcrUtils.getStringProperty(node, HIPPOSYS_FIRSTNAME, null);
    }

    @Override
    public String getLastName() throws RepositoryException {
        return JcrUtils.getStringProperty(node, HIPPOSYS_LASTNAME, null);
    }

    @Override
    public String getEmail() throws RepositoryException {
        return JcrUtils.getStringProperty(node, HIPPOSYS_EMAIL, null);
    }

    @Override
    public Calendar getLastLogin() throws RepositoryException {
        return JcrUtils.getDateProperty(node, HIPPOSYS_LASTLOGIN, null);
    }

    @Override
    public String getProperty(final String propertyName) throws RepositoryException {
        return JcrUtils.getStringProperty(node, propertyName, null);
    }

    @Override
    public Iterable<Group> getMemberships() throws RepositoryException {
        final List<Group> memberships = new ArrayList<Group>();
        final NodeIterator nodes = getInternalGroupManager().getMemberships(id);
        while (nodes.hasNext()) {
            memberships.add(new GroupImpl(nodes.nextNode(), securityService));
        }
        return Collections.unmodifiableCollection(memberships);
    }

    private String getProviderId() throws RepositoryException {
        return JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_SECURITYPROVIDER, null);
    }

    private HippoUserManager getInternalUserManager() {
        return securityService.getInternalUserManager();
    }

    private HippoUserManager getUserManager() throws RepositoryException {
        return securityService.getUserManager(getProviderId());
    }

    private GroupManager getInternalGroupManager() throws RepositoryException {
        return securityService.getInternalGroupManager();
    }

    @Override
    public String toString() {
        return "User: " + getId();
    }
}
