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

import org.hippoecm.repository.security.ManagerContext;

/**
 * A role a user has on a domain.
 */
public interface Role {

    /**
     * NONE JCR permission constant
     */
    static final int NONE = 0;

    /**
     * READ JCR permission constant
     */
    static final int READ = 1;

    /**
     * WRITE JCR permission constant
     */
    static final int WRITE = 2;

    /**
     * REMOVE JCR permission constant
     */
    static final int REMOVE = 4;

    /**
     * Initialize the role from the backend
     * @param context the context containing params needed by the backend
     * @param roleId the unique role id
     * @throws RoleException
     */
    public void init(ManagerContext context, String roleId) throws RoleException;

    /**
     * Get the unique role id
     * @return the unique role id string
     * @throws RoleException
     */
    public String getRoleId() throws RoleException;

    /**
     * Get the JCR permissions assigned to the current role
     * @return
     * @throws RoleException
     */
    public int getJCRPermissions() throws RoleException;
    /**
     * Helper method for pretty printing the requested permission
     * @param permissions
     * @return the 'unix' style permissions string
     */
}
