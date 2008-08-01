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
package org.hippoecm.repository.security.group;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.transaction.NotSupportedException;

import org.hippoecm.repository.security.ManagerContext;

/**
 * Interface for managing groups in the backend
 */
public interface GroupManager {
    final static String SVN_ID = "$Id$";

    /**
     * Initialize the backend with the given context and load the groups
     * @param context the context with the params needed by the backend
     * @throws RepositoryException
     * @see ManagerContext
     */
    public void init(ManagerContext context) throws RepositoryException;

    /**
     * Check if the manager is configured and initialized
     * @return true if initialized otherwise false
     */
    public boolean isInitialized();

    /**
     * Initialization hook for the security managers. This method gets 
     * called after the init which is handled by the {@link AbstractUserManager}
     * @param context The {@link ManagerContext} with params for the backend
     * @throws RepositoryException
     * @See ManagerContext
     */
    public void initManager(ManagerContext context) throws RepositoryException;
    
    /**
     * List the groupIds currently managed by the backend
     * @return the Set of GroupIds
     * @throws RepositoryException
     */
    public Set<String> listGroups() throws RepositoryException;

    /**
     * Get the node for the group with the given groupId
     * @param groupId
     * @return the user node
     * @throws RepositoryException
     */
    public Node getGroupNode(String groupId) throws RepositoryException;

    /**
     * Create a (skeleton) node for the group in the repository
     * @param groupId
     * @param the nodeType for the group. This must be a derivative of hippo:group
     * @return the newly created user node
     * @throws RepositoryException
     */
    public Node createGroupNode(String groupId, String nodeType) throws RepositoryException;
    

    /**
     * Hook for the provider to sync from the backend with the repository.
     * @param userId
     */
    public void syncGroup(String groupId);
    
    /**
     * Add a group to the backend
     * @param groupId
     * @return true if the group is successful added in the backend
     * @throws NotSupportedException
     * @throws RepositoryException
     */
    public boolean addGroup(String groupId) throws NotSupportedException, RepositoryException;

    /**
     * Delete a group from the backend
     * @param groupId
     * @return true if the group is successful removed in the backend
     * @throws NotSupportedException
     * @throws RepositoryException
     */
    public boolean deleteGroup(String groupId) throws NotSupportedException, RepositoryException;

}
