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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.NotSupportedException;

import org.hippoecm.repository.security.ManagerContext;

public class DummyRoleManager implements RoleManager {


    public boolean addRole(Role role) throws NotSupportedException, RoleException {
        throw new NotSupportedException("Dummy manager");
    }

    public boolean deleteRole(Role role) throws NotSupportedException, RoleException {
        throw new NotSupportedException("Dummy manager");
    }

    public void init(ManagerContext context) throws RoleException {
    }

    public boolean isInitialized() {
        return true;
    }

    public Set<Role> listRoles() throws RoleException {
        return Collections.unmodifiableSet(new HashSet<Role>(0));
    }

}
