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
package org.hippoecm.repository.security.user;

import java.security.Principal;
import java.util.Set;

import org.hippoecm.repository.security.AAContext;
import org.hippoecm.repository.security.group.Group;
import org.hippoecm.repository.security.role.Role;

public interface User {
    
    public void init(AAContext context, String UserId) throws UserNotFoundException;;
    
    public String getUserID() throws UserNotFoundException;
    
    public Set<Group> getMemberships() throws UserNotFoundException;

    public Set<Role> getRoles() throws UserNotFoundException;
    
    public Set<Principal> getPrincipals() throws UserNotFoundException;

}
