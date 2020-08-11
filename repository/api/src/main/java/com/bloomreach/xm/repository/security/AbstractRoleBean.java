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

import java.util.HashSet;

/**
 * Simple (abstract) POJO bean implementation of {@link AbstractRole}
 */
public abstract class AbstractRoleBean implements AbstractRole {

    private String name;
    private String description;
    private boolean system;
    private final HashSet<String> roles = new HashSet<>();

    public AbstractRoleBean() {
    }

    public AbstractRoleBean(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    public AbstractRoleBean(final AbstractRole role) {
        this.name = role.getName();
        this.description = role.getDescription();
        this.system = role.isSystem();
        this.roles.addAll(role.getRoles());
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public boolean isSystem() {
        return system;
    }

    public void setSystem(final boolean system) {
        this.system = system;
    }

    @Override
    public HashSet<String> getRoles() {
        return roles;
    }
}
