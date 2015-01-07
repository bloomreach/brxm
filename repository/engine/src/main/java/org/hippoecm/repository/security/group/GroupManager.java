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
package org.hippoecm.repository.security.group;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.transaction.NotSupportedException;

import org.hippoecm.repository.security.ManagerContext;
import org.hippoecm.repository.security.user.AbstractUserManager;

/**
 * Interface for managing groups in the backend
 */
public interface GroupManager {

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
     * Check if the group exists
     * @param groupId
     * @return true if the group exists
     * @throws RepositoryException
     */
    public boolean hasGroup(String groupId) throws RepositoryException;

    /**
     * Get the node for the group with the given groupId
     * @param groupId
     * @return the user node
     * @throws RepositoryException
     */
    public Node getGroup(String groupId) throws RepositoryException;

    /**
     * Create a (skeleton) node for the group in the repository
     * @param groupId
     * @return the newly created user node
     * @throws RepositoryException
     */
    public Node createGroup(String groupId) throws RepositoryException;

    /**
     * Helper method the returns the group node and
     * @param groupId
     * @return
     * @throws RepositoryException
     */
    public Node getOrCreateGroup(String groupId) throws RepositoryException;

    /**
     * Check if the current manager manages the group
     * @param group
     * @return true if the group is managed by the current manager
     */
    public boolean isManagerForGroup(Node group) throws RepositoryException;

    /**
     * Get the node type for new group nodes
     * @return the node type
     */
    public String getNodeType();

    /**
     * Checks if the backend is case aware (ie, ldap usually isn't, the internal provider is)
     * @return
     */
    public boolean isCaseSensitive();

    /**
     * Get memberships from the repository for a user
     *
     * @param rawUserId the unparsed userId
     * @throws RepositoryException
     */
    public NodeIterator getMemberships(String rawUserId) throws RepositoryException;

    /**
     * Get memberships from the repository for a user
     *
     * @param userId the unparsed userId
     * @throws RepositoryException
     */
    public Set<String> getMembershipIds(String userId) throws RepositoryException;

    /**
     * Get the members of a group
     * @param group
     * @throws RepositoryException
     */
    public Set<String> getMembers(Node group) throws RepositoryException;

    /**
     * Get all the groups, regardless of their provider.
     *
     * @return  an iterator of group nodes
     * @throws RepositoryException
     */
    public NodeIterator listGroups(long offset, long limit) throws RepositoryException;

    /**
     * Get all the groups associated with a particular provider.
     *
     * @return an iterator of group nodes
     * @throws RepositoryException
     */
    public NodeIterator listGroups(String providerId, long offset, long limit) throws RepositoryException;

    /**
     * Set members of a group
     * @param group
     * @throws RepositoryException
     */
    public void setMembers(Node group, Set<String> members) throws RepositoryException;

    /**
     * Make a user member of the group.
     * @param group
     * @param userId
     * @throws RepositoryException
     */
    public void addMember(Node group, String userId) throws RepositoryException;

    /**
     * Remove a user from the group.
     * @param group
     * @param userId
     * @throws RepositoryException
     */
    public void removeMember(Node group, String userId) throws RepositoryException;

    /**
     * Hook for the provider to sync from the backend with the repository.
     * @param user
     */
    public void syncMemberships(Node user) throws RepositoryException;

    /**
     * Save current outstanding changes to the repository.
     */
    public void saveGroups() throws RepositoryException;

    /**
     * Get the memberships of the user from the backend.
     * @param user
     * @throws RepositoryException
     */
    public Set<String> backendGetMemberships(Node user) throws RepositoryException;

    /**
     * Add a group to the backend (optional)
     * @param groupId
     * @return true if the group is successful added in the backend
     * @throws NotSupportedException
     * @throws RepositoryException
     */
    public boolean backendCreateGroup(String groupId) throws NotSupportedException, RepositoryException;

    /**
     * Delete a group from the backend (optional)
     * @param groupId
     * @return true if the group is successful removed in the backend
     * @throws NotSupportedException
     * @throws RepositoryException
     */
    public boolean backendDeleteGroup(String groupId) throws NotSupportedException, RepositoryException;

    public boolean isExternal();
}
