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
package org.hippoecm.repository.security.user;

import java.util.Set;

import javax.transaction.NotSupportedException;

import org.hippoecm.repository.security.AAContext;
import org.hippoecm.repository.security.user.User;

/**
 * Interface for managing users in the backend
 */
public interface UserManager {

    /**
     * Initialize the UserManager with the given Context
     * @param context The context with params for the backend
     * @throws UserException
     * @See AAContext
     */
    public void init(AAContext context) throws UserException;

    /**
     * Get a list of Users know to the repository from the backend
     * @return A Set of users
     * @throws UserException
     * @see User
     */
    public Set<User> listUsers() throws UserException;

    /**
     * Try to add a user to the backend
     * @param user the user to add
     * @return true is the user is succesful added
     * @throws NotSupportedException if the backend doesn't support adding users
     * @throws UserException
     * @see User
     */
    public boolean addUser(User user) throws NotSupportedException, UserException;

    /**
     * Try to delete a user from the backend
     * @param user the user to delete
     * @return true is the user is succesful deleted
     * @throws NotSupportedException if the backend doesn't support deleting users
     * @throws UserException
     * @see User
     */
    public boolean deleteUser(User user) throws NotSupportedException, UserException;

}
