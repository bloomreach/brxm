/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.security.group;

import java.security.Principal;
import java.util.Set;

import org.hippoecm.repository.security.AAContext;
import org.hippoecm.repository.security.role.Role;
import org.hippoecm.repository.security.user.User;

public interface Group {


    //------------------------< Interface Impl >--------------------------//
    public void init(AAContext context, String GroupId) throws GroupNotFoundException;

    public String getGroupId() throws GroupNotFoundException;

    public Set<User> getMembers() throws GroupNotFoundException;

    public Set<Role> getRoles() throws GroupNotFoundException;

    public Set<Principal> getPrincipals() throws GroupNotFoundException;

}
