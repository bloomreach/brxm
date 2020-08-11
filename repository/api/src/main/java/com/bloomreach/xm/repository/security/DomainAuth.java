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

import java.util.SortedMap;

/**
 * A model object interface for a hipposys:domain node and (only) its hipposys:authrole children,
 * {@link Comparable} on its {@link #getPath()}
 */
public interface DomainAuth extends Comparable<DomainAuth> {

    /**
     * @return de domain (node) name
     */
    String getName();

    /**
     * @return de domain (node) path
     */
    String getPath();

    /**
     * @return de parent (domainfolder) node path
     */
    String getFolderPath();

    /**
     * @return the description of the authrole
     */
    String getDescription();

    /**
     * @return the map of {@link AuthRole} children, keyed by their (node) name
     */
    SortedMap<String, AuthRole> getAuthRolesMap();

    /**
     * Convenient method for getting a {@link AuthRole} child by name
     * @param authRoleName the name of the {@link AuthRole} to get
     * @return the authrole with the provided name, or null not a child
     */
    AuthRole getAuthRole(final String authRoleName);
}
