/*
 *  Copyright 2008 Hippo.
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

import org.hippoecm.repository.security.AAContext;

/**
 * A role a user has on a domain.
 */
public interface Role {
    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

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
     * @throws RoleNotFoundException
     */
    public void init(AAContext context, String roleId) throws RoleNotFoundException;

    /**
     * Get the unique role id
     * @return the unique role id string
     * @throws RoleNotFoundException
     */
    public String getRoleId() throws RoleNotFoundException;

    /**
     * Get the JCR permissions assigned to the current role
     * @return
     * @throws RoleNotFoundException
     */
    public int getJCRPermissions() throws RoleNotFoundException;
}
