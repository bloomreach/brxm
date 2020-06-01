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
package org.onehippo.repository.security;

import java.util.Set;

import org.hippoecm.repository.api.HippoSession;

/**
 * A SessionUser provides the <em>resolved</em> User Role names for a logged in user.
 */
public interface SessionUser extends User {

    /**
     * Get the resolved user role names assigned for this logged in user
     * <p>
     * The user role names are resolved (which may drop non-existing user role names), and includes possible implied
     * user roles names.
     * </p>
     * @see HippoSession#getUser()
     * @return the resolved user role names assigned to the logged in user
     */
    @Override
    Set<String> getUserRoles();
}
