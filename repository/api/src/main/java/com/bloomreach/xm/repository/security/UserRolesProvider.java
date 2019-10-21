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

import java.util.Set;

/**
 * Provider for userroles
 */
public interface UserRolesProvider {

    Set<UserRole> getRoles();

    boolean hasRole(final String userRoleName);
    UserRole getRole(final String userRoleName);

    Set<UserRole> resolveRoles(final String userRoleName);
    Set<UserRole> resolveRoles(final Iterable<String> userRoleNames);

    /**
     * Resolve the names of the user roles representing and implied by a role name.
     * @param userRoleName userrole name
     * @return the set of resolved userrole names, empty if none found
     */
    Set<String> resolveRoleNames(final String userRoleName);

    /**
     * Resolve the set of userrole names representing and implied by a iterable collection of userrole names.
     * @param userRoleNames iterable collection of userrole names
     * @return the set of resolved userrole names, empty if none found
     */
    Set<String> resolveRoleNames(final Iterable<String> userRoleNames);
}
