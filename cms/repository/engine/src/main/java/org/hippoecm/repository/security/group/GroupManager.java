/*
 * Copyright 2008 Hippo
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

import java.util.Set;

import javax.transaction.NotSupportedException;

import org.hippoecm.repository.security.AAContext;

/**
 * Interface for managing groups in the backend
 */
public interface GroupManager {


    /**
     * Initialize the backend with the given context and load the groups
     * @param context the context with the params needed by the backend
     * @throws GroupException
     * @see AAContext
     */
    public void init(AAContext context) throws GroupException;


    /**
     * List the groups in the backend 
     * @return the Set of Groups
     * @throws GroupException
     * @see Group
     */
    public Set<Group> listGroups() throws GroupException;
    
    /**
     * List the groups of which the user with userId is a memeber
     * @param userId the userId of the user
     * @return A set with groups of which the user is a member
     * @throws GroupException
     */
    public Set<Group> listMemeberships(String userId) throws GroupException;

    /**
     * Add a group to the backend
     * @param group the group
     * @return true if the group is successful added in the backend
     * @throws NotSupportedException
     * @throws GroupException
     */
    public boolean addGroup(Group group) throws NotSupportedException, GroupException;

    /**
     * Delete a group from the backend
     * @param group
     * @return true if the group is successful removed in the backend
     * @throws NotSupportedException
     * @throws GroupException
     */
    public boolean deleteGroup(Group group) throws NotSupportedException, GroupException;

}
