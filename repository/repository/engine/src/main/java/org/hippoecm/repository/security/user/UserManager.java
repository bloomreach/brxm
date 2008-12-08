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
package org.hippoecm.repository.security.user;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.transaction.NotSupportedException;

import org.hippoecm.repository.security.ManagerContext;

/**
 * Interface for managing users in the backend
 */
public interface UserManager {
    final static String SVN_ID = "$Id$";

    /**
     * Default initialize the UserManager with the given {@link ManagerContext}.
     * Calls initManager after the general init which is handled by the
     * {@link AbstractUserManager}.
     * @param context The {@link ManagerContext} with params for the backend
     * @throws RepositoryException
     * @See ManagerContext
     */
    public void init(ManagerContext context) throws RepositoryException;

    /**
     * Initialization hook for the security managers. This method gets 
     * called after the init which is handled by the {@link AbstractUserManager}
     * @param context The {@link ManagerContext} with params for the backend
     * @throws RepositoryException
     * @See ManagerContext
     */
    public void initManager(ManagerContext context) throws RepositoryException;

    /**
     * Check if the manager is configured and initialized
     * @return true if initialized otherwise false
     */
    public boolean isInitialized();

    /**
     * Authenticate the user with the current provider's user manager
     * @param creds SimpleCredentials
     * @return true when successfully authenticate
     * @throws RepositoryException
     */
    public boolean authenticate(SimpleCredentials creds) throws RepositoryException;

    /**
     * Check if the user with the given userId exists in the repository
     * @param userId
     * @return the user node
     * @throws RepositoryException
     */
    public boolean hasUser(String userId) throws RepositoryException;

    /**
     * Get the node for the user with the given userId
     * @param userId
     * @return the user node or null if the user doesn't exist
     * @throws RepositoryException
     */
    public Node getUser(String userId) throws RepositoryException;

    public NodeIterator listUsers(String providerId) throws RepositoryException;
    public NodeIterator listUsers() throws RepositoryException;
    
    /**
     * Create a (skeleton) node for the user in the repository
     * @param userId
     * @return the newly created user node
     * @throws RepositoryException
     */
    public Node createUser(String userId) throws RepositoryException;

    /**
     * Check if the current manager manages the user 
     * @param group
     * @return true if the group is managed by the current user
     */
    public boolean isManagerForUser(Node user) throws RepositoryException;

    /**
     * Get the node type for new user nodes
     * @return the node type
     */
    public String getNodeType();
    
    /**
     * Checks if the backend is case aware (ie, ldap usually isn't, the internal provider is)
     * @return
     */
    public boolean isCaseSensitive();

    /**
     * Update last login timestamp. This is handled by the {@link AbstractUserManager}.
     * @param userId
     */
    public void updateLastLogin(String userId);

    /**
     * Hook for the provider to sync from the backend with the repository.
     * Called just after authenticate.
     * @param userId
     */
    public void syncUserInfo(String userId);

    /**
     * Check if the current user is active.
     * @param userId
     * @return true if user is active
     * @throws RepositoryException
     */
    public boolean isActive(String userId) throws RepositoryException;

    
    /**
     * Save current outstanding changes to the repository.
     */
    public void saveUsers() throws RepositoryException;

    /**
     * Set the user's password in the backend
     * @param password
     * @param userId
     * @throws NotSupportedException thrown when backend does not support setting the password
     * @throws RepositoryException
     */
    public void backendSetPassword(String userId, char[] password) throws NotSupportedException, RepositoryException;
    
    /**
     * Try to add a user to the backend
     * @param user the user to add
     * @return true is the user is successful added
     * @throws NotSupportedException if the backend doesn't support adding users
     * @throws RepositoryException
     */
    public boolean backendAddUser(String userId, char[] password) throws NotSupportedException, RepositoryException;

    /**
     * Try to delete a user from the backend
     * @param user the user to delete
     * @return true is the user is successful deleted
     * @throws NotSupportedException if the backend doesn't support deleting users
     * @throws RepositoryException
     */
    public boolean backendDeleteUser(String userId) throws NotSupportedException, RepositoryException;


    /**
     * Set the user to active or inactive
     * @param active true if the user must be set to active
     * @param userId
     * @throws NotSupportedException thrown when backend does not support setting the active flag
     * @throws RepositoryException
     */
    public void backendSetActive(String userId, boolean active) throws NotSupportedException, RepositoryException;

    /**
     * Set a key value pair property on the user in the backend
     * @param key the key string
     * @param value the value string
     * @throws NotSupportedException thrown when the backend does not support setting the key
     * @throws RepositoryException
     */
    public void backendSetProperty(String userId, String key, String value) throws NotSupportedException, RepositoryException;

}
