/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.security.user;

import java.security.Principal;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.AuthorizableTypeException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

/**
 * Delegating to the internal <code>UserManager</code>.
 * <P>
 * This class can be used to decorate an existing <code>UserManager</code> to build a custom <code>UserManager</code> easier.
 * </P>
 */
public class DelegatingUserManager implements UserManager {

    private final UserManager delegatee;

    public DelegatingUserManager(final UserManager delegatee) {
        this.delegatee = delegatee;
    }

    @Override
    public Authorizable getAuthorizable(String id) throws RepositoryException {
        return delegatee.getAuthorizable(id);
    }

    @Override
    public Authorizable getAuthorizable(Principal principal) throws RepositoryException {
        return delegatee.getAuthorizable(principal);
    }

    @Override
    public Authorizable getAuthorizableByPath(String path) throws UnsupportedRepositoryOperationException,
            RepositoryException {
        return delegatee.getAuthorizableByPath(path);
    }

    @Override
    public Iterator<Authorizable> findAuthorizables(String relPath, String value) throws RepositoryException {
        return delegatee.findAuthorizables(relPath, value);
    }

    @Override
    public Iterator<Authorizable> findAuthorizables(String relPath, String value, int searchType)
            throws RepositoryException {
        return delegatee.findAuthorizables(relPath, value, searchType);
    }

    @Override
    public Iterator<Authorizable> findAuthorizables(Query query) throws RepositoryException {
        return delegatee.findAuthorizables(query);
    }

    @Override
    public User createUser(String userID, String password) throws AuthorizableExistsException, RepositoryException {
        return delegatee.createUser(userID, password);
    }

    @Override
    public User createUser(String userID, String password, Principal principal, String intermediatePath)
            throws AuthorizableExistsException, RepositoryException {
        return delegatee.createUser(userID, password, principal, intermediatePath);
    }

    @Override
    public User createSystemUser(final String userID, final String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        return delegatee.createSystemUser(userID, intermediatePath);
    }

    @Override
    public Group createGroup(String groupID) throws AuthorizableExistsException, RepositoryException {
        return delegatee.createGroup(groupID);
    }

    @Override
    public Group createGroup(Principal principal) throws AuthorizableExistsException, RepositoryException {
        return delegatee.createGroup(principal);
    }

    @Override
    public Group createGroup(Principal principal, String intermediatePath) throws AuthorizableExistsException,
            RepositoryException {
        return delegatee.createGroup(principal, intermediatePath);
    }

    @Override
    public Group createGroup(String groupID, Principal principal, String intermediatePath)
            throws AuthorizableExistsException, RepositoryException {
        return delegatee.createGroup(groupID, principal, intermediatePath);
    }

    @Override
    public boolean isAutoSave() {
        return delegatee.isAutoSave();
    }

    @Override
    public void autoSave(boolean enable) throws UnsupportedRepositoryOperationException, RepositoryException {
        delegatee.autoSave(enable);
    }

    @Override
    public <T extends Authorizable> T getAuthorizable(final String id, final Class<T> authorizableClass) throws AuthorizableTypeException, RepositoryException {
        return delegatee.getAuthorizable(id, authorizableClass);
    }

}
