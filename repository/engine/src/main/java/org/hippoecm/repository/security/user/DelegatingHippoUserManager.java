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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableTypeException;

/**
 * Delegating to the internal <code>HippoUserManager</code>.
 * <P>
 * This class can be used to decorate an existing <code>HippoUserManager</code> to build a custom <code>HippoUserManager</code> easier.
 * </P>
 */
public class DelegatingHippoUserManager extends DelegatingUserManager implements HippoUserManager {

    private final HippoUserManager delegatee;

    public DelegatingHippoUserManager(final HippoUserManager delegatee) {
        super(delegatee);
        this.delegatee = delegatee;
    }

    @Override
    public boolean hasUser(String userId) throws RepositoryException {
        return delegatee.hasUser(userId);
    }

    @Override
    public Node getUser(String userId) throws RepositoryException {
        return delegatee.getUser(userId);
    }

    @Override
    public Node createUser(String userId) throws RepositoryException {
        return delegatee.createUser(userId);
    }

    @Override
    public NodeIterator listUsers(long offset, long limit) throws RepositoryException {
        return delegatee.listUsers(offset, limit);
    }

    @Override
    public NodeIterator listUsers(String providerId, long offset, long limit) throws RepositoryException {
        return delegatee.listUsers(providerId, offset, limit);
    }

    @Override
    public boolean authenticate(SimpleCredentials creds) throws RepositoryException {
        return delegatee.authenticate(creds);
    }

    @Override
    public boolean isActive(final String userId) throws RepositoryException {
        return delegatee.isActive(userId);
    }

    @Override
    public boolean isPasswordExpired(final String userId) throws RepositoryException {
        return delegatee.isPasswordExpired(userId);
    }

    @Override
    public void syncUserInfo(String userId) {
        delegatee.syncUserInfo(userId);
    }

    @Override
    public void updateLastLogin(String userId) {
        delegatee.updateLastLogin(userId);
    }

    @Override
    public void saveUsers() throws RepositoryException {
        delegatee.saveUsers();
    }

    @Override
    public boolean isCaseSensitive() {
        return delegatee.isCaseSensitive();
    }

}
