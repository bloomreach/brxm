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
 * Base interface for {@link Role} and {@link UserRole}
 */
public interface AbstractRole {
    /**
     * The name of the role
     * @return The name of the role
     */
    String getName();

    /**
     * The description of the role
     * @return the description of the role
     */
    String getDescription();

    /**
     * Indicator if the role is used or reserved for system purposes.
     * @return true if this is a system role, false otherwise
     */
    boolean isSystem();

    /**
     * The same of role names which are implied (included or merged) with this role.
     * @return the set of other role names to implied by this role.
     */
    Set<String> getRoles();
}
