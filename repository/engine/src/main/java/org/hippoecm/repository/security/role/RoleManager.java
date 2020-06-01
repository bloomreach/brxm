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
package org.hippoecm.repository.security.role;

import java.util.Set;

import javax.transaction.NotSupportedException;

import org.hippoecm.repository.security.ManagerContext;

/**
 * Interface for managing roles in the backend
 */
public interface RoleManager {

    /**
     * Initialize the backend with the given context and load the roles
     * @param context the context with the params needed by the backend
     * @throws RoleException
     * @see ManagerContext
     */
    public void init(ManagerContext context) throws RoleException;

    /**
     * Check if the manager is configured and initialized
     * @return true if initialized otherwise false
     */
    public boolean isInitialized();

    /**
     * List the roles in the backend
     * @return the Set of Roles
     * @throws RoleException
     * @see Role
     */
    public Set<Role> listRoles() throws RoleException;

    /**
     * Add a role to the backend
     * @param Role the role
     * @return true if the role is successful added in the backend
     * @throws NotSupportedException
     * @throws RoleException
     */
    public boolean addRole(Role role) throws NotSupportedException, RoleException;

    /**
     * Delete a role from the backend
     * @param Role the role to be deleted
     * @return true if the group is successful removed in the backend
     * @throws NotSupportedException
     * @throws RoleException
     */
    public boolean deleteRole(Role role) throws NotSupportedException, RoleException;

}
