/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package com.bloomreach.xm.repository.security.impl;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;

import com.bloomreach.xm.repository.security.AuthRole;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_DESCRIPTION;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_GROUPS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_ROLE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERS;

public class AuthRoleImpl implements AuthRole {

    private final String name;
    private final String path;
    private final String domainPath;
    private final String role;
    private final String description;
    private final String userRole;
    private final SortedSet<String> groups;
    private final SortedSet<String> users;

    public AuthRoleImpl(final Node authRoleNode) throws RepositoryException {
        this.name = NodeNameCodec.decode(authRoleNode.getName());
        this.path = authRoleNode.getPath();
        this.domainPath = authRoleNode.getParent().getPath();
        this.role = authRoleNode.getProperty(HIPPO_ROLE).getString();
        this.description = JcrUtils.getStringProperty(authRoleNode, HIPPOSYS_DESCRIPTION, null);
        this.userRole = JcrUtils.getStringProperty(authRoleNode, HIPPO_USERROLE, null);
        this.groups = Collections.unmodifiableSortedSet(new TreeSet<>(JcrUtils.getStringSetProperty(authRoleNode,
                HIPPO_GROUPS, Collections.emptySet())));
        this.users = Collections.unmodifiableSortedSet(new TreeSet<>(JcrUtils.getStringSetProperty(authRoleNode,
                HIPPO_USERS, Collections.emptySet())));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getDomainPath() {
        return domainPath;
    }

    @Override
    public String getRole() {
        return role;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getUserRole() {
        return userRole;
    }

    @Override
    public SortedSet<String> getGroups() {
        return groups;
    }

    @Override
    public SortedSet<String> getUsers() {
        return users;
    }

    @Override
    public int compareTo(final AuthRole o) {
        return o.getPath().compareTo(path);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AuthRole && path.equals(((AuthRole)obj).getPath());
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
