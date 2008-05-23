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

import java.util.Set;

import javax.transaction.NotSupportedException;

import org.hippoecm.repository.security.AAContext;

public interface Group {
    /**
     * Initialize the group and (optionally) set the members
     * @param context the current AAContext
     * @param groupId initialize the group with the given groupId
     * @throws GroupException
     */
    public void init(AAContext context, String groupId) throws GroupException;

    /**
     * Get the unique group id
     * @return String
     * @throws GroupException
     */
    public String getGroupId() throws GroupException;

    /**
     * Get the members for this group as a list of UserId's
     * @return
     * @throws GroupException
     */
    public Set<String> listMemebers() throws GroupException;
    
    /**
     * Check if a user is a member of the group
     * @param userId the userId
     * @return true if the user is a memeber
     * @throws GroupException
     */
    public boolean isMemeber(String userId) throws GroupException;
    
    /**
     * 
     * @param userId
     * @return
     * @throws NotSupportedException
     * @throws GroupException
     */
    public boolean addMember(String userId) throws NotSupportedException, GroupException;
    
    /**
     * 
     * @param userId
     * @return
     * @throws NotSupportedException
     * @throws GroupException
     */
    public boolean deleteMember(String userId) throws NotSupportedException, GroupException;
}
