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
package com.bloomreach.xm.repository.security;

import java.util.TreeSet;

/**
 * Simple POJO bean implementation of {@link AuthRole}
 */
public class AuthRoleBean implements AuthRole {

    private final String name;
    private final String domainPath;
    private final String role;
    private String description;
    private String userRole;
    private final TreeSet<String> groups = new TreeSet<>();
    private final TreeSet<String> users = new TreeSet<>();

    public AuthRoleBean(final String name, final String domainPath, final String role) {
        if (name == null || name.equals("") || name.contains("/")) {
            throw new IllegalArgumentException("name is required, must not be empty and must not contain a /");
        }
        if (domainPath == null || !domainPath.startsWith("/") || domainPath.endsWith("/")) {
            throw new IllegalArgumentException("domainPath is required, must start with a / and must not end with a /");
        }
        if (role == null || role.equals("")) {
            throw new IllegalArgumentException("role is required and must not be empty");
        }
        this.name = name;
        this.domainPath = domainPath;
        this.role = role;
    }

    public AuthRoleBean(final AuthRole other) {
        this(other.getName(), other.getDomainPath(), other.getRole());
        description = other.getDescription();
        userRole = other.getUserRole();
        groups.addAll(other.getGroups());
        users.addAll(other.getUsers());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDomainPath() {
        return domainPath;
    }

    @Override
    public String getPath() {
        return domainPath + "/" + name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getRole() {
        return role;
    }

    @Override
    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(final String userRole) {
        this.userRole = userRole;
    }

    @Override
    public TreeSet<String> getGroups() {
        return groups;
    }

    @Override
    public TreeSet<String> getUsers() {
        return users;
    }

    @Override
    public int compareTo(final AuthRole o) {
        return o.getPath().compareTo(getPath());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AuthRole && getPath().equals(((AuthRole)obj).getPath());
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }
}
