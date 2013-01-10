/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNodeType;

/**
 * A auth role defines which users and groups have a specific role
 * in a domain. The roles are defined a in {@link Role}
 */
public class AuthRole {

    /** SVN id placeholder */

    /**
     * The role id
     * @see Role
     */
    private final String role;

    /**
     * A set holding the group ids belonging to the auth role
     */
    private Set<String> groups = new HashSet<String>();

    /**
     * A set holding the user ids belonging to the auth role
     */
    private Set<String> users = new HashSet<String>();

    /**
     * The hash code
     */
    private transient int hash;


    /**
     * Initialize the auth role from the configuration node. The initialization
     * does not check if roles, users and group do actually exist.
     * @param node the node holding the configuration
     * @throws RepositoryException
     */
    public AuthRole(final Node node) throws RepositoryException {
        if (node == null) {
            throw new IllegalArgumentException("AuthRole node cannot be null");
        }
        role = node.getProperty(HippoNodeType.HIPPO_ROLE).getString();
        if (node.hasProperty(HippoNodeType.HIPPO_USERS)) {
            Value[] values = node.getProperty(HippoNodeType.HIPPO_USERS).getValues();
            for (Value value : values) {
                users.add(value.getString());
            }
        }
        if (node.hasProperty(HippoNodeType.HIPPO_GROUPS)) {
            Value[] values = node.getProperty(HippoNodeType.HIPPO_GROUPS).getValues();
            for (Value value : values) {
                groups.add(value.getString());
            }
        }
    }

    /**
     * Check if a user with the given user id has the role
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
     * Get the role id belonging to the role
     * @return the role id
     */
    public String getRole() {
        return role;
    }

    /**
     * Get the set of user ids that have the current role
     * @return the set of user ids
     */
    public Set<String> getUsers() {
        return Collections.unmodifiableSet(users);
    }

    /**
     * Get the set of group ids that have the current role
     * @return the set of group ids
     */
    public Set<String> getGroups() {
        return Collections.unmodifiableSet(groups);
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
        sb.append(" [Role: ");
        sb.append(role);
        sb.append("]");
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
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AuthRole)) {
            return false;
        }
        AuthRole other = (AuthRole) obj;
        if (!role.equals(other.getRole())) {
            return false;
        }
        if (users.size() != other.getUsers().size() || !users.containsAll(other.getUsers())) {
            return false;
        }
        if (groups.size() != other.getGroups().size() || !groups.containsAll(other.getGroups())) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        if (hash == 0) {
            hash = this.toString().hashCode();
        }
        return hash;
    }
}
