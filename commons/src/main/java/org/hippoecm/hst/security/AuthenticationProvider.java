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
package org.hippoecm.hst.security;

import java.lang.SecurityException;
import java.lang.String;
import java.util.Set;

/**
 * AuthenticationProvider
 * <p>
 * Configures an authentication provider.
 * </p>
 */
public interface AuthenticationProvider {

    /**
     * Authenticate a user.
     * 
     * @param userName The user name.
     * @param password The user password.
     * @return the {@link User}
     */
    User authenticate(String userName, char [] password) throws SecurityException;
    
    /**
     * Returns security roles of the given username
     * @param username
     * @deprecated since v14, will be removed in v15+. Use {@link #getRolesByUser(User)} instead
     */
    @Deprecated
    Set<Role> getRolesByUsername(String username) throws SecurityException;

    /**
     * Returns security roles of the given user
     * <p>
     *     Note: this is a default method delegating to deprecated {@link #getRolesByUser(User)}
     *     which will become required to be implemented when that method is removed in v15+
     * </p>
     * @param user
     */
    default Set<Role> getRolesByUser(User user) throws SecurityException {
        return getRolesByUsername(user.getName());
    }
}
