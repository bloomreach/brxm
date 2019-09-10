/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.domain;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;

import com.google.common.collect.ImmutableSet;

/**
 * A auth role defines which users and/or groups and/or user role have a specific role
 * in a domain.
 */
public class AuthRole {

    /**
     * The AuthRole name (identifier within the containing Domain)
     */
    private final String name;

    /**
     * The role id
     */
    private final String role;

    /**
     * The userRole id
     */
    private final String userRole;

    /**
     * A set holding the group ids belonging to the auth role
     */
    private final Set<String> groups;

    /**
     * A set holding the user ids belonging to the auth role
     */
    private final Set<String> users;

    /**
     * Initialize the auth role from the configuration node. The initialization
     * does not check if roles, userRole, users and groups actually exist.
     * @param node the node holding the configuration
     * @throws RepositoryException -
     */
    public AuthRole(final Node node) throws RepositoryException {
        if (node == null) {
            throw new IllegalArgumentException("AuthRole node cannot be null");
        }
        name = NodeNameCodec.decode(node.getName());
        role = node.getProperty(HippoNodeType.HIPPO_ROLE).getString();
        userRole = JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_USERROLE, null);
        users = ImmutableSet.copyOf(JcrUtils.getMultipleStringProperty(node, HippoNodeType.HIPPO_USERS, new String[0]));
        groups = ImmutableSet.copyOf(JcrUtils.getMultipleStringProperty(node, HippoNodeType.HIPPO_GROUPS, new String[0]));
    }

    /**
     * Check if a user with the given user id has the
     * @param userId the id of the user
     * @return true if the user has the role
     */
    public boolean hasUser(String userId) {
        return users.contains(userId);
    }

    /**
     * Check if a group with the given group id has the role
     * @param groupId the group id
     * @return true if the group has the role
     */
    public boolean hasGroup(String groupId) {
        return groups.contains(groupId);
    }

    /**
     * Get the identifying name (within the domain) for the AuthRole
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Get the role id
     * @return the role id
     */
    public String getRole() {
        return role;
    }

    /**
     * Get the userRole id for the role
     * @return the userRole, may be null
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Get the set of user ids for the role
     * @return the set of user ids
     */
    public Set<String> getUsers() {
        return users;
    }

    /**
     * Get the set of group ids for the role
     * @return the set of group ids
     */
    public Set<String> getGroups() {
        return groups;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        boolean first = true;
        StringBuffer sb = new StringBuffer();
        Set<String> us = getUsers();
        Set<String> gs = getGroups();
        sb.append("AuthRole : ");
        sb.append(name);
        sb.append(" [Role: ");
        sb.append(role);
        sb.append("]");
        if (userRole != null) {
            sb.append(" UserRole: ");
            sb.append(userRole);
        }
        sb.append(" Users: [");
        for (String name : us) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(name);
        }
        sb.append("] ");
        first = true;
        sb.append(" Groups: [");
        for (String name : gs) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(name);
        }
        sb.append("]\r\n");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        return obj instanceof AuthRole && getName().equals(((AuthRole)obj).getName());
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return getName().hashCode();
    }
}
